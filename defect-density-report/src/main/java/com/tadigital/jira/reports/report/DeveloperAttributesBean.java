package com.tadigital.jira.reports.report;

import java.util.List;

import com.atlassian.jira.issue.status.SimpleStatus;

public class DeveloperAttributesBean {

	private String projectCategory;
	private String project;
	private String projectLead;
	private String developerName;
	private String jiraUserName;
	private SimpleStatus projectStatus;
	private int numberOfRelease;

	private String developerId;
	private String designation;
	private String practice;
	private String band;

	private Long defects;
	private Long projectId;
	private Long totalStories;
	private Long totalSubStoriesConsidered;
	private Long originalEstimatesTotal;
	private long actualHours;

	private double architectEstimates;
	private double defectDensityArchitect;
	private double defectDensity;
	private double defectDensityActual;
	
	private List<BugData> bugDataList;
	private long nonDevBugCount;
	private long allDefects;

	
	public DeveloperAttributesBean(Long totalStories, Long totalSubStoriesConsidered, double architectEstimates, Long originalEstimatesTotal, Long actualHours,
			Long defects, double defectDensityArchitect, double defectDensity, double defectDensityActual ) {
		this.totalStories = totalStories;
		this.totalSubStoriesConsidered =  totalSubStoriesConsidered;
		this.architectEstimates = architectEstimates;
		this.originalEstimatesTotal = originalEstimatesTotal;
		this.actualHours = actualHours;
		this.defects = defects;
		this.defectDensityArchitect = defectDensityArchitect;
		this.defectDensity = defectDensity;
		this.defectDensityActual =  defectDensityActual;
	}

	public String getDeveloperName() {
		return developerName;
	}

	public Long getDefects() {
		return defects;
	}

	public Long getTotalStories() {
		return totalStories;
	}

	public Long getOriginalEstimatesTotal() {
		return originalEstimatesTotal;
	}

	public double getDefectDensity() {
		return defectDensity;
	}

	public void setDeveloperName(String developerName) {
		this.developerName = developerName;
	}

	public void setDefects(Long defects) {
		this.defects = defects;
	}

	public void setTotalStories(Long totalStories) {
		this.totalStories = totalStories;
	}

	public void setOriginalEstimatesTotal(Long originalEstimatesTotal) {
		this.originalEstimatesTotal = originalEstimatesTotal;
	}

	public void setDefectDensity(double defectDensity) {
		this.defectDensity = defectDensity;
	}

	public double getArchitectEstimates() {
		return architectEstimates;
	}

	public double getDefectDensityArchitect() {
		return defectDensityArchitect;
	}

	public void setArchitectEstimates(double architectEstimates) {
		this.architectEstimates = architectEstimates;
	}

	public void setDefectDensityArchitect(double defectDensityArchitect) {
		this.defectDensityArchitect = defectDensityArchitect;
	}

	public String getProjectCategory() {
		return projectCategory;
	}

	public String getProject() {
		return project;
	}

	public String getProjectLead() {
		return projectLead;
	}

	public void setProjectCategory(String projectCategory) {
		this.projectCategory = projectCategory;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public void setProjectLead(String projectLead) {
		this.projectLead = projectLead;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getDeveloperId() {
		return developerId;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDeveloperId(String developerId) {
		this.developerId = developerId;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getPractice() {
		return practice;
	}

	public void setPractice(String practice) {
		this.practice = practice;
	}

	public String getJiraUserName() {
		return jiraUserName;
	}

	public void setJiraUserName(String jiraUserName) {
		this.jiraUserName = jiraUserName;
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

	public String getBand() {
		return band;
	}

	public void setBand(String band) {
		this.band = band;
	}

	public long getActualHours() {
		return actualHours;
	}

	public double getdefectDensityActual() {
		return defectDensityActual;
	}

	public void setActualHours(long actualHours) {
		this.actualHours = actualHours;
	}

	public void setdefectDensityActual(double defectDensityActual) {
		this.defectDensityActual = defectDensityActual;
	}

	public Long getTotalSubStoriesConsidered() {
		return totalSubStoriesConsidered;
	}

	public void setTotalSubStoriesConsidered(Long totalSubStoriesConsidered) {
		this.totalSubStoriesConsidered = totalSubStoriesConsidered;
	}

	public List<BugData> getBugDataList() {
		return bugDataList;
	}

	public void setBugDataList(List<BugData> bugDataList) {
		this.bugDataList = bugDataList;
	}

	public long getNonDevBugCount() {
		return nonDevBugCount;
	}

	public void setNonDevBugCount(long nonDevBugCount) {
		this.nonDevBugCount = nonDevBugCount;
	}

	public long getAllDefects() {
		return allDefects;
	}

	public void setAllDefects(long allDefects) {
		this.allDefects = allDefects;
	}

}
