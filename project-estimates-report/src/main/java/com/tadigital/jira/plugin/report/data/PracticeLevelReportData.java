package com.tadigital.jira.plugin.report.data;

import com.tadigital.jira.plugin.data.DeveloperData;
import com.tadigital.jira.plugin.data.EstimatesData;

public class PracticeLevelReportData {
	
	private DeveloperData developerData;
	private EstimatesData estimatesData;
	
	public PracticeLevelReportData(final DeveloperData developerData, final EstimatesData estimatesData) {
		this.developerData = developerData;
		this.estimatesData = estimatesData;
	}
	
	public DeveloperData getDeveloperData() {
		return developerData;
	}
	
	public void setDeveloperData(DeveloperData developerData) {
		this.developerData = developerData;
	}
	
	public EstimatesData getEstimatesData() {
		return estimatesData;
	}
	
	public void setEstimatesData(EstimatesData estimatesData) {
		this.estimatesData = estimatesData;
	}
}
