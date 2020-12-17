package com.tadigital.jira.plugin.report;

import static com.atlassian.jira.component.ComponentAccessor.getCustomFieldManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//import org.apache.log4j.Logger;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.util.AggregateTimeTrackingBean;
import com.tadigital.jira.plugin.constants.ReportsConstant;


public class TimeTrackingSummaryBeanExtended {

	//private static final Logger LOG = Logger.getLogger(TimeTrackingSummaryBeanExtended.class);
	
    private static final int MAX_GRAPH_WIDTH = 400;
    private static final double HOURS_DIVIDE = 3600d;
    
    private final long originalEstimate;
    private final long timeSpent;
    private final long remainingEstimate;
    private final long aggregateOriginalEstimate;
    private final long aggregateTimeSpent;
    private final long aggregateRemainingEstimate;
    private long aggregateDevDeviation;
    private long aggregateArchitectDevDeviation;
    private long aggregateArchitectActualDeviation;
	private Map<String,Long> architectOriginalDeviation = new HashMap<>();;
    private Map<String,Long> devDeviation = new HashMap<>();;
    private Map<String,Double> devEstimates = new HashMap<>();
    private Map<String,Double> architectEstimates = new HashMap<>();
    private double aggregateDevEstimates = 0;
    private double aggregateArchitectEstimates = 0;
    private Map<String, Double> subTaskDevEstimates = new HashMap<>();
    private Map<String, Double> subTaskArchitectEstimates = new HashMap<>();
    private Map<String, Boolean> hasSubtask = new HashMap<>();
    private static final boolean SUBTASK_ENABLED = true;
    private static final boolean SUBTASK_NOT_ENABLED = false;
    private Map<String, Integer> totalTimeEstimate = new HashMap<>();
    private Map<String, Integer> subTaskTotal = new HashMap<>();
    private Map<String, Long> subTaskDevDeviation = new HashMap<>();
    private Map<String, Long> subTaskArchitectDevDeviation = new HashMap<>();
    private Map<String, Long> subTaskArchitectActualDeviation = new HashMap<>();
    private double aggregateIssueRemainingTime = 0;
    private double aggregateActualTime = 0;
    private long aggregateTotal = 0;
    private Map<String, Double> issueActualTimes = new HashMap<>();
    private Map<String, Double> issueRemainingTimes = new HashMap<>();
    private Map<String, Double> subTaskActualTimes = new HashMap<>();
    private Map<String, Double> subTaskRemainingTimes = new HashMap<>();
    private Map<String, Long> architectActualDeviation = new HashMap<>();
    private int numberOfExcludedSubstories = 0;
    
    TimeTrackingSummaryBeanExtended(long totOriginalEst, long totTimeSpent, long totRemainEst) {
        this.originalEstimate = totOriginalEst;
        this.timeSpent = totTimeSpent;
        this.remainingEstimate = totRemainEst;
        this.aggregateOriginalEstimate = totOriginalEst;
        this.aggregateTimeSpent = totTimeSpent;
        this.aggregateRemainingEstimate = totRemainEst;
    }

