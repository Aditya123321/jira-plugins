package com.tadigital.jira.plugin.data;

import com.atlassian.jira.issue.status.SimpleStatus;

public class ProjectData {
	
	private String projectCategory;
	private long projectId;
	private String projectName;
	private String projectKey;
	private String projectLead;
	private int totalStories;
	private int storiesConsidered;
	private int numberOfExcludedSubstories;
	private SimpleStatus projectStatus;
	private int numberOfRelease;
	private EstimatesData estimatesData;
	
	public ProjectData() {
		
	}
	
	public ProjectData(String projectCategory, long projectId, String projectName, String projectKey, String projectLead) {
		this.projectCategory = projectCategory;
		this.projectId = projectId;
		this.projectName = projectName;
		this.projectKey = projectKey;
		this.projectLead = projectLead;
	}

	public String getProjectCategory() {
		return projectCategory;
	}

	public void setProjectCategory(String projectCategory) {
		this.projectCategory = projectCategory;
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getProjectLead() {
		return projectLead;
	}

	public void setProjectLead(String projectLead) {
		this.projectLead = projectLead;
	}

	public int getTotalStories() {
		return totalStories;
	}

	public void setTotalStories(int totalStories) {
		this.totalStories = totalStories;
	}

	public int getStoriesConsidered() {
		return storiesConsidered;
	}

	public void setStoriesConsidered(int storiesConsidered) {
		this.storiesConsidered = storiesConsidered;
	}

	public int getNumberOfExcludedSubstories() {
		return numberOfExcludedSubstories;
	}

	public void setNumberOfExcludedSubstories(int numberOfExcludedSubstories) {
		this.numberOfExcludedSubstories = numberOfExcludedSubstories;
	}

	public SimpleStatus getProjectStatus() {
		return projectStatus;
	}

	public void setProjectStatus(SimpleStatus projectStatus) {
		this.projectStatus = projectStatus;
	}

	public int getNumberOfRelease() {
		return numberOfRelease;
	}

	public void setNumberOfRelease(int numberOfRelease) {
		this.numberOfRelease = numberOfRelease;
	}

	public EstimatesData getEstimatesData() {
		return estimatesData;
	}

	public void setEstimatesData(EstimatesData estimatesData) {
		this.estimatesData = estimatesData;
	}
}
