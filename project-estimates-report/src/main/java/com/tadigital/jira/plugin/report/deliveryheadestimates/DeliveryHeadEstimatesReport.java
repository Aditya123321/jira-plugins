package com.tadigital.jira.plugin.report.deliveryheadestimates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.plugin.report.ReportSubTaskFetcher;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.plugin.data.ProjectData;
import com.tadigital.jira.plugin.report.helper.EstimatesReportHelper;
import com.tadigital.jira.plugin.report.helper.ProjectCategoryHelper;

import webwork.action.ActionContext;

public class DeliveryHeadEstimatesReport extends AbstractReport {
	
	private static final String ALL_CLIENT_PROJECTS = "All Client Projects";
	private static final ProjectManager projectManager = ComponentAccessor.getProjectManager();
	
	public String generateReportHtml(ProjectActionSupport projectActionSupport, Map map) throws Exception {
		return descriptor.getHtml("view", getVelocityParams(projectActionSupport, map));
	}
	
	// Generate an EXCEL view of report
		@Override
		public String generateReportExcel(final ProjectActionSupport action, final Map reqParams) throws Exception {
			final StringBuilder contentDispositionValue = new StringBuilder(50);
			contentDispositionValue.append("attachment;filename=\"").append(getDescriptor().getName());
			Long categoryId = Long.parseLong((String) reqParams.get("projectCategoryValues"));
			String category = null;
			if(!categoryId.equals(Long.MIN_VALUE)) {
				if(categoryId.equals(Long.MAX_VALUE)) {
					category = ALL_CLIENT_PROJECTS;
				}
				else {
					category = projectManager.getProjectCategory(categoryId).getName();
				}
				contentDispositionValue.append("-").append(category);
			}
			contentDispositionValue.append(".xls\";");
			// Add header to fix JRA-8484
			final HttpServletResponse response = ActionContext.getResponse();
			response.addHeader("content-disposition", contentDispositionValue.toString());
			return descriptor.getHtml("excel", getVelocityParams(action, reqParams));
		}

		@Override
		public boolean isExcelViewSupported() {
			return true;
		}

	private Map<String, Object> getVelocityParams(ProjectActionSupport action, Map reqParams) throws SearchException {

		final Map<String, Object> velocityParams = new HashMap<>();
		
		EstimatesReportHelper estimatesReportHelper = new EstimatesReportHelper(projectManager,
				ComponentAccessor.getComponentOfType(SearchService.class),
				ComponentAccessor.getComponentOfType(ReportSubTaskFetcher.class),
				ComponentAccessor.getComponentOfType(IssueTypeManager.class), ComponentAccessor.getWorkflowManager(),
				ComponentAccessor.getCustomFieldManager(),
				ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
		
		Long categoryId = Long.parseLong((String) reqParams.get("projectCategoryValues"));
		final String categoryName = "categoryName";
		
		ProjectCategoryHelper projectCategoryHelper = new ProjectCategoryHelper(
				ComponentAccessor.getJiraAuthenticationContext(), projectManager,
				ComponentAccessor.getGroupManager());
		velocityParams.put("projectCategoryFilter", projectCategoryHelper.populateProjectCategories());
		
		velocityParams.put("reportKey", reqParams.get("reportKey"));
		
		if(categoryId.equals(Long.MIN_VALUE)) {
			velocityParams.put("none", true);
		}
		else {
			if (categoryId == Long.MAX_VALUE) {
				velocityParams.put(categoryName, ALL_CLIENT_PROJECTS);
			} else {
				if (projectManager.getProjectCategory(categoryId).getName() != null) {

					velocityParams.put(categoryName,
							projectManager.getProjectCategory(categoryId).getName());
				} else {
					velocityParams.put(categoryName, "Category is Not Defined");
				}
			}

			List<ProjectData> projectsData = estimatesReportHelper.getAllProjectsStatistics(categoryId);
			velocityParams.put("projectsData", projectsData);

			
			velocityParams.put("story", ReportsConstant.STORY);
			velocityParams.put("closed", ReportsConstant.STATUS_CLOSED);
			velocityParams.put("projectCategory", categoryId);
			velocityParams.put("jiraBaseUrl", ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL));
		}
		return velocityParams;

	}
}
