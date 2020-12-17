package com.tadigital.jira.valuegenerator;

import java.util.Map;

import javax.inject.Inject;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.tadigital.jira.plugin.helper.ProjectHelper;

public class ProjectValuesGenerator implements ValuesGenerator<Long> {

	@JiraImport
	private JiraAuthenticationContext authenticationContext;

	@JiraImport
	private ProjectManager projectManager;

	@JiraImport
	private GroupManager groupManager;

	@JiraImport
	private ProjectRoleManager projectRoleManager;

	@Inject
	public ProjectValuesGenerator(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager, ProjectRoleManager projectRoleManager) {
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
		this.projectRoleManager = projectRoleManager;
	}

	@Override
	public Map<Long, String> getValues(Map var1) {
		ProjectHelper projectHelper = new ProjectHelper(authenticationContext, projectManager, groupManager, projectRoleManager);
		return projectHelper.getProjects();
	}
}
