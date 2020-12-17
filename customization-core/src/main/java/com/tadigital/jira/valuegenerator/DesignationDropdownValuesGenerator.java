package com.tadigital.jira.valuegenerator;

import java.util.Map;

import javax.inject.Inject;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.tadigital.jira.utility.JiraUtils;

public class DesignationDropdownValuesGenerator implements ValuesGenerator<String> {

	private final String PRACTICE_MANAGER_ROLE = "Practice Managers";


	@JiraImport
	private JiraAuthenticationContext authenticationContext;

	@JiraImport
	private ProjectManager projectManager;

	@JiraImport
	private GroupManager groupManager;

	@JiraImport
	private ProjectRoleManager projectRoleManager;

	@Inject
	public DesignationDropdownValuesGenerator(JiraAuthenticationContext authenticationContext,
			ProjectManager projectManager, GroupManager groupManager, ProjectRoleManager projectRoleManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
		this.projectRoleManager = projectRoleManager;
	}

	@Override
	public Map<String, String> getValues(Map userParams) {
		return new JiraUtils().getBandValues(authenticationContext, groupManager, PRACTICE_MANAGER_ROLE);
	}

	public JiraAuthenticationContext getAuthenticationContext() {
		return authenticationContext;
	}

	public void setAuthenticationContext(JiraAuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
	}


}