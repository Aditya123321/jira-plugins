package com.tadigital.jira.plugin.report;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.util.AggregateTimeTrackingBean;
import com.atlassian.jira.issue.util.AggregateTimeTrackingCalculator;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.google.common.collect.Lists;
import com.tadigital.jira.plugin.constants.ReportsConstant;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ReportIssueExtended {
    private static final Long ZERO = new Long(0);

	private static List transformSubTasks(Issue issue, final AggregateTimeTrackingCalculator aggregateCalculator, Comparator comparator, Predicate issueInclusionPredicate) {
        if (issue.getSubTaskObjects() == null || issue.getSubTaskObjects().isEmpty()) {
            return Collections.emptyList();
        }

        final List reportIssueSubTasks = Lists.newArrayList(issue.getSubTaskObjects());
        CollectionUtils.transform(reportIssueSubTasks, new Transformer() {
            public Object transform(Object object) {
                Issue issue = (Issue) object;
				return new ReportIssueExtended(issue, aggregateCalculator.getAggregates(issue), new ArrayList<Object>(), false);
            }
        });
        Collections.sort(reportIssueSubTasks, comparator);
        CollectionUtils.filter(reportIssueSubTasks, issueInclusionPredicate);
        return reportIssueSubTasks;
    }

    @JiraImport
    private final Issue issue;
    @Autowired
    private final AggregateTimeTrackingBean aggregateBean;
    @JiraImport
    private final Collection<Object> subTasks;
    @JiraImport
    private final boolean isOrphan;

	private ReportIssueExtended(Issue issue, AggregateTimeTrackingBean aggregateBean, List<Object> subTasks, boolean isOrphan) {
        this.issue = issue;
        this.aggregateBean = aggregateBean;
        this.subTasks = new ArrayList<>();
        this.isOrphan = isOrphan;
        // Filtering of subtasks should be done through predicate filter ideally. Doing it here for now because of lack of time.
        for(Object o: subTasks) {
			final ReportIssueExtended subtask = ((ReportIssueExtended) o);
        	if((subtask).getIssueType().getName().equalsIgnoreCase(ReportsConstant.SUB_STORY)) {
        		this.subTasks.add(subtask);
        	}
        }
    }

	public ReportIssueExtended(Issue issue, AggregateTimeTrackingCalculator aggregateCalculator, Comparator comparator, Predicate issueInclusionPredicate) {
        // all externally created ReportIssues are orphans, whether they are subtasks or not.
		this(issue, aggregateCalculator.getAggregates(issue), transformSubTasks(issue, aggregateCalculator, comparator, issueInclusionPredicate), true);
    }

    public String getKey() {
        return issue.getKey();
    }

    public String getSummary() {
        return issue.getSummary();
    }

    public IssueType getIssueType() {
        return issue.getIssueTypeObject();
    }

    public Priority getPriority() {
        return issue.getPriorityObject();
    }

    public Status getStatus() {
        return issue.getStatusObject();
    }

    Long getAggregateRemainingEstimateLong(Long defaultValue) {
        return getNotNull(aggregateBean.getRemainingEstimate(), defaultValue);
    }

    Long getAggregateOriginalEstimateLong(Long defaultValue) {
        return getNotNull(aggregateBean.getOriginalEstimate(), defaultValue);
    }

    static Long getNotNull(Long num, Long defaultValue) {
        return (num == null) ? defaultValue : num;
    }

    public String getAccuracyPercentage() {
        return getAccuracyPercentage(issue.getOriginalEstimate(), issue.getEstimate(), issue.getTimeSpent());
    }

    public String getAggregateAccuracyPercentage() {
        return getAccuracyPercentage(aggregateBean.getOriginalEstimate(), aggregateBean.getRemainingEstimate(), aggregateBean.getTimeSpent());
    }

    private String getAccuracyPercentage(Long originalEstLong, Long timeEstLong, Long timeSpentLong) {
        long originalEst = getLongNullSafe(originalEstLong);
        if (originalEst == 0) {
            return "";
        }
        long timeEst = getLongNullSafe(timeEstLong);
        long timeSpent = getLongNullSafe(timeSpentLong);

        return "" + AccuracyCalculatorExtended.Percentage.calculate(originalEst, timeSpent, timeEst);
    }

    /**
     * Are there are any values against this issue at all.
     *
     * @return true if any times have been logged or estimated.
     */
    public boolean isTimeTracked() {
        return isTracked(issue.getOriginalEstimate(), issue.getEstimate(), issue.getTimeSpent());
    }

    /**
     * Are there are any aggregate values against this issue at all.
     *
     * @return true if any times have been logged or estimated in aggregate.
     */
    public boolean isAggregateTimeTracked() {
        return isTracked(aggregateBean.getOriginalEstimate(), aggregateBean.getRemainingEstimate(), aggregateBean.getTimeSpent());
    }

    static boolean isTracked(Long original, Long remaining, Long timeSpent) {
        return getLongNullSafe(original) != 0 || getLongNullSafe(remaining) != 0 || getLongNullSafe(timeSpent) != 0;
    }

    public boolean hasOriginalEstimate() {
        return issue.getOriginalEstimate() != null;
    }

    public boolean hasAggregateOriginalEstimate() {
        return aggregateBean.getOriginalEstimate() != null;
    }

    public Collection<Object> getSubTasks() {
        return subTasks;
    }

	public Issue getIssue() {
        return issue;
    }

    public boolean isOrphan() {
        return isOrphan;
    }

    public boolean isSubTask() {
        return issue.getParentObject() != null;
    }

    public Issue getParent() {
        return issue.getParentObject();
    }

    AggregateTimeTrackingBean getAggregateBean() {
        return aggregateBean;
    }

    boolean isAggregateComplete() {
        return !(getAggregateRemainingEstimateLong(ZERO).longValue() > 0);
    }

    /**
     * Converts the Long object to primitive long. Returns zero for null.
     *
     * @param value value to convert
     * @return primitive long value or zero if null
     */
    private static long getLongNullSafe(Long value) {
        return value == null ? 0 : value.longValue();
    }

	public boolean hasSubTasks() {
		return CollectionUtils.isEmpty(this.subTasks) ? false : true;
	}
}
