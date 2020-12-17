package com.tadigital.jira.plugin.report.practicemanager;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.reports.report.DeveloperAttributesBean;
import com.tadigital.jira.reports.report.ManagingFilters;
import com.tadigital.jira.utility.HRMSApiCallService;
import com.tadigital.jira.utility.JiraUtils;

import webwork.action.ActionContext;

@SuppressWarnings("rawtypes")
public class DefectDensityReportPracticeManager extends AbstractReport {

	private static final Logger log = Logger.getLogger(DefectDensityReportPracticeManager.class);

	public String generateReportHtml(ProjectActionSupport action, Map reqParams) throws Exception {
		return descriptor.getHtml("defectDensityPracticeView", getVelocityParams(action, reqParams));
	}

	private Map<String, Object> getVelocityParams(ProjectActionSupport action, Map reqParams) throws SearchException, IOException, Exception  {

		final Map<String, Object> velocityParams = new HashMap<>();
		ManagingFilters managingFilters = new ManagingFilters(
				ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(),
				ComponentAccessor.getComponentOfType(SearchService.class), ComponentAccessor.getProjectManager());

		String practice = (String) reqParams.get(ReportsConstant.PRACTICE);
		String band = (String) reqParams.get(ReportsConstant.BAND);
		String from = (String) reqParams.get(ReportsConstant.FROM);
		String to = (String) reqParams.get(ReportsConstant.TO);
		String practiceName = "";
		String bandName = "";

		HRMSApiCallService apiCallService = new HRMSApiCallService();
		JiraUtils jiraUtils = new JiraUtils();
		List<DeveloperAttributesBean> allDataBean;

		if (StringUtils.isEmpty(from)) {
			from = "1/Jan/20";
		}
		if (StringUtils.isEmpty(to)) {
			DateTimeFormatter df = DateTimeFormatter.ofPattern(ReportsConstant.DATE_FORMAT);
			to = df.format(LocalDateTime.now());
		}
		if (from.contains("-")) {
			from = jiraUtils.getFormattedDate(from, ReportsConstant.DATEPICKER_DATE_FORMAT,
					ReportsConstant.DATE_FORMAT);
		}
		if (to.contains("-")) {
			to = jiraUtils.getFormattedDate(to, ReportsConstant.DATEPICKER_DATE_FORMAT, ReportsConstant.DATE_FORMAT);
		}
		

		if (!practice.equalsIgnoreCase(ReportsConstant.NONE)) {
			allDataBean = managingFilters.getPracticeStatistics(apiCallService.getPracticeAPICall(practice,band),
						practice, from, to, band);			
			velocityParams.put("allData", allDataBean);
			velocityParams.put("total", managingFilters.getTotal(allDataBean));
			velocityParams.put("from",
					jiraUtils.getFormattedDate(from, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
			velocityParams.put("to",
					jiraUtils.getFormattedDate(to, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
			velocityParams.put("toNextDate",
					jiraUtils.getFormattedDate(jiraUtils.getNextDay(to), ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
			velocityParams.put("fromDate", jiraUtils.getFormattedDate(from, ReportsConstant.DATE_FORMAT,
					ReportsConstant.DATEPICKER_DATE_FORMAT));
			velocityParams.put("toDate", jiraUtils.getFormattedDate(to, ReportsConstant.DATE_FORMAT,
					ReportsConstant.DATEPICKER_DATE_FORMAT));
			velocityParams.put("practice", practice);
		} else {
			velocityParams.put("dataPresent", true);
		}

		velocityParams.put("practiceFilter",
				jiraUtils.getPracticeValues(ComponentAccessor.getJiraAuthenticationContext(),
						ComponentAccessor.getGroupManager(), ReportsConstant.PRACTICE_MANAGER_ROLE));
		velocityParams.put("bandFilter", jiraUtils.getBandValues(ComponentAccessor.getJiraAuthenticationContext(),
				ComponentAccessor.getGroupManager(), ReportsConstant.PRACTICE_MANAGER_ROLE));
		velocityParams.put("reportKey", reqParams.get("reportKey"));

		velocityParams.put("band", band);

		log.info(practice);
		log.info(band);
		log.info(from);
		log.info(to);

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
		return descriptor.getHtml("excel", getVelocityParams(action, reqParams));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validate(ProjectActionSupport action, Map params) {
		super.validate(action, params);
		String practice = (String) params.get(ReportsConstant.PRACTICE);
		if (StringUtils.isEmpty(practice)) {
			action.addError(ReportsConstant.PRACTICE, action.getText(""));
		}
	}

	@Override
	public boolean isExcelViewSupported() {
		return true;
	}
}
