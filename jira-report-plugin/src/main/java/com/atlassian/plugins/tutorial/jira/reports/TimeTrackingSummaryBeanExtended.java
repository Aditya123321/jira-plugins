package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.util.AggregateTimeTrackingBean;
import com.atlassian.jira.model.querydsl.CustomFieldOptionDTO;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.jira.component.ComponentAccessor;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.lang.Math;

import java.util.Collection;
import java.util.Collections;

import static com.atlassian.jira.component.ComponentAccessor.getCustomFieldManager;


public class TimeTrackingSummaryBeanExtended {

    private static final int MAX_GRAPH_WIDTH = 400;
    private static final BigDecimal HOURS_DIVIDE = new BigDecimal("3600");
    private final long originalEstimate;
    private final long timeSpent;
    private final long remainingEstimate;
    private final long aggregateOriginalEstimate;
    private final long aggregateTimeSpent;
    private final long aggregateRemainingEstimate;
    private Map<String,BigDecimal> deviation = new HashMap<>();;
    private Map<String,BigDecimal> devDeviation = new HashMap<>();;
    private Map<String,Long> devEstimates = new HashMap<>();
    private Map<String,Long> planEstimates = new HashMap<>();
    private long aggregateDevEstimates = 0;
    private long aggregatePlanEstimates = 0;
    private Map<String, Long> subTaskDevEstimates = new HashMap<>();
    private Map<String, Long> subTaskPlanEstimates = new HashMap<>();
    private Map<String, Boolean> hasSubtask = new HashMap<>();
    private static final boolean SUBTASK_ENABLED = true;
    private static final boolean SUBTASK_NOT_ENABLED = false;
    private Map<String, Integer> totalTimeEstimate = new HashMap<>();
    private Map<String, Integer> subTaskTotal = new HashMap<>();
    private Map<String, BigDecimal> subTaskDevDeviation = new HashMap<>();
    private Map<String, BigDecimal> subTaskDeviation = new HashMap<>();
    private long aggregateIssueRemainingTime = 0;
    private long aggregateIssueEstimate = 0;
    private long aggregateTotal = 0;
    private Map<String, Long> issueActualTimes = new HashMap<>();
    private Map<String, Long> issueRemainingTimes = new HashMap<>();
    private Map<String, Long> subTaskActualTimes = new HashMap<>();
    private Map<String, Long> subTaskRemainingTimes = new HashMap<>();

    TimeTrackingSummaryBeanExtended(long totOriginalEst, long totTimeSpent, long totRemainEst) {
        this.originalEstimate = totOriginalEst;
        this.timeSpent = totTimeSpent;
        this.remainingEstimate = totRemainEst;
        this.aggregateOriginalEstimate = totOriginalEst;
        this.aggregateTimeSpent = totTimeSpent;
        this.aggregateRemainingEstimate = totRemainEst;
    }