    public TimeTrackingSummaryBeanExtended(Collection<ReportIssueExtended> /* <ReportIssue> */ issues) {
        long totOriginalEst = 0;
        long totTimeSpent = 0;
        long totRemainEst = 0;
        long totAggOriginalEst = 0;
        long totAggTimeSpent = 0;
        long totAggRemainEst = 0;
        CustomField architectEstimateId = null;
        
        List<CustomField> customFieldList = getCustomFieldManager().getCustomFieldObjects();
        for(CustomField customField : customFieldList ){
            if(customField.getName().equalsIgnoreCase(ReportsConstant.ARCHITECT_ESTIMATES_CF)){
                architectEstimateId = getCustomFieldManager().getCustomFieldObject(customField.getIdAsLong());
            }
        }
        
        for (final Object issue1 : issues) {
        	double devEstimate = 0, architectEstimate = 0;
        	
            long issueTotalTime = 0;
            final ReportIssueExtended reportIssue = ((ReportIssueExtended) issue1);
            final Issue issue = reportIssue.getIssue();
            
            //Get Planning Estimate
            if(issue.getCustomFieldValue(architectEstimateId) != null) {
            	architectEstimate = Double.parseDouble(issue.getCustomFieldValue(architectEstimateId).toString());
            }
            
            //Get Dev Estimate
            if(issue.getOriginalEstimate() != null) {
            	devEstimate = convertSecondsToHours(issue.getOriginalEstimate());
            }
            
            //Get Actual Time - Actual Time should be taken as zero if its null.
            double issueActualTime = convertSecondsToHours(getLongValue(issue.getTimeSpent()));
            
            //Get Remaining Time - Remaining Time should be taken as zero if its null. It can be null when there is no original estimate.
            double issueRemaining = convertSecondsToHours(getLongValue(issue.getEstimate()));

            //Add story stats to total
            aggregateDevEstimates += devEstimate;
            aggregateArchitectEstimates += architectEstimate;
            aggregateActualTime += issueActualTime;
            aggregateIssueRemainingTime += issueRemaining;
            
            aggregateTotal += getLongValue(issue.getOriginalEstimate())/3600 + getLongValue(issue.getEstimate())/3600;
            totOriginalEst += getLongValue(issue.getOriginalEstimate());
            totTimeSpent += getLongValue(issue.getTimeSpent());
            totRemainEst += getLongValue(issue.getEstimate());
            issueTotalTime += getLongValue(issue.getOriginalEstimate()) + getLongValue(issue.getEstimate());
            totalTimeEstimate.put(issue.getKey(), Math.round(issueTotalTime/3600));
            hasSubtask.put(issue.getKey(), SUBTASK_NOT_ENABLED);
            
            if(reportIssue.hasSubTasks()) {
            	for (final Object o : reportIssue.getSubTasks()) {
                	Double subTaskDevEstimate = null, subTaskArchitectEstimate = null;
                    
                    Issue subTask = ((ReportIssueExtended) o).getIssue();
                    
                    //Get Planning Estimate
                    if(subTask.getCustomFieldValue(architectEstimateId) != null) {
                    	subTaskArchitectEstimate = Double.parseDouble(subTask.getCustomFieldValue(architectEstimateId).toString());
                    	subTaskArchitectEstimates.put(subTask.getKey(), subTaskArchitectEstimate);
                    }
                                   
                    //Get Dev Estimate
                    if(subTask.getOriginalEstimate() != null) {
                    	subTaskDevEstimate = convertSecondsToHours(subTask.getOriginalEstimate());
                    	subTaskDevEstimates.put(subTask.getKey(),subTaskDevEstimate);
                    }
                    
                    //Get Actual Time - Actual Time should be taken as zero if its null.
                    double subTaskActualTime = convertSecondsToHours(getLongValue(subTask.getTimeSpent()));
                    subTaskActualTimes.put(subTask.getKey(), subTaskActualTime);
                    
                    //Get Remaining Time - Remaining Time should be taken as zero if its null. It can be null when there is no original estimate.
                    double subTaskRemaining = convertSecondsToHours(getLongValue(subTask.getEstimate()));
                    subTaskRemainingTimes.put(subTask.getKey(), subTaskRemaining);
                    
                    //Add the estimates and actual and remaining time to story and total only if all estimates are present. 
                    //If any estimate is missing for the sub-story, then data from this sub-story should not be rolled up to the story level and in the total.
                    if(subTaskArchitectEstimate != null && subTaskDevEstimate != null) {
                        architectEstimate += subTaskArchitectEstimate;
                        devEstimate += subTaskDevEstimate;
                    	issueActualTime += subTaskActualTime;
                        issueRemaining += subTaskRemaining;
                        
                        aggregateArchitectEstimates += subTaskArchitectEstimate;
                        aggregateDevEstimates += subTaskDevEstimate;
                        aggregateActualTime += subTaskActualTime;
                        aggregateIssueRemainingTime += subTaskRemaining;
                    }
                    
                    else {
                    	numberOfExcludedSubstories++;
                    }
                    
                    //Calculate Deviations
                    subTaskArchitectDevDeviation.put(subTask.getKey(), calculateArchitectDevDeviation(subTaskDevEstimate, subTaskArchitectEstimate));
                    subTaskDevDeviation.put(subTask.getKey(), calculateDevDeviation(subTaskDevEstimate, subTaskActualTime, subTaskRemaining));
                    subTaskArchitectActualDeviation.put(subTask.getKey(), calculateArchitectActualDeviation(subTaskArchitectEstimate, subTaskActualTime, subTaskRemaining));

                    long subTaskTotalTime = getLongValue(subTask.getOriginalEstimate()) + getLongValue(subTask.getEstimate());
                    
                    totOriginalEst += getLongValue(subTask.getOriginalEstimate());
                    totTimeSpent += getLongValue(subTask.getTimeSpent());
                    totRemainEst += getLongValue(subTask.getEstimate());
                    hasSubtask.put(issue.getKey(), SUBTASK_ENABLED);
                    subTaskTotal.put(subTask.getKey(),Math.round(subTaskTotalTime/3600));
                    aggregateTotal += getLongValue(subTask.getOriginalEstimate())/3600 + getLongValue(subTask.getEstimate())/3600;
                }
            }
            
            final AggregateTimeTrackingBean aggregates = reportIssue.getAggregateBean();
            totAggOriginalEst += getLongValue(aggregates.getOriginalEstimate());
            totAggTimeSpent += getLongValue(aggregates.getTimeSpent());
            totAggRemainEst += getLongValue(aggregates.getRemainingEstimate());

            if(architectEstimate == 0) {
                architectOriginalDeviation.put(issue.getKey(), 0L);
            } else{
                architectOriginalDeviation.put(issue.getKey(), Math.round((devEstimate - architectEstimate) / architectEstimate * 100));
            }
            if(devEstimate == 0){
                devDeviation.put(issue.getKey(), 0L);
            } else {
                devDeviation.put(issue.getKey(), Math.round((issueActualTime + issueRemaining - devEstimate) / devEstimate * 100));
            }
            
            if(architectEstimate == 0){
            	architectActualDeviation.put(issue.getKey(), 0L);
            } else {
            	architectActualDeviation.put(issue.getKey(), Math.round((issueActualTime + issueRemaining - architectEstimate) / architectEstimate * 100));
            }
            
            //Formatting of double values of estimate below is needed because after adding two formatted doubles, the result is not formatted
            devEstimates.put(issue.getKey(),formatDouble(devEstimate));
            architectEstimates.put(issue.getKey(),formatDouble(architectEstimate));
            issueActualTimes.put(issue.getKey(), formatDouble(issueActualTime));
            issueRemainingTimes.put(issue.getKey(), formatDouble(issueRemaining));
        }
        this.originalEstimate = totOriginalEst/3600;
        this.timeSpent = totTimeSpent/3600;
        this.remainingEstimate = totRemainEst/3600;
        this.aggregateOriginalEstimate = totAggOriginalEst/3600;
        this.aggregateTimeSpent = totAggTimeSpent/3600;
        this.aggregateRemainingEstimate = totAggRemainEst/3600;
        if(this.aggregateDevEstimates == 0) {
        	this.aggregateDevDeviation = 0;
        } else {
        	aggregateDevDeviation = Math.round((aggregateActualTime + aggregateIssueRemainingTime - aggregateDevEstimates) / aggregateDevEstimates * 100);
        }
        
        if(this.aggregateArchitectEstimates == 0) {
        	aggregateArchitectDevDeviation = 0;
        } else {
        	aggregateArchitectDevDeviation = Math.round((aggregateDevEstimates - aggregateArchitectEstimates) / aggregateArchitectEstimates * 100);
        }
        
        if(this.aggregateArchitectEstimates == 0) {
        	aggregateArchitectActualDeviation = 0;
        } else {
        	aggregateArchitectActualDeviation = Math.round((aggregateActualTime + aggregateIssueRemainingTime - aggregateArchitectEstimates) / aggregateArchitectEstimates * 100);
        }
        
        //Formatting of double values of estimate below is needed because after adding two formatted doubles, the result is not formatted
        aggregateDevEstimates = formatDouble(aggregateDevEstimates);
        aggregateArchitectEstimates = formatDouble(aggregateArchitectEstimates);
        aggregateIssueRemainingTime = formatDouble(aggregateIssueRemainingTime);
        aggregateActualTime = formatDouble(aggregateActualTime);
    }
    
    
    
