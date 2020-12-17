package com.tadigital.jira.plugin.mapping;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingDefinition;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingEntry;
import  com.tadigital.jira.plugin.TAStoryProperties;
import  com.tadigital.jira.plugin.TACsvClient;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class PriorityValueMappingDefinition implements ValueMappingDefinition {
    private final TACsvClient simpleCsvClient;
    private final ConstantsManager constantsManager;
    public PriorityValueMappingDefinition(TACsvClient simpleCsvClient, ConstantsManager constantsManager) {
        this.simpleCsvClient = simpleCsvClient;
        this.constantsManager = constantsManager;
    }
    @Override
    public String getJiraFieldId() {
        return IssueFieldConstants.PRIORITY;
    }
    @SuppressWarnings("deprecation")
	@Override
    public Collection<ValueMappingEntry> getTargetValues() {
        return new ArrayList<ValueMappingEntry>(Collections2.transform(constantsManager.getPriorityObjects(),
                new Function<IssueConstant, ValueMappingEntry>() {
                    public ValueMappingEntry apply(IssueConstant from) {
                        return new ValueMappingEntry(from.getName(), from.getId());
                    }
                }));
    }
    @Override
    public boolean canBeBlank() {
        return false;
    }
    @Override
    public boolean canBeCustom() {
        return true;
    }
    @Override
    public boolean canBeImportedAsIs() {
        return true;
    }
    @Override
    public String getExternalFieldId() {
        return "priority";
    }
    @Override
    public String getDescription() {
        return null;
    }
    @Override
    public Set<String> getDistinctValues() {
        return Sets.newHashSet(Iterables.transform(simpleCsvClient.getInternalIssues(),
                new Function<TAStoryProperties, String>() {
                    @Override
                    public String apply(TAStoryProperties from) {
                        return from.getPriority();
                    }
                }));
    }
    @Override
    public Collection<ValueMappingEntry> getDefaultValues() {
        return new ImmutableList.Builder<ValueMappingEntry>().add(
                new ValueMappingEntry("Low", IssueFieldConstants.TRIVIAL_PRIORITY_ID),
                new ValueMappingEntry("Normal", IssueFieldConstants.MINOR_PRIORITY_ID),
                new ValueMappingEntry("High", IssueFieldConstants.MAJOR_PRIORITY_ID),
                new ValueMappingEntry("Urgent", IssueFieldConstants.CRITICAL_PRIORITY_ID),
                new ValueMappingEntry("Immediate", IssueFieldConstants.BLOCKER_PRIORITY_ID)
        ).build();
    }
    @Override
    public boolean isMandatory() {
        return false;
    }
}