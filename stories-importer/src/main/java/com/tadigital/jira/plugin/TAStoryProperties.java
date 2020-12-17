package com.tadigital.jira.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.csv.CSVRecord;

/**
 * Helper class to convert issues from csv values
 * 
 * @author Didhin.tv
 */
public class TAStoryProperties {
	
	private Properties properties = new Properties();
	private InputStream input = null;
	
	private String issueType;
	private String issueId;
	private String summary;
	private String priority;
	private  String assignee;
	private String description;
	
	private String analytics;
	private String aem;
	private String commerce;
	private String drupal;
	private String dataPerformance;
	private String enterprise;
	private String experience;	
	private String marketing;
	private String dotnet;
	private String salesforce;	
	private String uiMobility;
	


	public String getIssueType() {
		return issueType;
	}

	public String getIssueId() {
		return issueId;
	}

	public String getPriority() {
		return priority;
	}

	public String getAssignee() {
		return assignee;
	}

	public String getDescription() {
		return description;
	}

	public String getSummary() {
		return summary;
	}

	
	public TAStoryProperties(CSVRecord values) {
		try {
			input = getClass().getClassLoader().getResourceAsStream("stories-estimates-importer.properties");
			properties.load(input);
			
			issueType = values.get(properties.getProperty("issueType"));
			issueId = values.get(properties.getProperty("issueId"));
			summary = values.get(properties.getProperty("summary"));
			description = values.get(properties.getProperty("description"));
			priority = values.get(properties.getProperty("priority"));
			
			analytics = values.get(properties.getProperty("analytics"));
			aem = values.get(properties.getProperty("aem"));
			commerce = values.get(properties.getProperty("commerce"));
			drupal = values.get(properties.getProperty("drupal"));
			dataPerformance = values.get(properties.getProperty("data"));
			enterprise = values.get(properties.getProperty("enterprise"));
			experience = values.get(properties.getProperty("experience"));
			marketing = values.get(properties.getProperty("marketing"));
			dotnet = values.get(properties.getProperty("net"));
			salesforce = values.get(properties.getProperty("salesforce"));
			uiMobility = values.get(properties.getProperty("ui"));
			
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getAnalytics() {
		return analytics;
	}

	public String getAem() {
		return aem;
	}

	public String getCommerce() {
		return commerce;
	}

	public String getDrupal() {
		return drupal;
	}

	public String getDataPerformance() {
		return dataPerformance;
	}

	public String getEnterprise() {
		return enterprise;
	}

	public String getExperience() {
		return experience;
	}

	public String getMarketing() {
		return marketing;
	}

	public String getDotnet() {
		return dotnet;
	}

	public String getSalesforce() {
		return salesforce;
	}

	public String getUiMobility() {
		return uiMobility;
	}

	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}

	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAnalytics(String analytics) {
		this.analytics = analytics;
	}

	public void setAem(String aem) {
		this.aem = aem;
	}

	public void setCommerce(String commerce) {
		this.commerce = commerce;
	}

	public void setDrupal(String drupal) {
		this.drupal = drupal;
	}

	public void setDataPerformance(String dataPerformance) {
		this.dataPerformance = dataPerformance;
	}

	public void setEnterprise(String enterprise) {
		this.enterprise = enterprise;
	}

	public void setExperience(String experience) {
		this.experience = experience;
	}

	public void setMarketing(String marketing) {
		this.marketing = marketing;
	}

	public void setDotnet(String dotnet) {
		this.dotnet = dotnet;
	}

	public void setSalesforce(String salesforce) {
		this.salesforce = salesforce;
	}

	public void setUiMobility(String uiMobility) {
		this.uiMobility = uiMobility;
	}

	

}