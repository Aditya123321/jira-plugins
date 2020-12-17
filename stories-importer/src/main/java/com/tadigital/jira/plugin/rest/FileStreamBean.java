package com.tadigital.jira.plugin.rest;

import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class FileStreamBean {
	
	private List<CSVRecord>  rowData;

	public List<CSVRecord> getRowData() {
		return rowData;
	}

	public void setRowData(List<CSVRecord> rowData) {
		this.rowData = rowData;
	}

	
	

}
