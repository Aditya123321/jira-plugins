package com.tadigital.jira.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.importer.external.CustomFieldConstants;
import com.atlassian.jira.plugins.importer.external.beans.ExternalComponent;
import com.atlassian.jira.plugins.importer.external.beans.ExternalCustomFieldValue;
import com.atlassian.jira.plugins.importer.external.beans.ExternalIssue;
import com.atlassian.jira.plugins.importer.external.beans.ExternalLink;
import com.atlassian.jira.plugins.importer.external.beans.ExternalProject;
import com.atlassian.jira.plugins.importer.external.beans.ExternalUser;
import com.atlassian.jira.plugins.importer.external.beans.ExternalVersion;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingHelper;
import com.atlassian.jira.plugins.importer.imports.importer.AbstractDataBean;
import com.atlassian.jira.plugins.importer.imports.importer.ImportLogger;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TACsvDataBean extends AbstractDataBean<TACsvConfigBean> {
	private final TACsvClient csvClient;
	private final TACsvConfigBean configBean;
	private final ValueMappingHelper valueMappingHelper;
	private Properties properties = new Properties();
	private InputStream input = null;

	private IssueCRUD issueCRUD;
	private static final Logger log = Logger.getLogger(TACsvDataBean.class);

	public TACsvDataBean(TACsvConfigBean configBean) {
		super(configBean);
		this.configBean = configBean;
		this.csvClient = configBean.getCsvClient();
		this.valueMappingHelper = configBean.getValueMappingHelper();
	}

	@Override
	public Set<ExternalUser> getRequiredUsers(Collection<ExternalProject> projects, ImportLogger importLogger) {
		return getAllUsers(importLogger);
	}

	@Override
	public Set<ExternalUser> getAllUsers(ImportLogger log) {
		return Sets.newHashSet(
				Iterables.transform(csvClient.getInternalIssues(), new Function<TAStoryProperties, ExternalUser>() {
					@Override
					public ExternalUser apply(TAStoryProperties from) {
						return new ExternalUser(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getName(), ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getName());
					}
				}));
	}

	@Override
	public Set<ExternalProject> getAllProjects(ImportLogger log) {

		final ExternalProject project = new ExternalProject(configBean.getProjectName("project"),
				configBean.getProjectKey("project"));
		project.setExternalName("project");
		return Sets.newHashSet(project);
	}

	@Override
	public Iterator<ExternalIssue> getIssuesIterator(ExternalProject externalProject, ImportLogger importLogger) {
		return Iterables.transform(csvClient.getInternalIssues(), new Function<TAStoryProperties, ExternalIssue>() {
			@Override
			public ExternalIssue apply(TAStoryProperties from) {
				final ExternalIssue externalIssue = new ExternalIssue();
				try {
					input = getClass().getClassLoader().getResourceAsStream("stories-estimates-importer.properties");
					properties.load(input);
					externalIssue.setPriority(from.getPriority());
					if(!from.getIssueId().equals("") || from.getIssueId()!=null) {
						externalIssue.setExternalId(from.getIssueId());
					}
					
					externalIssue.setSummary(from.getSummary());
					externalIssue.setAssignee(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getName());
					externalIssue.setDescription(from.getDescription());
					externalIssue.setIssueType(from.getIssueType());

					// creating list for track estimates
					Map<String, String> trackValues = new HashMap<String, String>();
					
					trackValues.put(properties.getProperty("analytics"), from.getAnalytics());
					trackValues.put(properties.getProperty("aem"), from.getAem());
					trackValues.put(properties.getProperty("commerce"), from.getCommerce());
					trackValues.put(properties.getProperty("data"), from.getDataPerformance());
					trackValues.put(properties.getProperty("drupal"), from.getDrupal());
					trackValues.put(properties.getProperty("enterprise"), from.getEnterprise());
					trackValues.put(properties.getProperty("experience"), from.getExperience());
					trackValues.put(properties.getProperty("marketing"), from.getMarketing());
					trackValues.put(properties.getProperty("net"), from.getDotnet());
					trackValues.put(properties.getProperty("salesforce"), from.getSalesforce());
					trackValues.put(properties.getProperty("ui"), from.getUiMobility());
					issueCRUD = new IssueCRUD(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(),
							ComponentAccessor.getCustomFieldManager(), ComponentAccessor.getIssueManager(),
							ComponentAccessor.getProjectManager().getProjectByCurrentKey(externalProject.getKey()).getId(),
							externalProject.getKey(),
							ComponentAccessor.getOptionsManager(),
							ComponentAccessor.getProjectManager());

					Collection<Issue> existingSubStories;
					log.log(Level.WARN, "Track Values From CSV"+trackValues);
					existingSubStories = issueCRUD.getSubStoriesFromProject(from.getIssueId());

					if (existingSubStories.isEmpty()) {

						externalIssue.setSubtasks(createSubStories(from.getPriority(), trackValues));

					} else if (!(existingSubStories.isEmpty())) {
						log.log(Level.WARN, "EXISTING SUBSTORIES RETURNED"+existingSubStories);
						issueCRUD.updateExistingSubStories(existingSubStories, trackValues, externalIssue, from);				
					}
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return externalIssue;
			}
		}).iterator();
	}

	@Override
	public Collection<ExternalLink> getLinks(ImportLogger log) {
		final List<ExternalLink> externalLinks = Lists.newArrayList();
		@SuppressWarnings("unused")
		final String linkName = configBean.getLinkMapping("link");
		/*
		 * for (Issue issue : csvClient.getInternalIssues()) { }
		 */
		return externalLinks;
	}

	@Override
	public long getTotalIssues(Set<ExternalProject> selectedProjects, ImportLogger log) {
		return csvClient.getInternalIssues().size();
	}

	@Override
	public String getUnusedUsersGroup() {
		return "simple_csv_import_unused";
	}

	@Override
	public void cleanUp() {
	}

	@Override
	public String getIssueKeyRegex() {
		return null;
	}

	@Override
	public Collection<ExternalVersion> getVersions(ExternalProject externalProject, ImportLogger importLogger) {
		return Collections.emptyList();
	}

	@Override
	public Collection<ExternalComponent> getComponents(ExternalProject externalProject, ImportLogger importLogger) {
		return Collections.emptyList();
	}

	public Collection<ExternalIssue> createSubStories(String priority, Map<String, String> trackValues) {
		Collection<ExternalIssue> allTrackLevelIssues = new ArrayList<>();
		ExternalIssue issue;
		Map.Entry<String, String> track;
		try {
			input = getClass().getClassLoader().getResourceAsStream("stories-estimates-importer.properties");
			properties.load(input);
			Iterator<Map.Entry<String, String>> itr = trackValues.entrySet().iterator();
			while (itr.hasNext()) {
				track = itr.next();
				if (track.getValue() != "0" && track.getValue() != "" && track.getValue() != null
						&& !track.getValue().equals("0") && !track.getValue().equals("0") &&
						!track.getValue().equals("") && !(track.getValue().equals(""))) {
					issue = new ExternalIssue();
					issue.setPriority(priority);				
					issue.setSummary(track.getKey()+" Development");
					issue.setIssueType("Sub-Story");
					issue.setExternalCustomFieldValues(Lists.newArrayList(
							new ExternalCustomFieldValue(properties.getProperty("architect.estimates"), CustomFieldConstants.NUMBER_FIELD_TYPE,
									CustomFieldConstants.TEXT_FIELD_SEARCHER, track.getValue()),
							new ExternalCustomFieldValue(properties.getProperty("track.type"), CustomFieldConstants.SELECT_FIELD_TYPE,
									CustomFieldConstants.SELECT_FIELD_SEARCHER, track.getKey())));
					allTrackLevelIssues.add(issue);
				}
			}
			input.close();
		} catch (IOException e) {		
			e.printStackTrace();
		}
		return allTrackLevelIssues;
	}

}
