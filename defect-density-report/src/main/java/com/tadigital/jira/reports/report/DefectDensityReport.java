package com.tadigital.jira.reports.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.tadigital.jira.plugin.helper.ProjectHelper;
import com.tadigital.jira.plugin.helper.SprintHelper;

import webwork.action.ActionContext;

@Scanned
@SuppressWarnings("rawtypes")
public class DefectDensityReport extends AbstractReport {

	private static final Logger log = Logger.getLogger(DefectDensityReport.class);
	private ManagingFilters managingFilters;

	DefectDensityReport(final DurationFormatterExtended durationFormatter) {
	}

	@Inject
	public DefectDensityReport() {
		this(new DurationFormatterExtendedImpl(new I18nBean(), ComponentAccessor.getJiraDurationUtils()));
	}

	public String generateReportHtml(ProjectActionSupport action, Map reqParams) throws Exception {

		return descriptor.getHtml("defectDensityView", getVelocityParams(action, reqParams));
	}

	private Map<String, Object> getVelocityParams(ProjectActionSupport action, Map reqParams) throws SearchException, JSONException, IOException {
		JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
		final ApplicationUser loggedInUser = authenticationContext.getLoggedInUser();
		ProjectManager projectManager = ComponentAccessor.getProjectManager();
		SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
		Long projectId = Long.parseLong((String) reqParams.get("projectId"));
		
		String sprint = (String) reqParams.get("sprintId");
        Long sprintId;
        sprintId = sprint != null ? Long.parseLong(sprint) : Long.MIN_VALUE;
		
		final Map<String, Object> velocityParams = new HashMap<String, Object>();
		ProjectHelper projectHelper = new ProjectHelper(authenticationContext,
				projectManager, ComponentAccessor.getGroupManager(),
				ComponentAccessor.getComponent(ProjectRoleManager.class));

		managingFilters = new ManagingFilters(loggedInUser,
				searchService, projectManager);
		Long selectedProjectId = action.getSelectedProjectId();
    	if(projectHelper.isValidUser(loggedInUser, projectManager.getProjectObj(selectedProjectId))) {
    		if(projectId == selectedProjectId || projectId == Long.MIN_VALUE) {
    		projectId = selectedProjectId;
    		}
    	}
		if(projectId != Long.MIN_VALUE) {
		velocityParams.put("projectName", projectManager.getProjectObj(projectId).getName());

		List<DeveloperAttributesBean> dAttrBean = managingFilters.getDataForProject(projectId, sprintId);

		velocityParams.put("report", this);
		velocityParams.put("action", action);

		velocityParams.put("total", managingFilters.getTotal(dAttrBean));
		velocityParams.put("stats", dAttrBean);
		velocityParams.put("bugs",managingFilters.getNonDevBugCategoriesData(projectId));
		velocityParams.put("totalDefectsCount",managingFilters.getBugTotal(managingFilters.getNonDevBugCategoriesData(projectId))+
				managingFilters.getTotal(dAttrBean).getDefects());
		log.info(dAttrBean);
		} else {		
        		velocityParams.put("dataPresent", true);
		}

		velocityParams.put("projectsList", projectHelper.getProjects());
		
		SprintHelper sprintHelper = new SprintHelper(searchService, loggedInUser, projectId);
        velocityParams.put("sprintFilter", sprintHelper.getSprintMap());
        velocityParams.put("sprint", sprintId);
		
		velocityParams.put("reportKey", reqParams.get("reportKey"));
		velocityParams.put("projectId", projectId);

		return velocityParams;
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
		new MinutesDurationFormatterExtended();
		return descriptor.getHtml("excel", getVelocityParams(action, reqParams));
	}

	@Override
	public boolean isExcelViewSupported() {
		return true;
	}

}
