package com.tadigital.jira.plugin.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;



@Named
@WebServlet(name = "FileUploadServlet", urlPatterns = { "/taestimateupload" })
@MultipartConfig
public class FileUploadServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(FileUploadServlet.class.getCanonicalName());
	private static final char DEFAULT_SEPARATOR = ',';
	    
	@Autowired
	FileStreamBean fileStreamBean;
	
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if(ServletFileUpload.isMultipartContent(request)){

			try {
				List<FileItem> multiparts = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);

				for(FileItem item : multiparts){

					if(!item.isFormField()){
		
		    			 BufferedReader in1 = new BufferedReader(new InputStreamReader(item.getInputStream(), StandardCharsets.UTF_8));
		    			//Create the CSVFormat object
		    			CSVFormat format = CSVFormat.RFC4180.withHeader().withDelimiter(DEFAULT_SEPARATOR);
		    			
		    			//initialize the CSVParser object
		    			CSVParser parser = new CSVParser(in1, format);
		    			
		    			fileStreamBean.setRowData(parser.getRecords());
						break;
					}
				}

			} catch (Exception e) {

				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				LOGGER.info("Error Uploading File");
			}

		}

	}

}