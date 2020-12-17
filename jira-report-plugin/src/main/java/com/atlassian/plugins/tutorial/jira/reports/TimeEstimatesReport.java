package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import java.util.Map;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.properties.LookAndFeelBean;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.KeyComparator;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculator;
import com.atlassian.jira.issue.util.IssueImplAggregateTimeTrackingCalculator;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.ReportSubTaskFetcher;
import com.atlassian.jira.plugin.report.SubTaskInclusionOption;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.portal.FilterValuesGenerator;
import com.atlassian.jira.portal.SortingValuesGenerator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.jira.util.velocity.DefaultVelocityRequestContextFactory;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.model.querydsl.CustomFieldOptionDTO;


import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import com.google.common.collect.Lists;
import com.opensymphony.util.TextUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.ofbiz.core.entity.GenericEntityException;
import webwork.action.ActionContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.ONLY_SELECTED_VERSION;
import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.fromKey;
import static com.atlassian.jira.util.dbc.Assertions.notNull;


@Scanned
public class TimeEstimatesReport extends AbstractReport {
    private static final Long LONG_MAX = Long.MAX_VALUE;

    private final AccuracyCalculatorExtended accuracyCalculator = new AccuracyCalculatorImpl();
    @JiraImport
    private final ApplicationProperties applicationProperties;
    @JiraImport
    private final BuildUtilsInfo buildUtilsInfo;
    @JiraImport
    private final ConstantsManager constantsManager;
    private final DurationFormatterExtended defaultDurationFormatter;
    @JiraImport
    private final ReportSubTaskFetcher reportSubTaskFetcher;
    @JiraImport
    private final SearchService searchService;
    @JiraImport
    private final SubTaskManager subTaskManager;
    private final Totals totals = new Totals();
    @JiraImport
    private final VersionManager versionManager;

    private StoryPicker storyPicker;

    private EpicPicker epicPicker;
    // set to different ones for excel or html views
    private DurationFormatterExtended durationFormatter;

    private TimeTrackingSummaryBeanExtended summaryBean;

    TimeEstimatesReport(
            final VersionManager versionManager,
            final ApplicationProperties applicationProperties,
            final ConstantsManager constantsManager,
            final DurationFormatterExtended durationFormatter,
            final SearchService searchService,
            final BuildUtilsInfo buildUtilsInfo,
            final ReportSubTaskFetcher reportSubTaskFetcher,
            final SubTaskManager subTaskManager) {
        this.versionManager = versionManager;
        this.applicationProperties = applicationProperties;
        this.constantsManager = constantsManager;
        this.defaultDurationFormatter = durationFormatter;
        this.reportSubTaskFetcher = reportSubTaskFetcher;
        this.durationFormatter = defaultDurationFormatter;
        this.searchService = searchService;
        this.buildUtilsInfo = notNull("buildUtilsInfo", buildUtilsInfo);
        this.subTaskManager = subTaskManager;
    }

    TimeEstimatesReport(
            final VersionManager versionManager,
            final ApplicationProperties applicationProperties,
            final ConstantsManager constantsManager,
            final JiraDurationUtils jiraDurationUtils,
            final SearchService searchService,
            final BuildUtilsInfo buildUtilsInfo,
            final ReportSubTaskFetcher reportSubTaskFetcher,
            final SubTaskManager subTaskManager) {
        this(versionManager, applicationProperties, constantsManager, new DurationFormatterExtendedImpl(new I18nBean(),
                jiraDurationUtils), searchService, buildUtilsInfo, reportSubTaskFetcher, subTaskManager);
    }

    @Inject
    public TimeEstimatesReport(
             final VersionManager versionManager,
             final ApplicationProperties applicationProperties,
             final ConstantsManager constantsManager,
             final SearchService searchService,
             final BuildUtilsInfo buildUtilsInfo,
             final ReportSubTaskFetcher reportSubTaskFetcher,
             final SubTaskManager subTaskManager) {
        this(versionManager, applicationProperties, constantsManager, new DurationFormatterExtendedImpl(new I18nBean(),
                ComponentAccessor.getJiraDurationUtils()), searchService, buildUtilsInfo, reportSubTaskFetcher, subTaskManager);
    }

    @Override
    public boolean showReport() {
        boolean value = applicationProperties.getOption(APKeys.JIRA_OPTION_TIMETRACKING);
        return applicationProperties.getOption(APKeys.JIRA_OPTION_TIMETRACKING);
    }

