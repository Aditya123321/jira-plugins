package com.tadigital.jira.plugin.report;

import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.ONLY_SELECTED_VERSION;
import static com.atlassian.jira.plugin.report.SubTaskInclusionOption.fromKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugin.report.ReportSubTaskFetcher;
import com.atlassian.jira.plugin.report.SubTaskInclusionOption;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.opensymphony.util.TextUtils;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.plugin.report.helper.EstimatesReportHelper;
import com.tadigital.jira.plugin.helper.ProjectHelper;
import com.tadigital.jira.plugin.helper.SprintHelper;

import webwork.action.ActionContext;


@Scanned
public class TimeEstimatesReport extends AbstractReport {

    private ApplicationProperties applicationProperties;
    private ProjectManager projectManager; 

    private TimeTrackingSummaryBeanExtended summaryBean;    
    
    TimeEstimatesReport() {
        this.applicationProperties = ComponentAccessor.getApplicationProperties();
        this.projectManager = ComponentAccessor.getProjectManager();
    }

    @Override
    public boolean showReport() {
        return applicationProperties.getOption(APKeys.JIRA_OPTION_TIMETRACKING);
    }

    // Return a map of parameters to pass through to the velocity template for this report
    Map<String, Object> getParams(final ProjectActionSupport action, final Map reqParams) throws PermissionException, GenericEntityException, SearchException {
    	JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    	SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		final ApplicationUser currentLoggedInUser = authenticationContext.getLoggedInUser();        
        //final Long projectId = action.getSelectedProjectObject().getId();
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
        final Map<String, Object> velocityParams = new HashMap<String, Object>();
        Long projectId = Long.parseLong((String) reqParams.get("projectValues"));
        
        EstimatesReportHelper reportHelper = new EstimatesReportHelper(projectManager,
        		searchService, 
        		ComponentAccessor.getComponentOfType(ReportSubTaskFetcher.class), 
        		ComponentAccessor.getComponentOfType(IssueTypeManager.class),
        		ComponentAccessor.getWorkflowManager(),
        		ComponentAccessor.getCustomFieldManager(),
        		currentLoggedInUser);
        
      //Take status to be used from request parameter. There can be multiple status selected. Default value is Closed
        Object issueStatusObjects = reqParams.get("status");
        String[] statuses = {ReportsConstant.STATUS_CLOSED};
        if(issueStatusObjects != null) {
        	Class<?> statusClass = issueStatusObjects.getClass();
        	if(statusClass.isArray()) {
        		statuses = (String[]) issueStatusObjects;
        	}
        	else if (statusClass.equals(String.class)){
        		statuses = new String[] {issueStatusObjects.toString()};
        	}
        }
        velocityParams.put("statuses",statuses);
        String sprint = (String) reqParams.get("sprintValues");
        Long sprintId;
        sprintId = sprint != null ? Long.parseLong(sprint) : Long.MIN_VALUE;
        
		
		//Reportkey and ProjectValues are used in status filter form
        velocityParams.put("reportKey", reqParams.get("reportKey"));
        ProjectHelper projectHelper = new ProjectHelper(authenticationContext, projectManager, ComponentAccessor.getGroupManager(),
				ComponentAccessor.getComponentOfType(ProjectRoleManager.class));
		
        if(projectId.equals(Long.MIN_VALUE)) {
        	Long selectedProjectId = action.getSelectedProjectId();
        	if(projectHelper.isValidUser(currentLoggedInUser, projectManager.getProjectObj(selectedProjectId))) {
            		projectId = selectedProjectId;
        	}
        	else {
        		velocityParams.put("none", true);
        	}
        }
        	Collection<Status> statusesObjects = reportHelper.getStatusesOfStoriesAndSubStoriesForProject(projectId);
        	Map<String, String> statusFilter = new LinkedHashMap<>();
        	statusesObjects.forEach(status -> statusFilter.put(status.getSimpleStatus().getName(), status.getSimpleStatus().getName()));
            velocityParams.put("statusFilter", statusFilter);
        	
        	final Project project = projectManager.getProjectObj(projectId);
            
            if(project != null) {
            	velocityParams.put("projectName", project.getName());
            }
            
        final Collection<ReportIssueExtended> issues = reportHelper.getReportIssues(currentLoggedInUser, projectId, subtaskInclusion, statuses, ReportsConstant.STORY, null, null, null, sprintId);
            summaryBean = new TimeTrackingSummaryBeanExtended(issues);
            int numberOfIssues = issues.size();
            
            velocityParams.put("report", this);
            velocityParams.put("action", action);
           // velocityParams.put("version", version);
            velocityParams.put("textUtils", textUtils);
            velocityParams.put("issues", issues);
            velocityParams.put("summaryBean", summaryBean);
            //velocityParams.put("sortingOrder", sortingOrder);
            //velocityParams.put("completedFilter", completedFilter);
            //velocityParams.put("versionIdString", versionIdString);
        velocityParams.put("remoteUser", currentLoggedInUser);
            velocityParams.put("issuesSize",numberOfIssues);
            //velocityParams.put("subtasksEnabled", subTaskManager.isSubTasksEnabled());
//            if (SubTaskInclusionOption.isValidKey(subtaskInclusionKey)) {
//                velocityParams.put("subtaskDescription", subtaskInclusion.getDescription(getI18nHelper()));
//            } else {
//                velocityParams.put("subtaskDescription", "unknown option: " + subtaskInclusionKey);
//            }
            velocityParams.put("architectEstimates",summaryBean.getArchitectEstimates());
            velocityParams.put("devEstimates",summaryBean.getDevEstimates());
            velocityParams.put("issueActualTimes", summaryBean.getIssueActualTimes());
            velocityParams.put("issueRemainingTimes", summaryBean.getIssueRemainingTimes());
            velocityParams.put("totalTimeEstimate", summaryBean.getTotalTimeEstimate());
            velocityParams.put("totalSubTaskTime", summaryBean.getSubTaskTotal());
            velocityParams.put("hasSubTask",summaryBean.getHasSubtask());
            velocityParams.put("subTaskArchitectEstimates",summaryBean.getSubTaskArchitectEstimates());
            velocityParams.put("subTaskDevEstimates",summaryBean.getSubTaskDevEstimates());
            velocityParams.put("subTaskActualTimes", summaryBean.getSubTaskActualTimes());
            velocityParams.put("subTaskRemainingEstimates", summaryBean.getSubTaskRemainingTimes());
            velocityParams.put("subTaskDevDeviation", summaryBean.getSubTaskDevDeviation());
            velocityParams.put("subTaskArchitectDevDeviation", summaryBean.getSubTaskArchitectDevDeviation());
            velocityParams.put("subTaskArchitectActualDeviation", summaryBean.getSubTaskArchitectActualDeviation());
            velocityParams.put("aggregateIssueEstimate", summaryBean.getAggregateActualTime());
            velocityParams.put("aggregateIssueRemainingTime", summaryBean.getAggregateIssueRemainingTime());
            velocityParams.put("aggregateTotal", summaryBean.getAggregateTotal());
            // Excel view params

            velocityParams.put("createDate", new Date());
            
            velocityParams.put("jiraBaseUrl", ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL));
            
