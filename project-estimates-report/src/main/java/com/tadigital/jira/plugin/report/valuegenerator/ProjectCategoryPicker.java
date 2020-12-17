package com.tadigital.jira.plugin.report.valuegenerator;

import java.util.Map;

import javax.inject.Inject;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.tadigital.jira.plugin.report.helper.ProjectCategoryHelper;

public class ProjectCategoryPicker implements ValuesGenerator<Long>{
	
	@JiraImport
	private JiraAuthenticationContext authenticationContext;

	@JiraImport
	private ProjectManager projectManager;

	@JiraImport
	private GroupManager groupManager;
	
	@Inject
	public ProjectCategoryPicker(JiraAuthenticationContext authenticationContext, ProjectManager projectManager,
			GroupManager groupManager) {
		super();
		this.authenticationContext = authenticationContext;
		this.projectManager = projectManager;
		this.groupManager = groupManager;
	}

	@Override
	public Map<Long, String> getValues(Map userParams) {
		ProjectCategoryHelper projectCategoryHelper =  new ProjectCategoryHelper(authenticationContext, projectManager, groupManager);
		return projectCategoryHelper.populateProjectCategories();
	}
}
