package com.tadigital.jira.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.CreateValidationResult;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.context.IssueContext;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.plugins.importer.external.beans.ExternalIssue;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
/**
 * @author Aditya Sharma
 *
 */
@Scanned
public class IssueCRUD {	

	private ApplicationUser user;
	private CustomFieldManager customFieldManager;
	private CustomField customField;	
	private IssueManager issueManager;	
	private Long projectId;
	private String projectKey;
	private ProjectManager projectManager;	
	private Long trackTypeId;
	private Long planEstimatesId;	
	private OptionsManager optionsManager;
	private Options options;
	private FieldConfig fieldConfig;
	
	private static final Logger log = Logger.getLogger(IssueCRUD.class);
	private Properties properties = new Properties();
	private InputStream input = null;
	

	public IssueCRUD(ApplicationUser user, CustomFieldManager customFieldManager, 
			IssueManager issueManager,  Long projectId, String projectKey, OptionsManager optionsManager, ProjectManager projectManager) {		
		this.user = user;	
		this.customFieldManager = customFieldManager;
		this.issueManager = issueManager;
		this.projectId = projectId;
		this.projectKey = projectKey;
		this.projectManager = projectManager;
		this.optionsManager = optionsManager;
	}

	//GET EXISTING SUBSTORIES FOR A PARTICULAR STORY
	@SuppressWarnings("deprecation")
	public Collection<Issue> getSubStoriesFromProject(String csvStoryId) {
		try {
			input = getClass().getClassLoader().getResourceAsStream("stories-estimates-importer.properties");
			properties.load(input);
			for (CustomField cf : getCustomFieldManager().getCustomFieldObjects()) {
				if (cf.getName().equalsIgnoreCase(properties.getProperty("external.issue.id"))) {
					customField = getCustomFieldManager().getCustomFieldObject(cf.getIdAsLong());
				}
			}
			input.close();
		} catch (IOException e) {
			log.log(Priority.WARN, "Error From Getting Properties File",e);
		}

		Collection<Issue> existingSubStories = new ArrayList<Issue>();
		Collection<Long> issueIds;
		try {
			issueIds = this.getIssueManager().getIssueIdsForProject(getProjectId());
			for (Long issueId : issueIds) {
				MutableIssue issue = this.getIssueManager().getIssueObject(issueId);

				if (csvStoryId!=null && issue!=null && issue.getCustomFieldValue(customField)!=null && (issue.getCustomFieldValue(customField) == csvStoryId
						|| issue.getCustomFieldValue(customField).equals(csvStoryId))) {
					log.log(Priority.WARN, "Getting the Sub stories for "+ issue +" ::  "+issue.getSubTaskObjects());
					return issue.getSubTaskObjects();

				} else {
					return existingSubStories;
				}
			}
		} catch (GenericEntityException e) {
			log.log(Priority.WARN, "Error in Getting issues method", e);
		}
		
		return existingSubStories;
	}
	
