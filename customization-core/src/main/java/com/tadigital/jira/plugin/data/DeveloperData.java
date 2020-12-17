package com.tadigital.jira.plugin.data;

public class DeveloperData {
	
	private long employeeId;
	private String name;
	private String jiraId;
	private String practice;
	private String designation;
	private int totalStories;
	private int storiesConsidered;
	private int numberOfExcludedSubstories;
	
	public DeveloperData() {
		
	}
	
	public DeveloperData(long employeeId, String name, String jiraId, String practice, String designation) {
		this.employeeId = employeeId;
		this.name = name;
		this.jiraId = jiraId;
		this.practice = practice;
		this.designation = designation;
	}
	
	public long getEmployeeId() {
		return employeeId;
	}
	
	public void setEmployeeId(long employeeId) {
		this.employeeId = employeeId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getJiraId() {
		return jiraId;
	}

	public void setJiraId(String jiraId) {
		this.jiraId = jiraId;
	}

	public String getPractice() {
		return practice;
	}

	public void setPractice(String practice) {
		this.practice = practice;
	}

	public String getDesignation() {
		return designation;
	}
	
	public void setDesignation(String designation) {
		this.designation = designation;
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
}
