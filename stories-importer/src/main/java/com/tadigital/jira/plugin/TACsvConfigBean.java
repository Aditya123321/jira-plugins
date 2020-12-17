package com.tadigital.jira.plugin;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.importer.external.beans.ExternalCustomField;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingDefinition;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingDefinitionsFactory;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingHelper;
import com.atlassian.jira.plugins.importer.imports.config.ValueMappingHelperImpl;
import com.atlassian.jira.plugins.importer.imports.importer.AbstractConfigBean2;
import com.google.common.collect.Lists;
import com.tadigital.jira.plugin.mapping.PriorityValueMappingDefinition;

public class TACsvConfigBean extends AbstractConfigBean2 {
	
	private TACsvClient csvClient;
	
	
	
	private IssueCRUD issueCRUD;
	
	
	public TACsvConfigBean(TACsvClient csvClient) {
		this.csvClient = csvClient;
	}

	@Override
	public List<String> getExternalProjectNames() {
		return Lists.newArrayList("project");
	}

	@Override
	public List<ExternalCustomField> getCustomFields() {
		List<ExternalCustomField> fieldArray = new ArrayList<ExternalCustomField>();
		//return Lists.newArrayList(ExternalCustomField.createText("cf", "Custom Field"));
		 return fieldArray;
	}

	@Override
	public List<String> getLinkNamesFromDb() {
		return Lists.newArrayList("link");
	}

	@SuppressWarnings("deprecation")
	@Override
	public ValueMappingHelper initializeValueMappingHelper() {
		final ValueMappingDefinitionsFactory mappingDefinitionFactory = new ValueMappingDefinitionsFactory() {
			public List<ValueMappingDefinition> createMappingDefinitions(ValueMappingHelper valueMappingHelper) {
				final List<ValueMappingDefinition> mappings = Lists.newArrayList();
				mappings.add(new PriorityValueMappingDefinition(getCsvClient(), getConstantsManager()));
				return mappings;
			}
		};
		return new ValueMappingHelperImpl(getWorkflowSchemeManager(), getWorkflowManager(), mappingDefinitionFactory,
				getConstantsManager());
	}

	public TACsvClient getCsvClient() {
		return csvClient;
	}


	
	public IssueCRUD getIssueCRUD() {
		return issueCRUD;
	}

}