    // Return a map of parameters to pass through to the velocity template for this report
    Map<String, Object> getParams(final ProjectActionSupport action, final Map reqParams) throws PermissionException, GenericEntityException, SearchException {
        final ApplicationUser remoteUser = action.getLoggedInApplicationUser();
        final Long projectId = action.getSelectedProjectObject().getId();
//        final String versionIdString = (String) reqParams.get("versionId");
//       Version version = null;
//        if (!versionIdString.equals("-1")) {
//           final Long versionId = new Long(versionIdString);
//           version = versionManager.getVersion(versionId);
//       }

        final TextUtils textUtils = new TextUtils();

//        final String sortingOrder = (String) reqParams.get("sortingOrder");
//        final String completedFilter = (String) reqParams.get("completedFilter");
        final String subtaskInclusionKey = "all";
        SubTaskInclusionOption subtaskInclusion = (subtaskInclusionKey == null) ? ONLY_SELECTED_VERSION : fromKey(subtaskInclusionKey);
        final String story = (String) reqParams.get("storyValues");
        final String epic = (String) reqParams.get("epicValues");
        final Collection issues = getReportIssues(remoteUser, projectId, story, epic, subtaskInclusion);
        summaryBean = new TimeTrackingSummaryBeanExtended(issues);
        int numberOfIssues = issues.size();
        Map<String, Long> planEstimates = summaryBean.getPlanEstimates();
        Map<String, Long> devEstimates = summaryBean.getDevEstimates();
        Map<String, Long> subTaskDevEstimates = summaryBean.getSubTaskDevEstimates();
        Map<String, Long> subTaskPlanEstimates = summaryBean.getSubTaskPlanEstimates();
        Map<String, Boolean> hasSubTask = summaryBean.getHasSubtask();
        Map<String, Integer> totalTimeEstimate = summaryBean.getTotalTimeEstimate();
        Map<String, Integer> totalSubTaskTime = summaryBean.getSubTaskTotal();
        Map<String, BigDecimal> subTaskDevDeviation = summaryBean.getSubTaskDevDeviation();
        Map<String, BigDecimal> subTaskDeviation = summaryBean.getSubTaskDeviation();
        Map<String, Long> subTaskActualTimes = summaryBean.getSubTaskActualTimes();
        Map<String, Long> subTaskRemainingEstimates = summaryBean.getSubTaskRemainingTimes();
        Map<String, Long> issueActualTimes = summaryBean.getIssueActualTimes();
        Map<String, Long> issueRemainingTimes = summaryBean.getIssueRemainingTimes();
        long aggregateTotal = summaryBean.getAggregateTotal();
        long aggregateIssueEstimate = summaryBean.getAggregateIssueEstimate();
        long aggregateIssueRemainingTime = summaryBean.getAggregateIssueRemainingTime();
        final Map<String, Object> velocityParams = new HashMap<String, Object>();
        velocityParams.put("report", this);
        velocityParams.put("action", action);
       // velocityParams.put("version", version);
        velocityParams.put("textUtils", textUtils);
        velocityParams.put("issues", issues);
        velocityParams.put("summaryBean", summaryBean);
        //velocityParams.put("sortingOrder", sortingOrder);
        //velocityParams.put("completedFilter", completedFilter);
        //velocityParams.put("versionIdString", versionIdString);
        velocityParams.put("constantsManager", constantsManager);
        velocityParams.put("remoteUser", remoteUser);
        velocityParams.put("totals", totals);
        velocityParams.put("issuesSize",numberOfIssues);
        //velocityParams.put("subtasksEnabled", subTaskManager.isSubTasksEnabled());
//        if (SubTaskInclusionOption.isValidKey(subtaskInclusionKey)) {
//            velocityParams.put("subtaskDescription", subtaskInclusion.getDescription(getI18nHelper()));
//        } else {
//            velocityParams.put("subtaskDescription", "unknown option: " + subtaskInclusionKey);
//        }
        velocityParams.put("devEstimates",devEstimates);
        velocityParams.put("planEstimates",planEstimates);
        velocityParams.put("subTaskPlanEstimates",subTaskPlanEstimates);
        velocityParams.put("subTaskDevEstimates",subTaskDevEstimates);
        velocityParams.put("hasSubTask",hasSubTask);
        velocityParams.put("totalTimeEstimate", totalTimeEstimate);
        velocityParams.put("totalSubTaskTime", totalSubTaskTime);
        velocityParams.put("subTaskDevDeviation", subTaskDevDeviation);
        velocityParams.put("subTaskDeviation", subTaskDeviation);
        velocityParams.put("aggregateTotal", aggregateTotal);
        velocityParams.put("aggregateIssueEstimate", aggregateIssueEstimate);
        velocityParams.put("aggregateIssueRemainingTime", aggregateIssueRemainingTime);
        velocityParams.put("issueActualTimes", issueActualTimes);
        velocityParams.put("issueRemainingTimes", issueRemainingTimes);
        velocityParams.put("subTaskActualTimes", subTaskActualTimes);
        velocityParams.put("subTaskRemainingEstimates", subTaskRemainingEstimates);

        // Excel view params
        final LookAndFeelBean lookAndFeelBean = LookAndFeelBean.getInstance(applicationProperties);

        final VelocityRequestContextFactory contextFactory = new DefaultVelocityRequestContextFactory(applicationProperties);

        String jiraLogo = lookAndFeelBean.getLogoUrl();
        final String jiraBaseUrl = contextFactory.getJiraVelocityRequestContext().getBaseUrl();
        if ((jiraLogo != null) && !jiraLogo.startsWith("http://") && !jiraLogo.startsWith("https://")) {
            jiraLogo = jiraBaseUrl + jiraLogo;
        }
        velocityParams.put("jiraLogo", jiraLogo);
        velocityParams.put("jiraLogoWidth", lookAndFeelBean.getLogoWidth());
        velocityParams.put("jiraLogoHeight", lookAndFeelBean.getLogoHeight());
        velocityParams.put("jiraTitle", applicationProperties.getString(APKeys.JIRA_TITLE));
        velocityParams.put("topBgColor", lookAndFeelBean.getTopBackgroundColour());
        velocityParams.put("buildInfo", buildUtilsInfo.getBuildInformation());
        velocityParams.put("buildNumber", buildUtilsInfo.getCurrentBuildNumber());
        velocityParams.put("createDate", new Date());
        velocityParams.put("jiraBaseUrl", jiraBaseUrl);

        return velocityParams;
    }

