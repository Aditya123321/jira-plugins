package com.tadigital.jira.plugin.report.helper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.utility.JiraUtils;

public class ProjectCategoryHelper {

	private JiraAuthenticationContext authenticationContext;
	private ProjectManager projectManager;
	private GroupManager groupManager;
	private static final String PRACTICE_MANAGERS_GROUP_NAME = "Practice Managers";
	private Map<Long, String> result = new LinkedHashMap<>();

	public ProjectCategoryHelper(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
	}

	public Map<Long, String> populateProjectCategories() {
		ApplicationUser user = authenticationContext.getLoggedInUser();
		HashMap<Long, String> categories = new LinkedHashMap<Long, String>();
		categories.put(Long.MIN_VALUE, ReportsConstant.NONE);
		// Checking if user is a delivery head
		if (isUserInGroup(user, getGroup(ReportsConstant.DELIVERY_HEAD_GROUP_NAME))
				|| isUserInGroup(user, getGroup(PRACTICE_MANAGERS_GROUP_NAME))) {
			categories.put(Long.MAX_VALUE, "All Client Projects");
			for (ProjectCategory projectCategory : projectManager.getAllProjectCategories()) {
				populateProjectCategory(projectCategory);
			}
			categories.putAll(new JiraUtils().getSortedValues(result));
		}

		return categories;
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

			result.put(projectCategory.getId(), projectCategory.getName().trim());
		}

	}
}