	//UPDATING THE TRACK ESTIMATES IF THE SUB STORY ALREADY EXISTS
	@SuppressWarnings("deprecation")
	public void updateExistingSubStories(Collection<Issue> existingSubStories, Map<String, String> trackValues, 
			ExternalIssue externalIssue, TAStoryProperties from) {

		// Initializing values
		Issue parentStory = existingSubStories.iterator().next().getParentObject();
		String priority = from.getPriority();
		String issueTypeId = "Sub-Story";
		

		this.setCustomFieldIds();

		CustomField trackType = this.getCustomFieldManager().getCustomFieldObject(getTrackTypeId());
		CustomField planEstimates = this.getCustomFieldManager().getCustomFieldObject(getPlanEstimatesId());

		// setting the iterators
		Iterator<Issue> issueIterator = existingSubStories.iterator();
		Iterator<Map.Entry<String, String>> itr = trackValues.entrySet().iterator();

		// defining the modified value object
		ModifiedValue<Object> mVal;
		Map<String, String> addNewTrackValues = trackValues;
		List<String> keys = new ArrayList<>();

		for (Issue subStory : existingSubStories) {
			log.log(Priority.WARN,"Sub-Story is ::" + subStory);
			for (Map.Entry<String, String> track : trackValues.entrySet()) {
				log.log(Priority.WARN,"Track is :: " + track);
				if (subStory!=null && track!=null && trackType!=null && subStory.getCustomFieldValue(trackType)!=null && subStory.getCustomFieldValue(trackType).toString().equals(track.getKey())) {
					if (!(planEstimates!=null && subStory.getCustomFieldValue(planEstimates)!=null && subStory.getCustomFieldValue(planEstimates).toString().equals(track.getValue()))) {

						log.log(Priority.WARN,"UPDATE EXISTING STORIES :: IN 2nd IF BLOCK :: " + subStory);
						log.log(Priority.WARN,"Comparing in 2nd if block " + subStory + " ::  and :: " + track);
						mVal = new ModifiedValue<Object>(subStory.getCustomFieldValue(planEstimates), track.getValue());

						// UPDATING THE PLAN ESTIMATES WITH THE NEW VALUE
						log.log(Priority.WARN,"\n Modified value :: " + mVal);
						planEstimates.updateValue(null, subStory, mVal, new DefaultIssueChangeHolder());
						keys.add(track.getKey());
						break;
					} else if (planEstimates!=null && subStory.getCustomFieldValue(planEstimates)!=null && subStory.getCustomFieldValue(planEstimates).toString().equals(track.getValue())) {

						log.log(Priority.WARN,"UPDATE EXISTING STORIES :: IN ELSE IF BLOCK :: " + subStory);
						log.log(Priority.WARN,"Comparing in else If block " + subStory + " ::  and :: " + track);
						keys.add(track.getKey());
						break;
					}
				}

			}
			parentStory = subStory.getParentObject();
			priority = subStory.getPriorityObject().getId();
			issueTypeId = subStory.getIssueTypeObject().getId();
		}

		for (String key : keys) {
			addNewTrackValues.remove(key);
		}
		log.log(Priority.WARN,"Add new tracks" + addNewTrackValues);
		// Adding new tracks to the existing Story
		if (addNewTrackValues.isEmpty()) {
			
		} else {
			System.out.println("in last else block");
			this.addNewSubStories(from, addNewTrackValues, parentStory, priority, issueTypeId);
		}
	}
	
	
	//ADDING NEW SUB STORIES TO AN EXISTING ISSUE WITH SUB STORIES
	public void addNewSubStories( TAStoryProperties from, Map<String, String> addNewTrackValues, 
			Issue parent, String priority, String issueTypeId) {

		CreateValidationResult validationResult;
		this.setCustomFieldIds();

		CustomField trackType = this.getCustomFieldManager().getCustomFieldObject(getTrackTypeId());

		Iterator<Map.Entry<String, String>> itr = addNewTrackValues.entrySet().iterator();
		Map.Entry<String, String> track;

		IssueContext issueContext = new IssueContextImpl(getProjectId(), issueTypeId);
		fieldConfig = trackType.getRelevantConfig(issueContext);
		options = optionsManager.getOptions(fieldConfig);

		Iterator<Option> iterOption = options.iterator();
		Option opt;
		String optId = "";

		while (itr.hasNext()) {
			track = itr.next();
			IssueInputParameters issueInputParameters = this.getIssueService().newIssueInputParameters();
			issueInputParameters.setProjectId(getProjectId());
			issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getName());
			issueInputParameters.setPriorityId(priority);
			issueInputParameters.setSummary(track.getKey()+" Development");
			issueInputParameters.setReporterId(parent.getReporterId());
			while (iterOption.hasNext()) {
				opt = iterOption.next();
				if (opt.getValue().equals(track.getKey())) {
					optId = opt.getOptionId().toString();
				}
			}
			issueInputParameters.addCustomFieldValue(getTrackTypeId(), optId);
			issueInputParameters.addCustomFieldValue(getPlanEstimatesId(), track.getValue());
			issueInputParameters.setIssueTypeId(issueTypeId);

			validationResult = this.getIssueService().validateSubTaskCreate(getUser(), parent.getId(),
					issueInputParameters);

			log.log(Level.WARN, "Validation Warnings " + validationResult.getWarningCollection());
			log.log(Level.WARN, "Validation Warnings " + validationResult.getErrorCollection());

			if (validationResult.isValid()) {
				IssueResult createResult = this.getIssueService().create(getUser(), validationResult);

				if (createResult.isValid()) {
					Issue subIssue = createResult.getIssue();

					try {
						getSubTaskManager().createSubTaskIssueLink(parent, subIssue, getUser());

					} catch (CreateException e) {

						log.log(Level.WARN, "Error in Creating Sub Task", e);
					}
				} else {
					log.log(Level.WARN, "Error in create result");
				}
			} else {
				log.log(Level.WARN, "Error in validate result");
			}
		}
	}
	
	public void setCustomFieldIds() {
		try {
			input = getClass().getClassLoader().getResourceAsStream("stories-estimates-importer.properties");
			properties.load(input);
			for(CustomField cf : getCustomFieldManager().getCustomFieldObjects()) {
				if(cf.getName().equalsIgnoreCase(properties.getProperty("track.type"))) {				
					setTrackTypeId(cf.getIdAsLong());
				} else if (cf.getName().equalsIgnoreCase(properties.getProperty("architect.estimates"))) {
					setPlanEstimatesId(cf.getIdAsLong());
				}
			}
			input.close();
		} catch (IOException e) {	
			log.log(Level.WARN,"Error in Getting Properties file",e);
		}
	}

	public ApplicationUser getUser() {
		return user;
	}
	public CustomFieldManager getCustomFieldManager() {
		return customFieldManager;
	}
	
	public CustomField getCustomField() {
		return customField;
	}
	
	public ConstantsManager getConstantsManager() {
		return ComponentAccessor.getConstantsManager();
	}

	public IssueManager getIssueManager() {
		return issueManager;
	}
	
	public IssueService getIssueService() {
		return ComponentAccessor.getIssueService();
	}

	public Long getProjectId() {
		return projectId;
	}
	
	public String getProjectKey() {
		return projectKey;
	}

	public ProjectManager getProjectManager() {
		return projectManager;
	}
	
	public OptionsManager getOptionsManager() {
		return optionsManager;
	}
	
	public SubTaskManager getSubTaskManager() {
		return ComponentAccessor.getSubTaskManager();
	}
	
	public Long getTrackTypeId() {
		return trackTypeId;
	}

	public void setTrackTypeId(Long trackTypeId) {
		this.trackTypeId = trackTypeId;
	}

	public Long getPlanEstimatesId() {
		return planEstimatesId;
	}

	public void setPlanEstimatesId(Long planEstimatesId) {
		this.planEstimatesId = planEstimatesId;
	}
}
