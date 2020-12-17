package com.tadigital.jira.plugin.report.helper;

import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.ONLY_SELECTED_VERSION;
import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.fromKey;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.KeyComparator;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculator;
import com.atlassian.jira.issue.util.IssueImplAggregateTimeTrackingCalculator;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.ReportSubTaskFetcher;
import com.atlassian.jira.plugin.report.SubTaskInclusionOption;
import com.atlassian.jira.portal.SortingValuesGenerator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.query.Query;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.plugin.data.DeveloperData;
import com.tadigital.jira.plugin.data.EstimatesData;
import com.tadigital.jira.plugin.data.ProjectData;
import com.tadigital.jira.plugin.helper.SprintHelper;
import com.tadigital.jira.plugin.report.IssueSubTaskTransformerExtended;
import com.tadigital.jira.plugin.report.ReportIssueExtended;
import com.tadigital.jira.plugin.report.SubTaskingIssueDecoratorExtended;
import com.tadigital.jira.plugin.report.TimeTrackingSummaryBeanExtended;
import com.tadigital.jira.plugin.report.data.PracticeLevelReportData;
import com.tadigital.jira.utility.HRMSApiCallService;
import com.tadigital.jira.utility.JiraUtils;

public class EstimatesReportHelper { 
	
	
    private static final String SUBTASK_INCLUSION_KEY = "all";
	private static final SubTaskInclusionOption SUBTASK_INCLUSION = (SUBTASK_INCLUSION_KEY == null) ? ONLY_SELECTED_VERSION : fromKey(SUBTASK_INCLUSION_KEY);
	private Long developerFieldId;
	private ProjectManager projectManager;
	private SearchService searchService;
	private ReportSubTaskFetcher reportSubTaskFetcher;
	private IssueTypeManager issueTypeManager;
	private WorkflowManager workflowManager;
	private ApplicationUser applicationUser;
	
	public EstimatesReportHelper(ProjectManager projectManager, SearchService searchService, ReportSubTaskFetcher reportSubTaskFetcher, IssueTypeManager issueTypeManager, WorkflowManager workflowManager, CustomFieldManager customFieldManager, ApplicationUser applicationUser) {
		this.projectManager = projectManager;
		this.searchService = searchService;
		this.reportSubTaskFetcher = reportSubTaskFetcher;
		this.issueTypeManager = issueTypeManager;
		this.workflowManager = workflowManager;
		this.applicationUser = applicationUser;
		
		for (CustomField customField : customFieldManager.getCustomFieldObjects()) {
			if (customField.getName().equalsIgnoreCase(ReportsConstant.DEVELOPER_CF)) {
				this.developerFieldId = customField.getIdAsLong();
			} 
		}
	}
	
	/**
	 * Returns a list of estimate beans for populating data.
	 * @param categoryId
	 * @return
	 * @throws SearchException
	 */
	public List<ProjectData> getAllProjectsStatistics(Long categoryId) throws SearchException {

		List<ProjectData> projectStatistics = new ArrayList<>();
		
		for (Project project : projectManager.getProjects()) {
			
			String PROJECT_CATEGORY_NAME = "--";
			String PROJECT_LEAD_NAME = setProjectLeadName(project.getProjectLead());
			
			if(isValidProjectCategory(project.getProjectCategory())) {
				PROJECT_CATEGORY_NAME = setProjectCategoryName(project.getProjectCategory());
				ProjectData projectData = new ProjectData(PROJECT_CATEGORY_NAME, project.getId(), project.getName(), project.getKey(), PROJECT_LEAD_NAME);
				populateEstimatesForProjects(projectData);
				populateProjectStatus(projectData);
				if(categoryId.equals(Long.MAX_VALUE)) {
					if(isProjectInClientGroup(project)) {
						projectStatistics.add(projectData);
					}
				}
				else {
					if(categoryId.equals(project.getProjectCategory().getId())) {
						projectStatistics.add(projectData);
					}
				}
			}
		}

		return projectStatistics;
	}
	
