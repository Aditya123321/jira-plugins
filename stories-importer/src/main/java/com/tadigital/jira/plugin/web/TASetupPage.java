package com.tadigital.jira.plugin.web;

import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.plugins.importer.extensions.ImporterController;
import com.atlassian.jira.plugins.importer.tracking.UsageTrackingService;
import com.atlassian.jira.plugins.importer.web.AbstractSetupPage;
import com.atlassian.jira.plugins.importer.web.ConfigFileHandler;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.tadigital.jira.plugin.TACsvClient;
import com.tadigital.jira.plugin.rest.FileStreamBean;

/**
 * @author Ravi.sangubotla
 *
 */
@Scanned
public class TASetupPage extends AbstractSetupPage {

	private static final long serialVersionUID = 1L;
	
	@Autowired
	FileStreamBean fileStreamBean;
	
	@ComponentImport("configFileHandler")
	private final ConfigFileHandler configFileHandler;

	@ComponentImport("usageTrackingService")
	private final UsageTrackingService usageTrackingService;

	@ComponentImport("webInterfaceManager")
	private final WebInterfaceManager webInterfaceManager;

	@ComponentImport("pluginAccessor")
	private final PluginAccessor pluginAccessor;
	
	@SuppressWarnings("deprecation")
	@Autowired
	public TASetupPage(UsageTrackingService usageTrackingService, WebInterfaceManager webInterfaceManager,
			PluginAccessor pluginAccessor, ConfigFileHandler configFileHandler) {
		super(usageTrackingService, webInterfaceManager, pluginAccessor);

		this.usageTrackingService = usageTrackingService;
		this.webInterfaceManager = webInterfaceManager;
		this.pluginAccessor = pluginAccessor;
		this.configFileHandler = configFileHandler;
	}

	@Override
	public String doDefault() throws Exception {
		//remove admin check
		/* if (!isAdministrator()) { return "denied"; } */
		 
		final ImporterController controller = getController();
		if (controller == null) {
			return RESTART_NEEDED;
		}
		return INPUT;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception {
		final ImporterController controller = getController();
		if (controller == null) {
			return RESTART_NEEDED;
		}
		if (!isPreviousClicked() && !controller.createImportProcessBean(this)) {
			return INPUT;
		}
		return super.doExecute();
	}

	@Override
	protected void doValidation() {
		if (isPreviousClicked()) {
			return;
		}
		try {
			
			new TACsvClient(fileStreamBean.getRowData());
		} catch (RuntimeException e) {
			addError("fileRowData", e.getMessage());
		} 
		super.doValidation();
		configFileHandler.verifyConfigFileParam(this);
	}
	
}