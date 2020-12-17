package com.tadigital.jira.plugin.report;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.annotation.Autowired;


import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class SubTaskingIssueDecoratorExtended implements Issue {
    @Autowired
    private final Issue issue;
    private final Set<Issue> subTasks = new HashSet<Issue>();

    /**
     * Constructs a new instance that decorates the given issue.
     *
     * @param issue issue to decorate
     */
    public SubTaskingIssueDecoratorExtended(Issue issue) {
        this.issue = issue;
    }

    /**
     * Adds the given sub-task to the set of this issue's sub-tasks.
     *
     * @param subTask sub-task to add
     */
    void addSubTask(Issue subTask) {
        if (subTask == null) {
            throw new IllegalArgumentException("subtask cannot be null");
        }
        if (!subTasks.add(subTask)) {
            throw new IllegalArgumentException("Issue " + issue.getKey() + " already contains " + subTask.getKey() + " subtask");
        }
    }

    Issue getDecoratedIssue() {
        return issue;
    }

    ///CLOVER:OFF these are brain-dead delegated methods

    public Collection<Version> getAffectedVersions() {
        return issue.getAffectedVersions();
    }

    public ApplicationUser getAssigneeUser() {
        return issue.getAssigneeUser();
    }

    public ApplicationUser getAssignee() {
        return issue.getAssignee();
    }

    public String getAssigneeId() {
        return issue.getAssigneeId();
    }

    public Collection<Attachment> getAttachments() {
        return issue.getAttachments();
    }

    /**
     * @return
     * @deprecated Use {@link #getComponents()}. Since v7.0
     */
    public Collection<ProjectComponent> getComponentObjects() {
        return getComponents();
    }

    @Override
    public Collection<ProjectComponent> getComponents() {
        return issue.getComponents();
    }

    public Timestamp getCreated() {
        return issue.getCreated();
    }

    public Timestamp getResolutionDate() {
        return issue.getResolutionDate();
    }

    public Object getCustomFieldValue(CustomField customField) {
        return issue.getCustomFieldValue(customField);
    }

    public String getDescription() {
        return issue.getDescription();
    }

    public Timestamp getDueDate() {
        return issue.getDueDate();
    }

    public String getEnvironment() {
        return issue.getEnvironment();
    }

    public Long getEstimate() {
        return issue.getEstimate();
    }

    public Object getExternalFieldValue(String fieldId) {
        return issue.getExternalFieldValue(fieldId);
    }

    public Collection<Version> getFixVersions() {
        return issue.getFixVersions();
    }

    public Long getId() {
        return issue.getId();
    }

    public IssueRenderContext getIssueRenderContext() {
        return issue.getIssueRenderContext();
    }

    public IssueType getIssueType() {
        return issue.getIssueType();
    }

    public IssueType getIssueTypeObject() {
        return issue.getIssueTypeObject();
    }

    public String getIssueTypeId() {
        return issue.getIssueTypeId();
    }

    public String getKey() {
        return issue.getKey();
    }

    @Override
    public Long getNumber() {
        return issue.getNumber();
    }

    public Long getOriginalEstimate() {
        return issue.getOriginalEstimate();
    }

    /**
     * Not supported, throws UnsupportedOperationException.
     */
    public GenericValue getParent() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("GenericValue accessors are not supported!");
    }

    public Long getParentId() {
        return issue.getParentId();
    }

    public Issue getParentObject() {
        return issue.getParentObject();
    }

    public Priority getPriority() {
        return issue.getPriority();
    }

    public Priority getPriorityObject() {
        return issue.getPriorityObject();
    }

    /**
     * Not supported, throws UnsupportedOperationException.
     */
    public GenericValue getProject() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("GenericValue accessors are not supported!");
    }

    public Project getProjectObject() {
        return issue.getProjectObject();
    }

    public Long getProjectId() {
        return issue.getProjectId();
    }

    public ApplicationUser getReporterUser() {
        return issue.getReporterUser();
    }

    public ApplicationUser getReporter() {
        return issue.getReporter();
    }

    public String getReporterId() {
        return issue.getReporterId();
    }

    @Override
    public ApplicationUser getCreator() {
        return issue.getCreator();
    }

    @Override
    public String getCreatorId() {
        return issue.getCreatorId();
    }

    @Override
    public String getResolutionId() {
        return issue.getResolutionId();
    }

    public Resolution getResolution() {
        return issue.getResolution();
    }

    public Resolution getResolutionObject() {
        return issue.getResolutionObject();
    }

    public GenericValue getSecurityLevel() {
        return issue.getSecurityLevel();
    }

    public Long getSecurityLevelId() {
        return issue.getSecurityLevelId();
    }

    /**
     * Not supported, throws UnsupportedOperationException.
     */
    public Status getStatus() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("GenericValue accessors are not supported!");
    }

    @Override
    public String getStatusId() {
        return issue.getStatusId();
    }

    public Status getStatusObject() {
        return issue.getStatusObject();
    }

    public Set<Label> getLabels() {
        return issue.getLabels();
    }

    @Override
    public boolean isArchived() {
        return issue.isArchived();
    }

    @Override
    public ApplicationUser getArchivedByUser() {
        return issue.getArchivedByUser();
    }

    @Override
    public String getArchivedById() {
        return issue.getArchivedById();
    }

    @Override
    public Timestamp getArchivedDate() {
        return issue.getArchivedDate();
    }

    /**
     * Returns a set of sub-tasks. This set that will be displayed on the
     * report and can be a subset of all sub-tasks that this issue has.
     *
     * @return set of sub-tasks, never null
     */
    public Collection<Issue> getSubTaskObjects() {
        return Collections.unmodifiableSet(subTasks);
    }

    /**
     * Throws {@link UnsupportedOperationException). Use {@link #getSubTaskObjects()} method instead.
     */
    public Collection<GenericValue> getSubTasks() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("GenericValue subtasks are not supported. Use getSubTaskObjects() method.");
    }

    public String getSummary() {
        return issue.getSummary();
    }

    public Long getTimeSpent() {
        return issue.getTimeSpent();
    }

    public Timestamp getUpdated() {
        return issue.getUpdated();
    }

    public Long getVotes() {
        return issue.getVotes();
    }

    public Long getWatches() {
        return issue.getWatches();
    }

    public Long getWorkflowId() {
        return issue.getWorkflowId();
    }

    public boolean isCreated() {
        return issue.isCreated();
    }

    public boolean isEditable() {
        return issue.isEditable();
    }

    public boolean isSubTask() {
        return issue.isSubTask();
    }

    public GenericValue getGenericValue() {
        return issue.getGenericValue();
    }

    public Long getLong(String name) {
        return issue.getLong(name);
    }

    public String getString(String name) {
        return issue.getString(name);
    }

    public Timestamp getTimestamp(String name) {
        return issue.getTimestamp(name);
    }

    /**
     * Throws {@link UnsupportedOperationException).
     */
    public void store() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Reports should not be persisting Issues!");
    }

    public int hashCode() {
        return issue.hashCode();
    }

    public boolean equals(Object obj) {
        return issue.equals(obj);
    }

    public String toString() {
        return issue.toString();
    }
}
