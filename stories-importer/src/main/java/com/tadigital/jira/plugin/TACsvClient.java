package com.tadigital.jira.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVRecord;

/**
 * we assume data is in constant format issueId,summary, priority, assignee,
 * customField,linkedIssueId
 */
public class TACsvClient {

	private final static Logger LOGGER = Logger.getLogger(TACsvClient.class.getCanonicalName());

	private List<TAStoryProperties> internalIssues=new ArrayList<>();

	public TACsvClient(List<CSVRecord> rowData) {

		try {
			int rowCount=1;
			//Array index starting from 1. 0 index is for column names. Removing row with column names.
			for (CSVRecord record:rowData) {
				rowCount++;
				if (record.size() != 16) {
					throw new RuntimeException("Invalid line " + (rowCount) + " " + record.getRecordNumber() + " should contain 16 columns");
					
				}
				internalIssues.add(new TAStoryProperties(record));
			}
		} catch (Exception e) {
			LOGGER.info("Error reading CSV Record");
		}
	}

	public List<TAStoryProperties> getInternalIssues() {
		return internalIssues;
	}

	public void setInternalIssues(List<TAStoryProperties> internalIssues) {
		this.internalIssues = internalIssues;
	}
	
	
}