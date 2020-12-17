package com.tadigital.jira.plugin.report.jira.deliveryreports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.utility.FilterValuesHelper;

@SuppressWarnings("rawtypes")
public class ProjectDropdownValuesGenerator implements ValuesGenerator {

	@JiraImport
	private JiraAuthenticationContext authenticationContext;

	@JiraImport
	private ProjectManager projectManager;

	@JiraImport
	private GroupManager groupManager;

	@JiraImport
	private ProjectRoleManager projectRoleManager;

	@Inject
	public ProjectDropdownValuesGenerator(JiraAuthenticationContext authenticationContext,
			ProjectManager projectManager, GroupManager groupManager, ProjectRoleManager projectRoleManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
		this.projectRoleManager = projectRoleManager;

	}

	@Override
	public Map<Long, String> getValues(Map userParams) {
		FilterValuesHelper valuesHelper = new FilterValuesHelper(authenticationContext, projectManager, groupManager);
		List<String> groupList = new ArrayList<String>();
		groupList.add(ReportsConstant.DELIVERY_HEAD_GROUP_NAME);
		groupList.add(ReportsConstant.PRACTICE_MANAGER_ROLE);

		return valuesHelper.populateProjectCategories(groupList);
	}

	public JiraAuthenticationContext getAuthenticationContext() {
		return authenticationContext;
	}

	public void setAuthenticationContext(JiraAuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
	}

}