    // Generate a HTML view of report
    public String generateReportHtml(final ProjectActionSupport action, final Map reqParams) throws Exception {
        durationFormatter = defaultDurationFormatter;
        storyPicker = new StoryPicker(searchService, ComponentAccessor.getJiraAuthenticationContext());
        epicPicker = new EpicPicker(searchService, ComponentAccessor.getJiraAuthenticationContext());
        return descriptor.getHtml("view", getParams(action, reqParams));
    }

    // Generate an EXCEL view of report
    @Override
    public String generateReportExcel(final ProjectActionSupport action, final Map reqParams) throws Exception {
        final StringBuilder contentDispositionValue = new StringBuilder(50);
        contentDispositionValue.append("attachment;filename=\"");
        contentDispositionValue.append(getDescriptor().getName()).append(".xls\";");

        // Add header to fix JRA-8484
        final HttpServletResponse response = ActionContext.getResponse();
        response.addHeader("content-disposition", contentDispositionValue.toString());
        durationFormatter = new MinutesDurationFormatterExtended();
        return descriptor.getHtml("excel", getParams(action, reqParams));
    }

    @Override
    public boolean isExcelViewSupported() {
        return true;
    }

    @Override
    public void validate(final ProjectActionSupport action, final Map params) {
        super.validate(action, params);
        final String selectedProjectId = (String) params.get("selectedProjectId");
        if (StringUtils.isNotEmpty(selectedProjectId)) {
            action.setSelectedProjectId(new Long(selectedProjectId));
        }
        if (action.getSelectedProjectObject() == null) {
            action.addErrorMessage(action.getText("admin.errors.timetracking.no.project"));
            return;
        }
        if (!action.getBrowsableProjects().contains(action.getSelectedProjectObject())) {
            action.addErrorMessage(action.getText("report.error.project.id.not.found"));
            return;
        }

        try {
            final Project project = action.getSelectedProjectObject();
            if (project == null) {
                action.addErrorMessage(getI18nHelper().getText("admin.errors.timetracking.no.project"));
                return;
            }

        } catch (final Exception e) {
            action.addErrorMessage(getI18nHelper().getText("admin.errors.timetracking.versions.error"));
        }
    }

