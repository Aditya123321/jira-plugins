package com.tadigital.jira.plugin.helper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectCategory;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.utility.JiraUtils;

public class ProjectHelper {

	private JiraAuthenticationContext authenticationContext;
	private ProjectManager projectManager;
	private GroupManager groupManager;
	private ProjectRoleManager projectRoleManager;
	private Group deliveryHeadGroup;
	private Group projectManagerGroup;

	public ProjectHelper(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager, ProjectRoleManager projectRoleManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
		this.projectRoleManager = projectRoleManager;
		this.deliveryHeadGroup = this.groupManager.getGroup(ReportsConstant.PROJECT_MANAGER_GROUP_NAME);
		this.projectManagerGroup = this.groupManager.getGroup(ReportsConstant.DELIVERY_HEAD_GROUP_NAME);
	}

	public Map<Long, String> getProjects() {
		Map<Long, String> projectList = new LinkedHashMap<Long, String>();
		Collection<Project> projects = getFilteredProjects();
		if(projects.size() == 0) {
			projectList.put(Long.MIN_VALUE, ReportsConstant.NONE);
		}
		else {
			getFilteredProjects().forEach(project -> {
				ProjectCategory projectCategory = projectManager.getProjectCategoryForProject(project);
				if(projectCategory != null) {
					projectList.put(project.getId(), (project.getName() + " : " + projectCategory.getName()).trim());
				}
				else {
					projectList.put(project.getId(), project.getName().trim());
				}
			});
		}
		return new JiraUtils().getSortedValues(projectList);
	}

	public Collection<Project> getFilteredProjects() {
		Set<Project> result = new LinkedHashSet<>();
		ApplicationUser user = authenticationContext.getLoggedInUser();
		// Checking if user is a Delivery Head
		if (isUserInGroup(user, deliveryHeadGroup)) {
			projectManager.getProjects().forEach(project -> result.add(project));
		} else {
			projectManager.getProjects().forEach(project -> {
				if (isProjectManager(user, project)) {
					result.add(project);
				}
			});
		}
	return result;
	}
	
	public boolean isValidUser(final ApplicationUser user, final Project project) {
		return isUserInGroup(user, deliveryHeadGroup) || isProjectManager(user, project);
	}
	
	private boolean isProjectManager(final ApplicationUser user, final Project project) {
		if(isUserInGroup(user, projectManagerGroup)) {
			if (project.getProjectLead().equals(user)) {
				return true;
			}
			return hasProjectManagerRole(user, project);
		}
		return false;
	}
	
	private boolean hasProjectManagerRole(final ApplicationUser user, final Project project) {
		Collection<ProjectRole> projectRoles = projectRoleManager.getProjectRoles(user, project);
		return projectRoles.contains(projectRoleManager.getProjectRole(ReportsConstant.PROJECT_MANAGER_ROLE));
	}
	
	private boolean isUserInGroup(ApplicationUser user, Group group) {
		return groupManager.isUserInGroup(user, group);
	}
}
