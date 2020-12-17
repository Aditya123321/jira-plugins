package com.tadigital.jira.reports.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@SuppressWarnings("rawtypes")
public class FilterValuesGenerator implements ValuesGenerator {	

	@ComponentImport
	private JiraAuthenticationContext authenticationContext;

	public FilterValuesGenerator(JiraAuthenticationContext authenticationContext) {
		super();
		this.authenticationContext = authenticationContext;

	}

	@Override
	public Map<String, String> getValues(Map userParams) {

		Map<String, String> projectMap = new HashMap<String, String>();

		List<Project> allProjects = ComponentAccessor.getProjectManager()
				.getProjectsLeadBy(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());

		for (Project proj : allProjects) {
			projectMap.put(proj.getId().toString(), proj.getName());
		}

		return projectMap;
	}

	public JiraAuthenticationContext getAuthenticationContext() {
		return authenticationContext;
	}

	public void setAuthenticationContext(JiraAuthenticationContext authenticationContext) {
		this.authenticationContext = authenticationContext;
	}

}