    /**
     * Get a collection of all version ids in the selected project
     *
     * @param project project to get the version ids for
     * @return collection of version ids
     */
    public Collection<String> getProjectVersionIds(final Project project) {
        final List<Version> versions = versionManager.getVersions(project.getId());
        final Collection<String> versionIds = Lists.newArrayListWithCapacity(versions.size());
        for (final Version version : versions) {
            versionIds.add(version.getId().toString());
        }
        return versionIds;
    }

    /**
     * Get the list of issues to be displayed in the report.
     *
     * @param user             user
     * @param projectId        project id
     * @param versionId        version id
     * @param sortingOrder     sorting order
     * @param completedFilter  completed filter, e.g. {@link com.atlassian.jira.portal.FilterValuesGenerator#FILTER_INCOMPLETE_ISSUES}
     * @param subtaskInclusion whether to include subtasks with null or any fixfor version
     * @return collection of issues
     * @throws SearchException if error occurs
     */
    Collection<ReportIssueExtended> getReportIssues(final ApplicationUser user, final Long projectId, final String story, final String epic,final SubTaskInclusionOption subtaskInclusion) throws SearchException {
        final Set<Issue> issuesFound = searchIssues(user, projectId, story, epic,subtaskInclusion);
        final Predicate reportIssueFilter = getCompletionFilter("all");
        final Set<SubTaskingIssueDecoratorExtended> transformedIssues = new IssueSubTaskTransformerExtended(reportIssueFilter).getIssues(issuesFound);

        final Comparator completionComparator = getCompletionComparator("least");

        return new Processor(durationFormatter, accuracyCalculator, completionComparator, reportIssueFilter).getDecoratedIssues(transformedIssues);
    }

    /**
     * Creates a filter that only lets issues that match the given completion criterion. Valid values for
     * completedFilter are FilterValuesGenerator.FILTER_ALL_ISSUES (which means do not filter any issues out) and
     * FilterValuesGenerator.FILTER_INCOMPLETE_ISSUES (which means only include issues that are incomplete or that have
     * at least one incomplete subtask).
     *
     * @param completedFilter designation for filtering issues based on whether they are completed.
     * @return a {@link Predicate} to filter issues.
     */
    private Predicate getCompletionFilter(final String completedFilter) {
        Predicate reportIssueFilter;
        if (completedFilter.equals(FilterValuesGenerator.FILTER_INCOMPLETE_ISSUES)) {
            // include only issues that are incomplete or that have incomplete subtasks
            reportIssueFilter = new Predicate() {
                public boolean evaluate(final Object object) {
                    if (object instanceof ReportIssueExtended) {
                        return !((ReportIssueExtended) object).isAggregateComplete();
                    }
                    final Issue issue = (Issue) object;
                    return (issue.getEstimate() != null) && (issue.getEstimate() != 0);
                }
            };
        } else {
            // include all
            reportIssueFilter = PredicateUtils.truePredicate();
        }
        return reportIssueFilter;
    }

