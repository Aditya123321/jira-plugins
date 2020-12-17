package com.tadigital.jira.plugin.report.jira.deliveryreports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.reports.report.DeveloperAttributesBean;
import com.tadigital.jira.reports.report.DurationFormatterExtended;
import com.tadigital.jira.reports.report.DurationFormatterExtendedImpl;
import com.tadigital.jira.reports.report.ManagingFilters;
import com.tadigital.jira.reports.report.MinutesDurationFormatterExtended;
import com.tadigital.jira.utility.FilterValuesHelper;

import webwork.action.ActionContext;

@SuppressWarnings("rawtypes")
@Scanned
public class DefectDensityReportDeliveryHeads extends AbstractReport {

	@SuppressWarnings("unused")
	private DurationFormatterExtended durationFormatter;
	private final DurationFormatterExtended defaultDurationFormatter;

	DefectDensityReportDeliveryHeads(final DurationFormatterExtended durationFormatter) {
		this.defaultDurationFormatter = durationFormatter;
		this.durationFormatter = defaultDurationFormatter;
	}

	@Inject
	public DefectDensityReportDeliveryHeads() {
		this(new DurationFormatterExtendedImpl(new I18nBean(), ComponentAccessor.getJiraDurationUtils()));
	}

	public String generateReportHtml(ProjectActionSupport action, Map reqParams) throws Exception {
		return descriptor.getHtml("defectDensityDeliveryView", getVelocityParams(action, reqParams));
	}

	private Map<String, Object> getVelocityParams(ProjectActionSupport action, Map reqParams) throws SearchException, JSONException, IOException  {

		final Map<String, Object> velocityParams = new HashMap<>();
		ManagingFilters managingFilters;
		final String categoryId = (String) reqParams.get("categoryId");
		final Long catId = new Long(categoryId);
		final String categoryName = "categoryName";

		managingFilters = new ManagingFilters(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(),
				ComponentAccessor.getComponentOfType(SearchService.class), ComponentAccessor.getProjectManager());
		if (catId != Long.MIN_VALUE) {
			if (catId == Long.MAX_VALUE) {
				velocityParams.put(categoryName, "All Client Projects");
			} else if (catId != null) {
				if (ComponentAccessor.getProjectManager().getProjectCategory(catId).getName() != null) {
					velocityParams.put(categoryName,
							ComponentAccessor.getProjectManager().getProjectCategory(catId).getName());
				} else {
					velocityParams.put(categoryName, "Category is Not Defined");
				}
			}
			List<DeveloperAttributesBean> allDataBean = managingFilters.getDataforProjectGroup(catId);
			velocityParams.put("allClientsData", allDataBean);
			velocityParams.put("total", managingFilters.getTotal(allDataBean));
			velocityParams.put("bugCatList", managingFilters.getBugCategories());
			velocityParams.put("bugCountCatWise", managingFilters.getBugDataForGroup(allDataBean));
		} else {
			velocityParams.put("dataPresent",true);
		}

		

		FilterValuesHelper dropdownHelper = new FilterValuesHelper(ComponentAccessor.getJiraAuthenticationContext(),
				ComponentAccessor.getProjectManager(), ComponentAccessor.getGroupManager());
		List<String> groupList = new ArrayList<String>();
		groupList.add(ReportsConstant.DELIVERY_HEAD_GROUP_NAME);
		groupList.add(ReportsConstant.PRACTICE_MANAGER_ROLE);
		velocityParams.put("projectCategoryFilter", dropdownHelper.populateProjectCategories(groupList));
		velocityParams.put("reportKey", reqParams.get("reportKey"));
		velocityParams.put("projectCategory", catId);
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
		durationFormatter = new MinutesDurationFormatterExtended();
		return descriptor.getHtml("excel", getVelocityParams(action, reqParams));
	}

	@Override
	public boolean isExcelViewSupported() {
		return true;
	}
}
