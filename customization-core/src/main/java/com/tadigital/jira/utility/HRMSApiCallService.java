package com.tadigital.jira.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.tadigital.jira.plugin.constants.ReportsConstant;

public class HRMSApiCallService {

	// private Properties properties = new Properties();

	private static final Logger log = Logger.getLogger(HRMSApiCallService.class);

	public HRMSApiCallService() {
		/*
		 * InputStream input = null; try { input =
		 * getClass().getClassLoader().getResourceAsStream(
		 * "customization-core.properties"); this.properties.load(input); } catch
		 * (IOException e) { log.warn(e);
		 * 
		 * } finally { if (input != null) { try { input.close(); } catch (IOException e)
		 * { log.warn(e); } }
		 * 
		 * }
		 */
	}

	public String getApiCall(String practiceName) throws IOException, Exception {

		String urlParameters = "";

		 if (practiceName != null) {
			urlParameters = "?practice=" + URLEncoder.encode(practiceName, ReportsConstant.UTF);
		}
		String endpointURL = ReportsConstant.prodBaseUrl + ReportsConstant.employeeAPI + urlParameters;
		URL url = null;
		try {
			url = new URL(endpointURL);
		} catch (Exception e) {

			log.warn(e);
			throw e;
		}
		return executePostRequest(url);
	}

	private String executePostRequest(URL url) throws IOException {
		String ret = StringUtils.EMPTY;
		BufferedReader in = null;
		String readLine = null;
		HttpsURLConnection con = null;

		try {
			con = (HttpsURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			int responseCode = con.getResponseCode();			

			if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));				

				StringBuilder response = new StringBuilder();
				while ((readLine = in.readLine()) != null) {
					response.append(readLine);
				}
				ret = response.toString();
			}
		} catch (IOException e) {

			log.warn(e);
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
			if (con != null) {
				con.disconnect();
			}

		}

		return ret;
	}

	public Map<String, String> getPreparedURL(String url) throws IOException {
		URL prepareUrl = new URL(url);
		String[] values = executePostRequest(prepareUrl).substring(1, executePostRequest(prepareUrl).length() - 1)
				.split(",");
		Map<String, String> dropdownValues = new LinkedHashMap<String, String>();
		Arrays.sort(values);
		for (String s : values) {
			dropdownValues.put(getFormattedString(s), getFormattedString(s));

		}

		return dropdownValues;
	}

	public String getFormattedString(String value) {
		return value.replaceAll("^\"+|\"+$", "");
	}
	
	public String getDeveloperData(String jiraUsername) throws IOException {
		
		String urlParameters = "";
		if(!StringUtils.isEmpty(jiraUsername)) {
			urlParameters = "?jiraUserName=" + URLEncoder.encode(jiraUsername, ReportsConstant.UTF);			
		}
		String endpointURL = ReportsConstant.prodBaseUrl + ReportsConstant.devDeveloperAPI + urlParameters;
		
		URL url = null;
		try {
			url = new URL(endpointURL);
		} catch (Exception e) {

			log.warn(e);
			throw e;
		}
		
		return executePostRequest(url);
		
	}

	public String getPracticeAPICall(String practiceName, String bandName) throws Exception {

		String urlParameters = "";
		if(practiceName.equalsIgnoreCase(ReportsConstant.ALL)) {
			practiceName = ReportsConstant.ALLPRACTICES;
		}
		if(bandName.equalsIgnoreCase(ReportsConstant.ALL)) {
			bandName = ReportsConstant.ALLBANDS;
		}
		if (!StringUtils.isEmpty(practiceName) && !StringUtils.isEmpty(bandName)) {
			urlParameters = "?practice=" +URLEncoder.encode(practiceName, ReportsConstant.UTF)+ "&band=" + URLEncoder.encode(bandName, ReportsConstant.UTF);
		}
		String endpointURL = ReportsConstant.prodBaseUrl + ReportsConstant.practiceBandAPI + urlParameters;
		URL url = null;
		try {
			url = new URL(endpointURL);
		} catch (Exception e) {

			log.warn(e);
			throw e;
		}
		return executePostRequest(url);
	}

	/*
	 * public Properties getProperties() { return properties; }
	 * 
	 * public void setProperties(Properties properties) { this.properties =
	 * properties; }
	 */
}