    private Comparator getCompletionComparator(final String sortingOrder) {
        final Comparator comparator = new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                final ReportIssueExtended reportIssue1 = (ReportIssueExtended) arg0;
                final ReportIssueExtended reportIssue2 = (ReportIssueExtended) arg1;
                int result = reportIssue1.getAggregateRemainingEstimateLong(LONG_MAX).compareTo(
                        reportIssue2.getAggregateRemainingEstimateLong(LONG_MAX));
                if (result == 0) {
                    // they are the same, so fall back to largest original estimate
                    result = reportIssue2.getAggregateRemainingEstimateLong(LONG_MAX).compareTo(
                            reportIssue1.getAggregateRemainingEstimateLong(LONG_MAX));
                }
                if (result == 0) {
                    // still the same so sort by issue key
                    return KeyComparator.COMPARATOR.compare(reportIssue1.getIssue().getKey(), reportIssue2.getIssue().getKey());
                }
                return result;
            }
        };
        if (sortingOrder.equals(SortingValuesGenerator.SORT_BY_MOST_COMPLETED)) {
            return comparator;
        }

        return new ReverseComparator(comparator);
    }

    private Set<Issue> searchIssues(final ApplicationUser user, final Long projectId, final String story, final String epic,final SubTaskInclusionOption subtaskInclusion) throws SearchException {
        final Query parentQuery = getSearchForParents(projectId, story, epic);
        final SearchResults<Issue> parentSearchResults = searchService.search( user, parentQuery,new PagerFilter(Integer.MAX_VALUE));

        final Set<Issue> result = new LinkedHashSet<Issue>();
        final List<Issue> parentIssues = parentSearchResults.getResults();
        result.addAll(parentIssues);

        // search for subtasks if necessary
        final List<Issue> subtasks = reportSubTaskFetcher.getSubTasks(user, parentIssues, subtaskInclusion, false);
        result.addAll(subtasks);

        return result;
    }

    /*
     * get the initial search request that gets the parent issues and subtasks of the same version
     */
    private Query getSearchForParents(final Long projectId, final String story, final String epic) {
        final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        final JqlClauseBuilder builder = queryBuilder.where().project(projectId);


        if(!(story.contains("1") || story.contains("2") || story.contains("3"))){
            builder.and().summary(story);
        }

        if(!(epic.contains("1") || epic.contains("2") || epic.contains("3"))){
            builder.and().summary(epic);
        }

        return queryBuilder.buildQuery();
    }

    public int getCompletionPercentage() {
        return (int) (((float) summaryBean.getTimeSpent() / (float) (summaryBean.getTimeSpent() + summaryBean.getRemainingEstimate())) * 100F);
    }

    public int getAccuracyPercentage() {
        return AccuracyCalculatorExtended.Percentage.calculate(summaryBean.getOriginalEstimate(), summaryBean.getTimeSpent(),
                summaryBean.getRemainingEstimate());
    }

    /* for unit testing only! */
    Totals getTotals() {
        return totals;
    }

    private I18nHelper getI18nHelper() {
        return descriptor.getI18nBean();
    }

    /*
     * accessor for tests
     */
    DurationFormatterExtended getDurationFormatter() {
        return durationFormatter;
    }

    /**
     * Responsible for taking a List of Issues and creating a List of {@link ReportIssue ReportIssues} for our report
     */
    static class Processor {
        private final DurationFormatterExtended durationFormatter;
        private final AccuracyCalculatorExtended accuracyCalculator;
        private final Comparator comparator;
        private final Predicate reportIssueFilter;

        Processor(final DurationFormatterExtended durationFormatter, final AccuracyCalculatorExtended accuracyCalculator, final Comparator comparator) {
            this(durationFormatter, accuracyCalculator, comparator, PredicateUtils.truePredicate());
        }

        /**
         * Constructs a Processor that can also filter returned results using the reportIssueFilter.
         *
         * @param durationFormatter  for making the durations pretty
         * @param accuracyCalculator to work out the accuracy
         * @param comparator         to sort the issues
         * @param reportIssueFilter  to remove unwanted issues
         */
        Processor(final DurationFormatterExtended durationFormatter, final AccuracyCalculatorExtended accuracyCalculator, final Comparator comparator, final Predicate reportIssueFilter) {
            this.durationFormatter = durationFormatter;
            this.accuracyCalculator = accuracyCalculator;
            this.comparator = comparator;
            this.reportIssueFilter = reportIssueFilter;
        }

        List getDecoratedIssues(final Collection /* <SubTaskingIssueDecorator> */issues) {
            final AggregateTimeTrackingCalculator timeTrackingCalculator = new IssueImplAggregateTimeTrackingCalculator(
                    new IssueImplAggregateTimeTrackingCalculator.PermissionChecker() {
                        public boolean hasPermission(final Issue subTask) {
                            // permission checks should have already been done.
                            return true;
                        }
                    });
            final List<ReportIssueExtended> decoratedIssues = new ArrayList<ReportIssueExtended>(issues.size());
            for (final Object issue1 : issues) {
                final Issue issue = (Issue) issue1;
                final ReportIssueExtended reportIssue = new ReportIssueExtended(issue, timeTrackingCalculator, durationFormatter, accuracyCalculator, comparator,
                        reportIssueFilter);
                // add those that match the filter only
                if (reportIssueFilter.evaluate(reportIssue)) {
                    decoratedIssues.add(reportIssue);
                }
            }
            Collections.sort(decoratedIssues, comparator);
            return Collections.unmodifiableList(decoratedIssues);
        }
    }

    final class AccuracyCalculatorImpl implements AccuracyCalculatorExtended {
        public String calculateAndFormatAccuracy(final Long originalEstimate, final Long remainingEstimate, final Long timeSpent) {
            if (accuracyIncalculable(originalEstimate, remainingEstimate, timeSpent)) {
                return getI18nHelper().getText("viewissue.timetracking.unknown");
            }
            final Long accuracy = getAccuracy(originalEstimate, remainingEstimate, timeSpent);
            return durationFormatter.shortFormat(accuracy);
        }

        public Long calculateAccuracy(final Long originalEstimate, final Long remainingEstimate, final Long timeSpent) {
            if (accuracyIncalculable(originalEstimate, remainingEstimate, timeSpent)) {
                return null;
            }
            return getAccuracy(originalEstimate, remainingEstimate, timeSpent);
        }

        public int onSchedule(final Long originalEstimate, final Long remainingEstimate, final Long timeSpent) {
            if ((originalEstimate == null) || (remainingEstimate == null) || (timeSpent == null)) {
                return 0;
            }
            final long accuracy = getAccuracy(originalEstimate, remainingEstimate, timeSpent);
            return accuracy == 0 ? 0 : ((accuracy > 0) ? 1 : -1);
        }

        private Long getAccuracy(final long originalEst, final long timeEst, final long timeSpent) {
            return originalEst - timeEst - timeSpent;
        }

        private boolean accuracyIncalculable(final Long originalEstimate, final Long remainingEstimate, final Long timeSpent) {
            return (originalEstimate == null) || (remainingEstimate == null) || (timeSpent == null);
        }
    }

    public class Totals {
        public String getOriginalEstimate() {
            return durationFormatter.shortFormat(summaryBean.getOriginalEstimate());
        }

        public String getTimeSpent() {
            return durationFormatter.shortFormat(summaryBean.getTimeSpent());
        }

        public String getRemainingEstimate() {
            return durationFormatter.shortFormat(summaryBean.getRemainingEstimate());
        }

        public String getAccuracyNice() {
            return accuracyCalculator.calculateAndFormatAccuracy(summaryBean.getOriginalEstimate(), summaryBean.getRemainingEstimate(), summaryBean.getTimeSpent());
        }

        public String getAccuracy() {
            final Long accuracy = accuracyCalculator.calculateAccuracy(summaryBean.getOriginalEstimate(), summaryBean.getRemainingEstimate(), summaryBean.getTimeSpent());
            return durationFormatter.format(accuracy);
        }

        public String getAccuracyPercentage() {
            return "" + AccuracyCalculatorExtended.Percentage.calculate(summaryBean.getOriginalEstimate(), summaryBean.getRemainingEstimate(),
                    summaryBean.getTimeSpent());
        }

        public int onSchedule() {
            return accuracyCalculator.onSchedule(summaryBean.getOriginalEstimate(), summaryBean.getRemainingEstimate(), summaryBean.getTimeSpent());
        }

        /* used in bars.vm */
        public String getTotalCurrentEstimate() {
            return durationFormatter.shortFormat(summaryBean.getRemainingEstimate() + summaryBean.getTimeSpent());
        }

        public String getAggregateOriginalEstimate() {
            return durationFormatter.shortFormat(summaryBean.getAggregateOriginalEstimate());
        }

        public String getAggregateTimeSpent() {
            return durationFormatter.shortFormat(summaryBean.getAggregateTimeSpent());
        }

        public String getAggregateRemainingEstimate() {
            return durationFormatter.shortFormat(summaryBean.getAggregateRemainingEstimate());
        }

        public String getAggregateAccuracyNice() {
            return accuracyCalculator.calculateAndFormatAccuracy(summaryBean.getAggregateOriginalEstimate(), summaryBean.getAggregateRemainingEstimate(), summaryBean.getAggregateTimeSpent());
        }

        public String getAggregateAccuracy() {
            final Long accuracy = accuracyCalculator.calculateAccuracy(summaryBean.getAggregateOriginalEstimate(), summaryBean.getAggregateRemainingEstimate(), summaryBean.getAggregateTimeSpent());
            return durationFormatter.format(accuracy);
        }

        public String getAggregateAccuracyPercentage() {
            return "" + AccuracyCalculatorExtended.Percentage.calculate(summaryBean.getAggregateOriginalEstimate(),
                    summaryBean.getAggregateRemainingEstimate(), summaryBean.getAggregateTimeSpent());
        }

        public int isAggregateOnSchedule() {
            return accuracyCalculator.onSchedule(summaryBean.getOriginalEstimate(), summaryBean.getRemainingEstimate(), summaryBean.getTimeSpent());
        }
    }
}