    TimeTrackingSummaryBeanExtended(Collection /* <ReportIssue> */ issues) {
        long totOriginalEst = 0;
        long totTimeSpent = 0;
        long totRemainEst = 0;
        long totAggOriginalEst = 0;
        long totAggTimeSpent = 0;
        long totAggRemainEst = 0;
        CustomField devEstimateId = null;
        CustomField planEstimateId = null;
        double devEstimate = 0;
        double planEstimate = 0;
        double subTaskDevEstimate = 0;
        double subTaskPlanEstimate = 0;
        List<CustomField> customFieldList = getCustomFieldManager().getCustomFieldObjects();
        for(CustomField customField : customFieldList ){
            if (customField.getName().equalsIgnoreCase("Dev Estimate")) {
                devEstimateId = getCustomFieldManager().getCustomFieldObject(customField.getIdAsLong());
            }
            if(customField.getName().equalsIgnoreCase("Planning Estimates")){
                planEstimateId = getCustomFieldManager().getCustomFieldObject(customField.getIdAsLong());
            }
        }
        for (final Object issue1 : issues) {
            long issueTotalTime = 0;
            final ReportIssueExtended reportIssue = ((ReportIssueExtended) issue1);
            final Issue issue = reportIssue.getIssue();
            try {
                devEstimate = Double.parseDouble(issue.getCustomFieldValue(devEstimateId).toString());
                planEstimate = Double.parseDouble(issue.getCustomFieldValue(planEstimateId).toString());
            }catch(NullPointerException e){
                //do nothing
            }
            aggregateDevEstimates += Math.round(devEstimate);
            aggregatePlanEstimates += Math.round(planEstimate);
            aggregateIssueEstimate += getLongValue(issue.getOriginalEstimate())/3600;
            aggregateIssueRemainingTime += getLongValue(issue.getEstimate())/3600;
            aggregateTotal += getLongValue(issue.getOriginalEstimate())/3600 + getLongValue(issue.getEstimate())/3600;
            totOriginalEst += getLongValue(issue.getOriginalEstimate());
            totTimeSpent += getLongValue(issue.getTimeSpent());
            totRemainEst += getLongValue(issue.getEstimate());
            long issueOriginal = getLongValue(issue.getOriginalEstimate());
            long issueRemaining = getLongValue(issue.getEstimate());
            issueTotalTime += getLongValue(issue.getOriginalEstimate()) + getLongValue(issue.getEstimate());
            totalTimeEstimate.put(issue.getSummary(), Math.round(issueTotalTime/3600));
            hasSubtask.put(issue.getSummary(), SUBTASK_NOT_ENABLED);
            issueActualTimes.put(issue.getSummary(), issueOriginal/3600);
            issueRemainingTimes.put(issue.getSummary(), issueRemaining/3600);
            for (final Object o : reportIssue.getSubTasks()) {
                long subTaskTotalTime= 0;
                Issue subTask = ((ReportIssueExtended) o).getIssue();
                try {
                    subTaskDevEstimate = Double.parseDouble(subTask.getCustomFieldValue(devEstimateId).toString());
                    subTaskPlanEstimate = Double.parseDouble(subTask.getCustomFieldValue(planEstimateId).toString());
                } catch(NullPointerException e){
                    //do nothing
                }
                subTaskDevEstimates.put(subTask.getSummary(),Math.round(subTaskDevEstimate));
                subTaskPlanEstimates.put(subTask.getSummary(), Math.round(subTaskPlanEstimate));
                subTaskTotalTime += getLongValue(subTask.getOriginalEstimate()) + getLongValue(subTask.getEstimate());
                aggregateDevEstimates += subTaskDevEstimate;
                aggregatePlanEstimates += subTaskPlanEstimate;
                totOriginalEst += getLongValue(subTask.getOriginalEstimate());
                totTimeSpent += getLongValue(subTask.getTimeSpent());
                totRemainEst += getLongValue(subTask.getEstimate());
                long subTaskOriginal = getLongValue(subTask.getOriginalEstimate());
                long subTaskRemaining = getLongValue(subTask.getEstimate());
                hasSubtask.put(issue.getSummary(), SUBTASK_ENABLED);
                subTaskTotal.put(subTask.getSummary(),Math.round(subTaskTotalTime/3600));
                if(subTaskPlanEstimate == 0){
                    subTaskDeviation.put(subTask.getSummary(),BigDecimal.ZERO);
                } else {
                    subTaskDeviation.put(subTask.getSummary(), BigDecimal.valueOf((subTaskDevEstimate - subTaskPlanEstimate) / subTaskPlanEstimate).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                if(subTaskDevEstimate == 0){
                    subTaskDevDeviation.put(subTask.getSummary(), BigDecimal.ZERO);
                } else {
                    subTaskDevDeviation.put(subTask.getSummary(), BigDecimal.valueOf((subTaskOriginal - (subTaskDevEstimate * 3600)) / (subTaskDevEstimate * 3600)).setScale(2, BigDecimal.ROUND_HALF_UP));
                }
                aggregateIssueEstimate += getLongValue(subTask.getOriginalEstimate())/3600;
                aggregateIssueRemainingTime += getLongValue(subTask.getEstimate())/3600;
                aggregateTotal += getLongValue(subTask.getOriginalEstimate())/3600 + getLongValue(subTask.getEstimate())/3600;
                subTaskRemainingTimes.put(subTask.getSummary(), subTaskOriginal/3600);
                subTaskActualTimes.put(subTask.getSummary(), subTaskRemaining/3600);
            }
            final AggregateTimeTrackingBean aggregates = reportIssue.getAggregateBean();
            totAggOriginalEst += getLongValue(aggregates.getOriginalEstimate());
            totAggTimeSpent += getLongValue(aggregates.getTimeSpent());
            totAggRemainEst += getLongValue(aggregates.getRemainingEstimate());

            if(planEstimate == 0) {
                deviation.put(issue.getSummary(), BigDecimal.ZERO);
            } else{
                deviation.put(issue.getSummary(), BigDecimal.valueOf(((devEstimate) - planEstimate) / planEstimate).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            if(devEstimate == 0){
                devDeviation.put(issue.getSummary(), BigDecimal.ZERO);
            } else {
                devDeviation.put(issue.getSummary(), BigDecimal.valueOf((issueOriginal - (devEstimate * 3600)) / (devEstimate * 3600)).setScale(2, BigDecimal.ROUND_HALF_UP));
            }
            devEstimates.put(issue.getSummary(),Math.round(devEstimate));
            planEstimates.put(issue.getSummary(),Math.round(planEstimate));

        }
        this.originalEstimate = totOriginalEst/3600;
        this.timeSpent = totTimeSpent/3600;
        this.remainingEstimate = totRemainEst/3600;
        this.aggregateOriginalEstimate = totAggOriginalEst/3600;
        this.aggregateTimeSpent = totAggTimeSpent/3600;
        this.aggregateRemainingEstimate = totAggRemainEst/3600;
    }

    private long getLongValue(Long input) {
        return (input == null) ? 0 : input.longValue();
    }

    public long getOriginalEstimate() {
        return originalEstimate;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public Map<String,BigDecimal> getDevDeviation() { return devDeviation;}

    public Map<String,BigDecimal> getDeviation() {return deviation;}

    public long getRemainingEstimate() {
        return remainingEstimate;
    }

    public long getAggregateOriginalEstimate() {
        return aggregateOriginalEstimate;
    }

    public long getAggregateRemainingEstimate() {
        return aggregateRemainingEstimate;
    }

    public long getAggregateTimeSpent() {
        return aggregateTimeSpent;
    }

    public int getCompletionTotalWidth() {
        if (timeSpent + remainingEstimate > originalEstimate) {
            return MAX_GRAPH_WIDTH;
        } else {
            return (int) (((float) (timeSpent + remainingEstimate) / (float) originalEstimate) * MAX_GRAPH_WIDTH);
        }
    }

    public int getEstimationTotalWidth() {
        return MAX_GRAPH_WIDTH;
    }

    public int getCompletedWidth() {
        return (int) (((float) timeSpent / (float) (timeSpent + remainingEstimate)) * getCompletionTotalWidth());
    }

    public int getIncompleteWidth() {
        return getCompletionTotalWidth() - getCompletedWidth();
    }

    public int getEstimateWidth() {
        if (originalEstimate > timeSpent + remainingEstimate) {
            return getEstimationTotalWidth();
        } else {
            return (int) (((float) originalEstimate / (float) (remainingEstimate + timeSpent)) * getEstimationTotalWidth());
        }
    }

    public int getUnderEstimateWidth() {
        if (originalEstimate > timeSpent + remainingEstimate) {
            return 0;
        } else {
            return getEstimationTotalWidth() - getEstimateWidth();
        }
    }

    public int getOverEstimateWidth() {
        if (originalEstimate > timeSpent + remainingEstimate) {
            return getEstimationTotalWidth() - (getCompletedWidth() + getIncompleteWidth());
        } else {
            return 0;
        }
    }

    public Map<String,Long> getDevEstimates() {return devEstimates;}

    public Map<String, Long> getPlanEstimates() {return planEstimates;}

    public long getAggregateDevEstimates() {return aggregateDevEstimates;}

    public long getAggregatePlanEstimates() {return aggregatePlanEstimates;}

    public Map<String, Long> getSubTaskDevEstimates() {return subTaskDevEstimates;}

    public Map<String, Long> getSubTaskPlanEstimates() {return subTaskPlanEstimates;}

    public Map<String,Boolean> getHasSubtask() {return hasSubtask;}

    public Map<String, Integer> getTotalTimeEstimate() {return totalTimeEstimate;}

    public Map<String, Integer> getSubTaskTotal() {return subTaskTotal;}

    public Map<String, BigDecimal> getSubTaskDevDeviation() {return subTaskDevDeviation;}

    public Map<String, BigDecimal> getSubTaskDeviation() {return subTaskDeviation;}

    public long getAggregateIssueRemainingTime() {return aggregateIssueRemainingTime;}

    public long getAggregateIssueEstimate() {return aggregateIssueEstimate;}

    public long getAggregateTotal() {return aggregateTotal;}

    public Map<String, Long> getSubTaskActualTimes() {return subTaskActualTimes;}

    public Map<String, Long> getSubTaskRemainingTimes() {return subTaskRemainingTimes;}

    public Map<String, Long> getIssueActualTimes() {return issueActualTimes;}

    public Map<String, Long> getIssueRemainingTimes() {return issueRemainingTimes;}
}
