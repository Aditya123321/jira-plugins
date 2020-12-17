package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.jira.portal.SortingValuesGenerator;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.jira.bc.project.DefaultProjectService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.project.Project;
import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.query.Query;
import com.atlassian.jira.component.ComponentAccessor;
import org.apache.commons.collections.map.ListOrderedMap;
import com.atlassian.jira.issue.issuetype.IssueType;
import org.ofbiz.core.entity.GenericValue;


import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import java.util.*;


public class EpicPicker implements ValuesGenerator {
    @ComponentImport
    private SearchService searchService;
    @ComponentImport
    private JiraAuthenticationContext authenticationContext;

    private static final String EPIC_CONSTANT = "1";
    private static final String ALL_SELECT = "2";
    private static final String NONE_SELECT = "3";

    @Inject
    public EpicPicker(SearchService searchService, JiraAuthenticationContext authenticationContext) {
        this.searchService = searchService;
        this.authenticationContext = authenticationContext;
    }

    public Map getValues(Map userParams) {
        GenericValue project = (GenericValue)userParams.get("project");
        long projectId = Long.parseLong(project.getString("id"));
        Map<String, String> result = new HashMap<>();
        result.put(ALL_SELECT,"All");
        result.put(NONE_SELECT,"None");
        Set<Issue> issues = new HashSet<Issue>();
        try {
            issues = searchIssues(authenticationContext.getLoggedInUser(), projectId);
            for(Issue issue : issues){
                IssueType issueType= issue.getIssueType();
                if(issueType.getName().contains("Epic")) {
                    result.put(issue.getSummary(), issue.getSummary());
                    }
                }
        } catch(SearchException e){
                //do nothing
        }

        if(result.size() < 2){
            result.clear();
            result.put(EPIC_CONSTANT,"No Epics Available");
        }
        Map resultMap = new ListOrderedMap();
        resultMap.putAll(result);
        return resultMap;
    }

    private Query getSearchForParents(final Long projectId) {
        final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
        final JqlClauseBuilder builder = queryBuilder.where().project(projectId);

        return queryBuilder.buildQuery();
    }

    private Set<Issue> searchIssues(final ApplicationUser user, final Long projectId) throws SearchException {
        final Query parentQuery = getSearchForParents(projectId);
        final SearchResults<Issue> parentSearchResults = searchService.search(user,parentQuery,new PagerFilter(Integer.MAX_VALUE));
        final Set<Issue> result = new LinkedHashSet<Issue>();
        final List<Issue> parentIssues = parentSearchResults.getResults();
        result.addAll(parentIssues);

        return result;
    }
}