	public List<PracticeLevelReportData> getPracticeLevelReportDateForAll(final Map<String, String> practiceMap, final String band, final String from, final String to) throws Exception {
		List<PracticeLevelReportData> practiceLevelReportDataList = new ArrayList<>();
		JiraUtils jiraUtils = new JiraUtils();
		for(Map.Entry<String, String> entry: practiceMap.entrySet()) {
			String practice = entry.getKey();
			if(from.contains("/") || to.contains("/")) {
				practiceLevelReportDataList.addAll(getPracticecLevelReportData(practice, band, from, to));
			}
			else {
				practiceLevelReportDataList.addAll(getPracticecLevelReportData(practice, band, jiraUtils.getFormattedDate(from, ReportsConstant.DATEPICKER_DATE_FORMAT,
						ReportsConstant.DATE_FORMAT),
						jiraUtils.getFormattedDate(to, ReportsConstant.DATEPICKER_DATE_FORMAT,
								ReportsConstant.DATE_FORMAT)));
			}
		}
		return practiceLevelReportDataList;
	}
	
	public List<PracticeLevelReportData> getPracticecLevelReportData(final String practice, final String band, final String from, final String to) throws Exception {
		
		HRMSApiCallService apiCallService = new HRMSApiCallService();
		JSONArray jsonarray = new JSONArray(apiCallService.getApiCall(practice));
		ReportsConstant reportsConstant = new ReportsConstant();
		
		List<PracticeLevelReportData> practiceLevelReportDataList = new ArrayList<>();
		
		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject obj = jsonarray.getJSONObject(i);
			if (!reportsConstant.getNotRequiredList().contains(obj.get(ReportsConstant.JSON_GET_DESIGNATION).toString())) {

				if (obj.get("bandName").toString().equalsIgnoreCase(band) || band.equalsIgnoreCase(ReportsConstant.ALL)) {
					practiceLevelReportDataList
					.add(populatePracticeLevelReportData(practice, obj, convertStringToDate(from), convertStringToDate(to)));
				}
			}
		}
		return practiceLevelReportDataList;
	}
	
	private PracticeLevelReportData populatePracticeLevelReportData(final String practice, final JSONObject developerObject, final Date from, final Date to) throws SearchException {
		String[] statuses = { ReportsConstant.STATUS_CLOSED };
		Collection<ReportIssueExtended> issues = getReportIssues(applicationUser, null, SUBTASK_INCLUSION, statuses,
				ReportsConstant.SUB_STORY, developerObject.get(ReportsConstant.JSON_GET_LOWERUSERNAME).toString(), from, to, null);
		TimeTrackingSummaryBeanExtended summaryBean = new TimeTrackingSummaryBeanExtended(issues);
		EstimatesData estimatesData = populateEstimates(summaryBean);
		DeveloperData developerData = populateDeveloperData(practice, developerObject, summaryBean, issues.size(), from, to);
		return new PracticeLevelReportData(developerData, estimatesData);
	}
	
	private Date convertStringToDate(final String timeStamp) throws ParseException {
		if(StringUtils.isEmpty(timeStamp)) {
			return null;
		}
		return new SimpleDateFormat(ReportsConstant.DATE_FORMAT).parse(timeStamp);
	}
	
	private EstimatesData populateEstimates(final TimeTrackingSummaryBeanExtended summaryBean) {
		EstimatesData estimatesData = new EstimatesData();
		estimatesData.setArchitectEstimate(summaryBean.getAggregateArchitectEstimates());
		estimatesData.setOriginalEstimate(summaryBean.getAggregateDevEstimates());
		estimatesData.setActualEstimate(summaryBean.getAggregateActualTime());
		estimatesData.setRemainingEstimate(summaryBean.getAggregateIssueRemainingTime());
		estimatesData.setDevEstimateDeviation(summaryBean.getAggregateDevDeviation());
		estimatesData.setArchitectDevDeviation(summaryBean.getAggregateArchitectDevDeviation());
		estimatesData.setArchitectActualDeviation(summaryBean.getAggregateArchitectActualDeviation());
		return estimatesData;
	}
	
	private DeveloperData populateDeveloperData(final String practice, final JSONObject developerObject, final TimeTrackingSummaryBeanExtended summaryBean,  final int storiesConsidered, final Date fromDate, final Date toDate) throws SearchException {
		final String jiraName = developerObject.get(ReportsConstant.JSON_GET_LOWERUSERNAME).toString();
		DeveloperData developerData = new DeveloperData(
				Long.parseLong(developerObject.get(ReportsConstant.JSON_GET_ID).toString()),
				developerObject.get(ReportsConstant.JSON_GET_FIRSTNAME).toString() + " "
						+ developerObject.get(ReportsConstant.JSON_GET_LASTNAME).toString(),
				developerObject.get(ReportsConstant.JSON_GET_LOWERUSERNAME).toString(),
				practice,
				developerObject.get(ReportsConstant.JSON_GET_DESIGNATION).toString());
		developerData.setNumberOfExcludedSubstories(summaryBean.getNumberOfExcludedSubstories());
		developerData.setStoriesConsidered(storiesConsidered);
		developerData.setTotalStories(getTotalSubStoriesCount(jiraName, fromDate, toDate));
		return developerData;
	}
	
	public EstimatesData computeTotalEstimates(Collection<PracticeLevelReportData> practiceLevelReportDatas) {
		
		EstimatesData totalEstimatesData = new EstimatesData();
		
		double architectEstimate = 0;
		double originalEstimate = 0;
		double actualEstimate = 0;
		double remainingEstimate = 0;
		long devEstimateDeviation = 0;
		long architectDevDeviation = 0;
		long architectActualDeviation = 0;
		
		for(PracticeLevelReportData practiceLevelReportData: practiceLevelReportDatas) {
			architectEstimate += practiceLevelReportData.getEstimatesData().getArchitectEstimate();
			originalEstimate += practiceLevelReportData.getEstimatesData().getOriginalEstimate();
			actualEstimate += practiceLevelReportData.getEstimatesData().getActualEstimate();
			remainingEstimate += practiceLevelReportData.getEstimatesData().getRemainingEstimate();
		}
		if(originalEstimate != 0) {
			devEstimateDeviation = Math.round((actualEstimate + remainingEstimate - originalEstimate) / originalEstimate * 100);
		}
		if(architectEstimate != 0) {
			architectDevDeviation = Math.round((originalEstimate - architectEstimate) / architectEstimate * 100);
			architectActualDeviation = Math.round((actualEstimate + remainingEstimate - architectEstimate) / architectEstimate * 100);
		}
		
		totalEstimatesData.setArchitectEstimate(architectEstimate);
		totalEstimatesData.setOriginalEstimate(originalEstimate);
		totalEstimatesData.setActualEstimate(actualEstimate);
		totalEstimatesData.setRemainingEstimate(remainingEstimate);
		totalEstimatesData.setDevEstimateDeviation(devEstimateDeviation);
		totalEstimatesData.setArchitectDevDeviation(architectDevDeviation);
		totalEstimatesData.setArchitectActualDeviation(architectActualDeviation);
		
		return totalEstimatesData;
	}
	
	private void populateEstimatesForProjects(ProjectData projectData) throws SearchException {
		String[] statuses = { ReportsConstant.STATUS_CLOSED };
		Collection<ReportIssueExtended> issues = getReportIssues(applicationUser, projectData.getProjectId(),
				SUBTASK_INCLUSION, statuses, ReportsConstant.STORY, null, null, null, null);
		TimeTrackingSummaryBeanExtended summaryBean = new TimeTrackingSummaryBeanExtended(issues);
		projectData.setEstimatesData(populateEstimates(summaryBean));
		projectData.setNumberOfExcludedSubstories(summaryBean.getNumberOfExcludedSubstories());
		projectData.setStoriesConsidered(issues.size());
		projectData.setTotalStories(getTotalStoriesCount(projectData));
	}
	
	private void populateProjectStatus(ProjectData projectData) throws SearchException {
		Query query = getSearchForParents(projectData.getProjectId(), null, ReportsConstant.ISSUETYPE_RELEASE, null, null, null, null);
		final SearchResults<Issue> searchResults = searchService.search( applicationUser, query, PagerFilter.getUnlimitedFilter());
		int releaseCount = searchResults.getTotal();
		projectData.setNumberOfRelease(releaseCount);
		if(releaseCount <= 1) {
			if(releaseCount == 1) {
				projectData.setProjectStatus(searchResults.getResults().get(0).getStatus().getSimpleStatus());
			}
		}
	}
	
	  private int getTotalStoriesCount(ProjectData projectData) throws SearchException {
		  Query query = getSearchForParents(projectData.getProjectId(), null, ReportsConstant.STORY, null, null, null, null);
		  final SearchResults<Issue> searchResults = searchService.search( applicationUser, query, PagerFilter.getUnlimitedFilter());
		  return searchResults.getTotal();
	  }
	  
	  private int getTotalSubStoriesCount(final String developerName, final Date fromDate, final Date toDate) throws SearchException {
		  Query query = getSearchForParents(null, null, ReportsConstant.SUB_STORY, developerName, fromDate, toDate, null);
		  final SearchResults<Issue> searchResults = searchService.search( applicationUser, query, PagerFilter.getUnlimitedFilter());
		  return searchResults.getTotal();
	  }
	  
	  public Collection<Status> getStatusesOfStoriesAndSubStoriesForProject(Long projectId) {
		  Collection<Status> statusesObjects = new HashSet<>();
			  issueTypeManager.getIssueTypes().stream()
			  	.filter(issueType -> issueType.getName().equalsIgnoreCase(ReportsConstant.STORY) || issueType.getName().equalsIgnoreCase(ReportsConstant.SUB_STORY))
			  	.forEach(issueType -> statusesObjects.addAll(workflowManager.getWorkflow(projectId,issueType.getId()).getLinkedStatusObjects()));  
		  return statusesObjects;
	  }
	
	/**
     * Get the list of issues to be displayed in the report.
     *
     * @param user             user
     * @param projectId        project id
     * @param versionId        version id
     * @param sortingOrder     sorting order
     * @param completedFilter  completed filter, e.g. {@link com.atlassian.jira.portal.FilterValuesGenerator#FILTER_INCOMPLETE_ISSUES}
     * @param subtaskInclusion whether to include subtasks with null or any fix for version
     * @return collection of issues
     * @throws SearchException if error occurs
     */
	public Collection<ReportIssueExtended> getReportIssues(final ApplicationUser user, final Long projectId,
			final SubTaskInclusionOption subtaskInclusion, final String[] statuses, final String issueType, final String jiraName, final Date fromDate, final Date toDate, final Long sprintId) throws SearchException {
		final Set<Issue> issuesFound = searchIssues(user, projectId, subtaskInclusion, statuses, issueType, jiraName, fromDate, toDate, sprintId);
		final Predicate reportIssueFilter = getCompletionFilter("all");

		// Filter out the sub stories which are not having the given statuses
		java.util.function.Predicate<Issue> reportSubTasksFilter = issue -> Arrays.asList(statuses)
				.contains(issue.getStatus().getName());
		final Set<SubTaskingIssueDecoratorExtended> transformedIssues = new IssueSubTaskTransformerExtended(
				reportSubTasksFilter).getIssues(issuesFound);

		final Comparator<ReportIssueExtended> completionComparator = getCompletionComparator("least");

		return new Processor(completionComparator, reportIssueFilter).getDecoratedIssues(transformedIssues);
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
        return PredicateUtils.truePredicate();
    }
	 
	private Comparator<ReportIssueExtended> getCompletionComparator(final String sortingOrder) {
        final Comparator<ReportIssueExtended> comparator = new Comparator<ReportIssueExtended>() {
            public int compare(final ReportIssueExtended reportIssue1, final ReportIssueExtended reportIssue2) {
                return KeyComparator.COMPARATOR.compare(reportIssue1.getIssue().getKey(), reportIssue2.getIssue().getKey());
            }
        };
        if (sortingOrder.equals(SortingValuesGenerator.SORT_BY_MOST_COMPLETED)) {
            return comparator;
        }

        return new ReverseComparator(comparator);
    }
	
	private Set<Issue> searchIssues(final ApplicationUser user, final Long projectId, final SubTaskInclusionOption subtaskInclusion, final String[] statuses, final String issueType, final String jiraName, final Date fromDate, final Date toDate, final Long sprintId) throws SearchException {
        final Query parentQuery = getSearchForParents(projectId, statuses, issueType, jiraName, fromDate, toDate, sprintId);
        final SearchResults<Issue> parentSearchResults = searchService.search( user, parentQuery, PagerFilter.getUnlimitedFilter());

        final Set<Issue> result = new LinkedHashSet<Issue>();
        final List<Issue> parentIssues = parentSearchResults.getResults();
        result.addAll(parentIssues);

        // search for subtasks if necessary
        final List<Issue> subtasks = reportSubTaskFetcher.getSubTasks(user, parentIssues, subtaskInclusion, false);
        result.addAll(subtasks);

        return result;
    }
	
	/*
     * get the initial search request that gets the stories for the given project and statuses
     */
    private Query getSearchForParents(final Long projectId, final String[] statuses, final String issueType, final String jiraName, final Date fromDate, final Date toDate, final Long sprintId) {
        final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        final JqlClauseBuilder builder = queryBuilder.where().issueType(issueType);
        SprintHelper sprintHelper = new SprintHelper(searchService, applicationUser, projectId);
        if(projectId != null) {
        	builder.and().project(projectId);
        }
        if(statuses != null) {
        	builder.and().status(statuses);
        }
        if(jiraName != null) {
        	builder.and().customField(developerFieldId).eq(jiraName);
        }
        if(fromDate != null) {
        	builder.and().resolutionDateAfter(fromDate);
        }
        if(toDate != null) {
        	builder.and().resolutionDateBetween(new Date(0), toDate);
        }
        if(sprintId != null) {
        	if(!sprintId.equals(Long.MIN_VALUE)) {
            	builder.and().customField(sprintHelper.getSprintCustomField().getIdAsLong()).eq(sprintId);
            }
        }

        return queryBuilder.buildQuery();
    }
    
	private boolean isValidProjectCategory(ProjectCategory projectCategory) {
		if(projectCategory != null && projectCategory.getName() != null) {
			return true;
		}
		return false;
	}
	
	private String setProjectLeadName(ApplicationUser applicationUser) {
		  if(applicationUser != null) {
			  return applicationUser.getName();
		  }
		  return "--";
	}
	
	private String setProjectCategoryName(ProjectCategory projectCategory) {
		return projectCategory.getName();
	}
	
	/**
	 * Only projects with valid Category name will be considered
	 * @param project
	 * @return
	 */
	private boolean isProjectInClientGroup(Project project) {
		if(project.getProjectCategory().getName().contains("Client.")) {
			return true;
		}
		return false;
	}

	public Long getDeveloperFieldId() {
		return developerFieldId;
	}

	/**
     * Responsible for taking a List of Issues and creating a List of {@link ReportIssue ReportIssues} for our report
     */
    static class Processor {
    	private final Comparator<ReportIssueExtended> comparator;
        private final Predicate reportIssueFilter;

		Processor(final Comparator<ReportIssueExtended> comparator) {
			this(comparator, PredicateUtils.truePredicate());
        }

        /**
         * Constructs a Processor that can also filter returned results using the reportIssueFilter.
         *
         * @param durationFormatter  for making the durations pretty
         * @param accuracyCalculator to work out the accuracy
         * @param comparator         to sort the issues
         * @param reportIssueFilter  to remove unwanted issues
         */
		Processor(final Comparator<ReportIssueExtended> comparator, final Predicate reportIssueFilter) {
            this.comparator = comparator;
            this.reportIssueFilter = reportIssueFilter;
        }

        List<ReportIssueExtended> getDecoratedIssues(final Collection<SubTaskingIssueDecoratorExtended> issues) {
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
				final ReportIssueExtended reportIssue = new ReportIssueExtended(issue, timeTrackingCalculator, comparator,
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
}