	private Long calculateDevDeviation(Double devEstimate, double actualTime, double remainingTime) {
		if(devEstimate == null) {
			return null;
		}
		if(devEstimate == 0){
		    return 0L;
		} else {
		    return Math.round((actualTime + remainingTime - devEstimate) / devEstimate * 100);
		}
	}

	private Long calculateArchitectDevDeviation(Double devEstimate, Double planEstimate) {
		if(devEstimate == null || planEstimate == null) {
			return null;
		}
		if(planEstimate == 0){
			return 0L;
		} else {
			return Math.round((devEstimate - planEstimate) / planEstimate * 100);
		}
	}
	
	private Long calculateArchitectActualDeviation(Double architectEstimate, double actualTime, double remainingTime) {
		if(architectEstimate == null) {
			return null;
		}
		if(architectEstimate == 0){
		    return 0L;
		} else {
		    return Math.round((actualTime + remainingTime - architectEstimate) / architectEstimate * 100);
		}
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

    public Map<String,Long> getDevDeviation() { return devDeviation;}

    public Map<String,Long> getArchitectOriginalDeviation() {return architectOriginalDeviation;}

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
    
    private double convertSecondsToHours(long seconds) {
    	double hours = seconds/HOURS_DIVIDE;
    	return formatDouble(hours);
    }

    private double formatDouble(double value) {
    	BigDecimal bd = BigDecimal.valueOf(value);
    	return bd.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
    
    public Map<String,Double> getDevEstimates() {return devEstimates;}

    public Map<String, Double> getArchitectEstimates() {return architectEstimates;}

    public double getAggregateDevEstimates() {return aggregateDevEstimates;}

    public double getAggregateArchitectEstimates() {return aggregateArchitectEstimates;}

    public Map<String, Double> getSubTaskDevEstimates() {return subTaskDevEstimates;}

    public Map<String, Double> getSubTaskArchitectEstimates() {return subTaskArchitectEstimates;}

    public Map<String,Boolean> getHasSubtask() {return hasSubtask;}

    public Map<String, Integer> getTotalTimeEstimate() {return totalTimeEstimate;}

    public Map<String, Integer> getSubTaskTotal() {return subTaskTotal;}

    public Map<String, Long> getSubTaskDevDeviation() {return subTaskDevDeviation;}

    public Map<String, Long> getSubTaskArchitectDevDeviation() {return subTaskArchitectDevDeviation;}

    public Map<String, Long> getSubTaskArchitectActualDeviation() {
		return subTaskArchitectActualDeviation;
	}

	public double getAggregateIssueRemainingTime() {return aggregateIssueRemainingTime;}

    public double getAggregateActualTime() {return aggregateActualTime;}

    public long getAggregateTotal() {return aggregateTotal;}

    public Map<String, Double> getSubTaskActualTimes() {return subTaskActualTimes;}

    public Map<String, Double> getSubTaskRemainingTimes() {return subTaskRemainingTimes;}

    public Map<String, Double> getIssueActualTimes() {return issueActualTimes;}

    public Map<String, Double> getIssueRemainingTimes() {return issueRemainingTimes;}
    
    public long getAggregateDevDeviation() {
		return aggregateDevDeviation;
	}

	public long getAggregateArchitectDevDeviation() {
		return aggregateArchitectDevDeviation;
	}

	public long getAggregateArchitectActualDeviation() {
		return aggregateArchitectActualDeviation;
	}

	public Map<String, Long> getArchitectActualDeviation() {
		return architectActualDeviation;
	}

	public int getNumberOfExcludedSubstories() {
		return numberOfExcludedSubstories;
	}
}
