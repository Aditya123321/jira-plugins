package com.tadigital.jira.plugin;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.ofbiz.core.entity.GenericEntityException;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.importer.imports.importer.ImportDataBean;
import com.atlassian.jira.plugins.importer.imports.importer.JiraDataImporter;
import com.atlassian.jira.plugins.importer.web.AbstractImporterController;
import com.atlassian.jira.plugins.importer.web.AbstractSetupPage;
import com.atlassian.jira.plugins.importer.web.ConfigFileHandler;
import com.atlassian.jira.plugins.importer.web.ImportProcessBean;
import com.atlassian.jira.plugins.importer.web.ImporterProjectMappingsPage;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import com.tadigital.jira.plugin.rest.FileStreamBean;
import com.tadigital.jira.plugin.web.TASetupPage;

/**
 * @author Ravi.sangubotla
 *
 */
@Scanned
public class TAStoryImporterController extends AbstractImporterController {

	public static final String IMPORT_CONFIG_BEAN = "com.tadigital.jira.plugin.google.csvimport.config";
	public static final String IMPORT_ID = "com.tadigital.jira.plugin.google.csvimport.import";
	private static final Logger log = Logger.getLogger(TAStoryImporterController.class);

	@ComponentImport(value = "jiraDataImporter")
	private final JiraDataImporter importer;

	@ComponentImport(value = "configFileHandler")
	private final ConfigFileHandler configFileHandler;
	
	
	IssueCRUD issueCRUD;

	@Autowired
	FileStreamBean fileStreamBean;

	@Autowired
	public TAStoryImporterController(JiraDataImporter importer, ConfigFileHandler configFileHandler) {

		super(importer, IMPORT_CONFIG_BEAN, IMPORT_ID);
		this.importer = importer;
		this.configFileHandler = configFileHandler;
		
	}

	@Override
	public boolean createImportProcessBean(AbstractSetupPage abstractSetupPage) {
		if (abstractSetupPage.invalidInput()) {
			return false;
		}
	
	

		log.log(Level.WARN,"ComponentAccessor.getUsername()::"

				+ ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getUsername());
		/*
		 * try { Collection<Long> issueIds =
		 * ComponentAccessor.getIssueManager().getIssueIdsForProject(10000l);
		 * log.log(Priority.WARN, "issue count :: " + issueIds.size());
		 * 
		 * for (Long issueId : issueIds) { log.log(Priority.WARN, "issue id :: " +
		 * issueId); MutableIssue issue =
		 * ComponentAccessor.getIssueManager().getIssueObject(issueId);
		 * log.log(Priority.WARN, "issue summary :: " + issue.getSummary() +
		 * issue.getDescription()+issue.getSubTaskObjects());
		 * 
		 * } } catch (GenericEntityException e) { e.printStackTrace(); }
		 */



		try {
			Collection<Long> issueIds = ComponentAccessor.getIssueManager().getIssueIdsForProject(10001l);
			log.log(Level.WARN,"issue count :: "+issueIds.size());
			
			for(Long issueId : issueIds) {
				log.log(Level.WARN,"issue id :: "+issueId);
				MutableIssue issue  = ComponentAccessor.getIssueManager().getIssueObject(issueId);
				log.log(Level.WARN,"issue summary :: "+issue.getSummary()+issue.getDescription());
				
			}
				
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			
			
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
		
		final TASetupPage setupPage = (TASetupPage) abstractSetupPage;
		//setupPage.getMultipart().getFileNames();
		final TACsvConfigBean configBean = new TACsvConfigBean(new TACsvClient(fileStreamBean.getRowData()));
		final ImportProcessBean importProcessBean = new ImportProcessBean();
		if (!configFileHandler.populateFromConfigFile(setupPage, configBean)) {
			return false;
		}
		importProcessBean.setConfigBean(configBean);
		storeImportProcessBeanInSession(importProcessBean);
		return true;
	}

	@Override
	public ImportDataBean createDataBean() throws Exception {
		final TACsvConfigBean configBean = getConfigBeanFromSession();
		return new TACsvDataBean(configBean);
	}

	private TACsvConfigBean getConfigBeanFromSession() {
		final ImportProcessBean importProcessBean = getImportProcessBeanFromSession();
		return importProcessBean != null ? (TACsvConfigBean) importProcessBean.getConfigBean() : null;
	}

	// @Override no override annotation to remain compatible with JIRA 5
	public List<String> getStepNameKeys() {
		return Lists.newArrayList("com.tadigital.jira.plugin.step.csvSetup",
				"com.tadigital.jira.plugin.step.projectMapping"/*
																 * ,
																 * "com.tadigital.jira.plugin.importer.step.customField"
																 */);
	}

	@Override
	public List<String> getSteps() {
		return Lists.newArrayList(TASetupPage.class.getSimpleName(),
				ImporterProjectMappingsPage.class.getSimpleName()/*
																	 * , ImporterCustomFieldsPage.class.getSimpleName()
																	 */);
	}

}
