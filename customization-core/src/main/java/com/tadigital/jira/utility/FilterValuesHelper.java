package com.tadigital.jira.utility;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.project.ProjectManager;
import com.tadigital.jira.plugin.constants.ReportsConstant;

public class FilterValuesHelper {

	private JiraAuthenticationContext authenticationContext;
	private ProjectManager projectManager;
	private GroupManager groupManager;
	private ProjectRoleManager projectRoleManager;
	private Map<Long, String> result = new LinkedHashMap<>();
	private static final Logger log = Logger.getLogger(FilterValuesHelper.class);

	public FilterValuesHelper(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
	}

	public FilterValuesHelper(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager, ProjectRoleManager projectRoleManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
		this.projectRoleManager = projectRoleManager;
	}

	public Map<Long, String> populateProjectCategories(List<String> groupList) {
		ApplicationUser user = authenticationContext.getLoggedInUser();
		HashMap<Long, String> categories = new LinkedHashMap<Long, String>();
		if (isValidUser(groupList, user)) {
			categories.put(Long.MIN_VALUE, ReportsConstant.NONE);
			categories.put(Long.MAX_VALUE, "All Client Projects");
			for (ProjectCategory projectCategory : projectManager.getAllProjectCategories()) {
				populateProjectCategory(projectCategory);
			}
			categories.putAll(new JiraUtils().getSortedValues(result));
		} else {
			if (categories.size() == 0) {
				categories.put(Long.MIN_VALUE, ReportsConstant.NONE);
			}
		}
		return categories;
	}

	private boolean isValidUser(List<String> groupList, ApplicationUser user) {
		// Checking if user is part of the group
		for (String groupName : groupList) {
			Group group = getGroup(groupName);
			if (isUserInGroup(user, group)) {
				return true;
			}
		}
		return false;
	}

	private boolean isUserInGroup(ApplicationUser user, Group group) {
		return groupManager.isUserInGroup(user, group);
	}

	private Group getGroup(final String name) {
		return groupManager.getGroup(name);
	}

	private void populateProjectCategory(final ProjectCategory projectCategory) {
		if (projectCategory != null && projectCategory.getName() != null
				&& !projectCategory.getName().contains("Client.")) {

			result.put(projectCategory.getId(), projectCategory.getName());
		}

	}
}
