package com.tadigital.jira.plugin.helper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.atlassian.greenhopper.service.sprint.Sprint;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.tadigital.jira.plugin.constants.ReportsConstant;

public class SprintHelper {
	
	private SearchService searchService;
	private ApplicationUser applicationUser;
	private long projectId;
	private CustomFieldManager customFieldManager;
	private CustomField sprintCustomField;
	
	public SprintHelper(SearchService searchService, ApplicationUser applicationUser, long projectId) {
		this.searchService = searchService;
		this.applicationUser = applicationUser;
		this.projectId = projectId;
		this.customFieldManager = ComponentAccessor.getCustomFieldManager();
		this.sprintCustomField = getSprintCustomFieldId(getCustomFieldManager().getCustomFieldObjects());
	}

	public Map<Long, String> getSprintMap() throws SearchException {
		Map<Long, String> map = new LinkedHashMap<>();
		map.put(Long.MIN_VALUE, ReportsConstant.NONE);
		getStoriesWithSprint(projectId).forEach(issue -> {
			List<Object> values = (List<Object>) sprintCustomField.getValue(issue);
			values.forEach(object -> {
					Sprint sprint = (Sprint) object;
					if(!map.containsKey(sprint.getId())) {
						map.put(sprint.getId(), sprint.getName());
					}
			});
		});
		return map;
	}
	
	private Collection<Issue> getStoriesWithSprint(final long projectId) throws SearchException {
		final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        final JqlClauseBuilder builder = queryBuilder.where().project(projectId);
        builder.and().issueType(ReportsConstant.STORY);
        builder.and().customField(sprintCustomField.getIdAsLong()).isNotEmpty();
        return searchService.search( applicationUser, queryBuilder.buildQuery(), PagerFilter.getUnlimitedFilter()).getResults();
	}
	
	private CustomField getSprintCustomFieldId(List<CustomField> customFieldObjects) {
		Predicate<CustomField> bySprintField = customField -> customField.getName().equalsIgnoreCase(ReportsConstant.SPRINT_CF);
		return customFieldObjects.stream().filter(bySprintField).findFirst().get();
	}
	
	protected CustomFieldManager getCustomFieldManager() {
		return customFieldManager;
	}

	public CustomField getSprintCustomField() {
		return sprintCustomField;
	}
}
