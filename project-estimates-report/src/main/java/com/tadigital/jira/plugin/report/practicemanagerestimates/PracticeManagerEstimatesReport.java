package com.tadigital.jira.plugin.report.practicemanagerestimates;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.plugin.report.ReportSubTaskFetcher;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.plugin.report.data.PracticeLevelReportData;
import com.tadigital.jira.plugin.report.helper.EstimatesReportHelper;
import com.tadigital.jira.utility.JiraUtils;

import webwork.action.ActionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;

public class PracticeManagerEstimatesReport extends AbstractReport {
	
	public String generateReportHtml(ProjectActionSupport projectActionSupport, Map map) throws Exception {
		return descriptor.getHtml("view", getVelocityParams(projectActionSupport, map));
	}
	
	// Generate an EXCEL view of report
	@Override
	public String generateReportExcel(final ProjectActionSupport action, final Map reqParams) throws Exception {
		final StringBuilder contentDispositionValue = new StringBuilder(50);
		contentDispositionValue.append("attachment;filename=\"").append(getDescriptor().getName());
		String practiceName = (String) reqParams.get(ReportsConstant.PRACTICE);
		if(!practiceName.equals("None")) {
			contentDispositionValue.append("-").append(practiceName);
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

	private Map<String, ?> getVelocityParams(ProjectActionSupport projectActionSupport, Map reqParams) throws Exception {
		
		JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
		GroupManager groupManager = ComponentAccessor.getGroupManager(); 
		
		final Map<String, Object> velocityParams = new HashMap<>();

		String practiceName = (String) reqParams.get(ReportsConstant.PRACTICE);
		String band = (String) reqParams.get(ReportsConstant.BAND);
		String fromDate = (String) reqParams.get(ReportsConstant.FROM);
		String toDate = (String) reqParams.get(ReportsConstant.TO);
		
		if (StringUtils.isEmpty(fromDate)) {
			fromDate = "1/Jan/20";
        }
        if (StringUtils.isEmpty(toDate)) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(ReportsConstant.DATE_FORMAT);
            toDate = df.format(LocalDateTime.now());
        }

		JiraUtils jiraUtils = new JiraUtils();
		Map<String, String> practiceFilter = jiraUtils.getPracticeValues(authenticationContext, groupManager, ReportsConstant.PRACTICE_MANAGER_ROLE); 
		velocityParams.put("practiceFilter", practiceFilter);
		velocityParams.put("bandFilter", jiraUtils.getBandValues(authenticationContext, groupManager, ReportsConstant.PRACTICE_MANAGER_ROLE));
		velocityParams.put("reportKey", reqParams.get("reportKey"));
		
		if(practiceName.equals("None")) {
			velocityParams.put("none", true);
		}
		else {
			EstimatesReportHelper estimatesReportHelper = new EstimatesReportHelper(ComponentAccessor.getProjectManager(),
					ComponentAccessor.getComponentOfType(SearchService.class),
					ComponentAccessor.getComponentOfType(ReportSubTaskFetcher.class),
					ComponentAccessor.getComponentOfType(IssueTypeManager.class), 
					ComponentAccessor.getWorkflowManager(),
					ComponentAccessor.getCustomFieldManager(),
					ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
			if(practiceName.equals(ReportsConstant.ALL)) {
				Map<String, String> filteredValues = practiceFilter.entrySet()
						.stream()
						.filter(map -> !(ReportsConstant.NONE.equals(map.getKey()) || ReportsConstant.ALL.equals(map.getKey())))
						.collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
				List<PracticeLevelReportData> practiceLevelReportDatas = estimatesReportHelper.getPracticeLevelReportDateForAll(filteredValues, band, fromDate, toDate);
				if(fromDate.contains("/") || toDate.contains("/")) {
					velocityParams.put("jqlFrom", jiraUtils.getFormattedDate(fromDate, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("jqlTo", jiraUtils.getFormattedDate(toDate, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("fromDate", jiraUtils.getFormattedDate(fromDate, ReportsConstant.DATE_FORMAT, ReportsConstant.DATEPICKER_DATE_FORMAT));
					velocityParams.put("toDate", jiraUtils.getFormattedDate(toDate, ReportsConstant.DATE_FORMAT, ReportsConstant.DATEPICKER_DATE_FORMAT));
				}
				else {
					velocityParams.put("jqlFrom", jiraUtils.getFormattedDate(fromDate,
							ReportsConstant.DATEPICKER_DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("jqlTo", jiraUtils.getFormattedDate(toDate,
							ReportsConstant.DATEPICKER_DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("fromDate", fromDate);
					velocityParams.put("toDate", toDate);
				}
				velocityParams.put("totalEstimatesData", estimatesReportHelper.computeTotalEstimates(practiceLevelReportDatas));
				Collections.sort(practiceLevelReportDatas, (dev1, dev2) -> dev1.getDeveloperData().getName().compareTo(dev2.getDeveloperData().getName()));
				velocityParams.put("developersData", practiceLevelReportDatas);
			}
			else {
				List<PracticeLevelReportData> practiceLevelReportDatas = null;
				if(fromDate.contains("/") || toDate.contains("/")) {
					practiceLevelReportDatas = estimatesReportHelper
							.getPracticecLevelReportData(practiceName, band, fromDate, toDate);
					
					velocityParams.put("jqlFrom", jiraUtils.getFormattedDate(fromDate, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("jqlTo", jiraUtils.getFormattedDate(toDate, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("fromDate", jiraUtils.getFormattedDate(fromDate, ReportsConstant.DATE_FORMAT, ReportsConstant.DATEPICKER_DATE_FORMAT));
					velocityParams.put("toDate", jiraUtils.getFormattedDate(toDate, ReportsConstant.DATE_FORMAT, ReportsConstant.DATEPICKER_DATE_FORMAT));
				}
				else {
					practiceLevelReportDatas = estimatesReportHelper
							.getPracticecLevelReportData(practiceName, band,
									jiraUtils.getFormattedDate(fromDate, ReportsConstant.DATEPICKER_DATE_FORMAT,
											ReportsConstant.DATE_FORMAT),
									jiraUtils.getFormattedDate(toDate, ReportsConstant.DATEPICKER_DATE_FORMAT,
											ReportsConstant.DATE_FORMAT));
					velocityParams.put("jqlFrom", jiraUtils.getFormattedDate(fromDate,
							ReportsConstant.DATEPICKER_DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("jqlTo", jiraUtils.getFormattedDate(toDate,
							ReportsConstant.DATEPICKER_DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
					velocityParams.put("fromDate", fromDate);
					velocityParams.put("toDate", toDate);
				}
				velocityParams.put("totalEstimatesData", estimatesReportHelper.computeTotalEstimates(practiceLevelReportDatas));
				Collections.sort(practiceLevelReportDatas, (dev1, dev2) -> dev1.getDeveloperData().getName().compareTo(dev2.getDeveloperData().getName()));
				velocityParams.put("developersData", practiceLevelReportDatas);
			}
			
			velocityParams.put("practiceName", practiceName);
			velocityParams.put("band", band);
			velocityParams.put("subStory", ReportsConstant.SUB_STORY);
			velocityParams.put("closed", ReportsConstant.STATUS_CLOSED);
			velocityParams.put("developer", ReportsConstant.DEVELOPER_CF);
			velocityParams.put("jiraBaseUrl", ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL));
		}
		return velocityParams;
	}
}