            velocityParams.put("projectValues", projectId);
            //Populating filters
            SprintHelper sprintHelper = new SprintHelper(searchService, currentLoggedInUser, projectId);
            velocityParams.put("sprintFilter", sprintHelper.getSprintMap());
            velocityParams.put("sprint", sprintId);
            velocityParams.put("projectFilter", projectHelper.getProjects());
        
        return velocityParams;
    }

    // Generate a HTML view of report
    public String generateReportHtml(final ProjectActionSupport action, final Map reqParams) throws Exception {
        return descriptor.getHtml("view", getParams(action, reqParams));
    }

    // Generate an EXCEL view of report
	@Override
	public String generateReportExcel(final ProjectActionSupport action, final Map reqParams) throws Exception {
		final StringBuilder contentDispositionValue = new StringBuilder(50);
		contentDispositionValue.append("attachment;filename=\"").append(getDescriptor().getName());                                                     
		final Long projectId = Long.parseLong((String) reqParams.get("projectValues"));
		if(!projectId.equals(Long.MIN_VALUE)) {
			final String projectName = projectManager.getProjectObj(projectId).getName();
			contentDispositionValue.append("-").append(projectName);
		}
		contentDispositionValue.append(".xls\";");

		// Add header to fix JRA-8484
		final HttpServletResponse response = ActionContext.getResponse();
		response.addHeader("content-disposition", contentDispositionValue.toString());
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

    public int getCompletionPercentage() {
        return (int) (((float) summaryBean.getTimeSpent() / (float) (summaryBean.getTimeSpent() + summaryBean.getRemainingEstimate())) * 100F);
    }

    public int getAccuracyPercentage() {
        return AccuracyCalculatorExtended.Percentage.calculate(summaryBean.getOriginalEstimate(), summaryBean.getTimeSpent(),
                summaryBean.getRemainingEstimate());
    }

    private I18nHelper getI18nHelper() {
        return descriptor.getI18nBean();
    }

}
