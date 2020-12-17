package com.tadigital.jira.plugin.impl;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.tadigital.jira.plugin.TAStoryComponent;

/**
 * @author Ravi.sangubotla
 *
 */
@Scanned
public class TAStoryComponentImpl implements TAStoryComponent {
	@ComponentImport("applicationProperties")
	private final ApplicationProperties applicationProperties;

	private static final Logger log = Logger.getLogger(TAStoryComponentImpl.class);

	@Autowired
	public TAStoryComponentImpl(ApplicationProperties applicationProperties) {
		this.applicationProperties = applicationProperties;
	}

	@Override
	public String getName() {
		log.info("ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getUsername()::"
				+ ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getUsername());
		if (null != applicationProperties) {
			return "myComponent:" + applicationProperties.getDisplayName();
		}

		return "myComponent";
	}
}