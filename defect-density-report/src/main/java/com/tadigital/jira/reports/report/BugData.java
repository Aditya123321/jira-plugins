package com.tadigital.jira.reports.report;

public class BugData {
	private String bugCategory;
	private long bugCount;
	private int bugsConsidered;
	
	public BugData(String bugCategory, long bugCount) {
		super();
		this.bugCategory = bugCategory;
		this.bugCount = bugCount;		
	}

	public String getBugCategory() {
		return bugCategory;
	}

	public long getBugCount() {
		return bugCount;
	}

	public int getBugsConsidered() {
		return bugsConsidered;
	}

	public void setBugCategory(String bugCategory) {
		this.bugCategory = bugCategory;
	}

	public void setBugCount(long bugCount) {
		this.bugCount = bugCount;
	}

	public void setBugsConsidered(int bugsConsidered) {
		this.bugsConsidered = bugsConsidered;
	}
	
	
}
