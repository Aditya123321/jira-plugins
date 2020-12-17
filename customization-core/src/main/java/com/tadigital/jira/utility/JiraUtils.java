package com.tadigital.jira.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.tadigital.jira.plugin.constants.ReportsConstant;

public class JiraUtils {

	private static final Logger log = Logger.getLogger(JiraUtils.class);

	HRMSApiCallService apiCallService = new HRMSApiCallService();

	// private Properties properties = new Properties();

	public JiraUtils() {
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

	public Map<String, String> getPracticeValues(JiraAuthenticationContext authenticationContext, GroupManager group,
			String groupName) {

		Map<String, String> result = new LinkedHashMap<>();
		result.put(ReportsConstant.NONE, ReportsConstant.NONE);
		ApplicationUser user = authenticationContext.getLoggedInUser();
		log.info("user" + user);

		if (group.isUserInGroup(user, group.getGroup(groupName))) {

			try {
				result.putAll(apiCallService.getPreparedURL(ReportsConstant.prodBaseUrl + ReportsConstant.practiceAPI));

			} catch (Exception e) {
				log.warn(e);
			}
		}
		result.put(ReportsConstant.ALL, ReportsConstant.ALLPRACTICES);
		return result;

	}

	public Map<String, String> getBandValues(JiraAuthenticationContext authenticationContext, GroupManager group,
			String groupName) {

		Map<String, String> result = new LinkedHashMap<>();

		ApplicationUser user = authenticationContext.getLoggedInUser();
		log.warn("user" + user);

		if (group.isUserInGroup(user, group.getGroup(groupName))) {
			result.put(ReportsConstant.ALL, ReportsConstant.ALLBANDS);
			try {
				result.putAll(apiCallService.getPreparedURL(ReportsConstant.prodBaseUrl + ReportsConstant.bandAPI));
			} catch (Exception e) {
				log.warn(e);
			}
		} else {
			result.put(ReportsConstant.NONE, ReportsConstant.NONE);
		}
		return result;
	}

	public String getFormattedDate(String date, String oldFormat, String newFormat) throws ParseException {
		if (!StringUtils.isEmpty(date)) {
			Date myDate = new SimpleDateFormat(oldFormat).parse(date);

			return new SimpleDateFormat(newFormat).format(myDate).toString();
		} else {
			return null;
		}
	}
	public String getNextDay(String currDate) throws ParseException
	{	SimpleDateFormat format = new SimpleDateFormat(ReportsConstant.DATE_FORMAT);
	  Date date = format.parse(currDate);
	  Calendar calendar = Calendar.getInstance();
	  calendar.setTime(date);
	  calendar.add(Calendar.DAY_OF_YEAR, 1);
	  return format.format(calendar.getTime()); 
		
	}
	public LinkedHashMap<Long, String> getSortedValues(Map<Long, String> values) {
		return values.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

	}
	/*
	 * public Properties getProperties() { return properties; }
	 * 
	 * public void setProperties(Properties properties) { this.properties =
	 * properties; }
	 */
}
