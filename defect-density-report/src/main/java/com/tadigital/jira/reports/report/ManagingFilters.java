package com.tadigital.jira.reports.report;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.tadigital.jira.plugin.constants.ReportsConstant;
import com.tadigital.jira.plugin.helper.SprintHelper;
import com.tadigital.jira.utility.HRMSApiCallService;
import com.tadigital.jira.utility.JiraUtils;

public class ManagingFilters {

	SearchService searchService;
	ApplicationUser appUser;
	SearchResults<Issue> searchResults;
	ProjectManager projectManager;

	CustomFieldManager customFieldManager;
	CustomField devCustomField;
	CustomField architectField;
	CustomField bugCategory;

	Long architectEstimatesId;
	Long developerId;
	Long causedByDeveloperId;
	Long bugCategoryId;

	Long defectCount;
	Long size;
	Long subStoriesConsidered;
	Double archEstimatesTotal;
	Long estimatesHours;
	Long actualHours;
	Long allDefects;
	double temp;
	double defectDensity;
	double defectDensityArch;
	double defectDensityActual;

	static final String NO_PROJECT_LEAD = "No Project Lead Assigned";
	static final String NO_PROJECT_CATEGORY = "No Project Category Assigned";
	static final String DEVELOPMENT_BUG = "Development Bug";
	static final String NO_BUG_CATEGORY = "No Bug Category";
	static final String RESOLUTION_INVALID = "Invalid";
	DeveloperAttributesBean developerAttributesBean;

	private static final Logger log = Logger.getLogger(ManagingFilters.class);

	public ManagingFilters(ApplicationUser appUser, SearchService searchService, ProjectManager projectManager) {
		this.appUser = appUser;
		this.searchService = searchService;
		this.projectManager = projectManager;
		this.setCustomFieldIds();
		devCustomField = this.getCustomFieldManager().getCustomFieldObject(getDeveloperId());
		architectField = this.getCustomFieldManager().getCustomFieldObject(getArchitectEstimatesId());
		devCustomField = this.getCustomFieldManager().getCustomFieldObject(getDeveloperId());
		bugCategory = this.getCustomFieldManager().getCustomFieldObject(getBugCategoryId());
	}

	public CustomFieldManager getCustomFieldManager() {
		return ComponentAccessor.getCustomFieldManager();
	}

	public void setCustomFieldManager() {
		this.customFieldManager = ComponentAccessor.getCustomFieldManager();
	}

	public Long getArchitectEstimatesId() {
		return architectEstimatesId;
	}

	public Long getDeveloperId() {
		return developerId;
	}

	public void setArchitectEstimatesId(Long architectEstimatesId) {
		this.architectEstimatesId = architectEstimatesId;
	}

	public void setDeveloperId(Long developerId) {
		this.developerId = developerId;
	}

	public Long getCausedByDeveloperId() {
		return causedByDeveloperId;
	}

	public void setCausedByDeveloperId(Long causedByDeveloperId) {
		this.causedByDeveloperId = causedByDeveloperId;
	}

	public void setCustomFieldIds() {

		for (CustomField cf : getCustomFieldManager().getCustomFieldObjects()) {
			if (cf.getName().equalsIgnoreCase(ReportsConstant.DEVELOPER_CF)) {
				setDeveloperId(cf.getIdAsLong());
			} else if (cf.getName().equalsIgnoreCase(ReportsConstant.ARCHITECT_ESTIMATES_CF)) {
				setArchitectEstimatesId(cf.getIdAsLong());
			} else if (cf.getName().equalsIgnoreCase(ReportsConstant.CAUSED_BY_DEVELOPER)) {
				setCausedByDeveloperId(cf.getIdAsLong());
			} else if (cf.getName().equalsIgnoreCase("Bug Category")) {
				setBugCategoryId(cf.getIdAsLong());
			}
		}
	}

	public ManagingFilters(ApplicationUser appuser) {
		this.appUser = appuser;
	}

	/********************************
	 * For Project Level reports
	 *
	 *******************************/
	// return all the developers from sub-story or sub-task
	public Set<ApplicationUser> getAllDevelopersFromSubTasksUnderProject(Long projectId) throws SearchException {

		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

		builder.where().project(projectId).and()
				.issueType(ReportsConstant.SUB_STORY, ReportsConstant.SUB_DEFECT, ReportsConstant.BUG).and()
				.customField(getDeveloperId()).isNotEmpty();
		Query query = builder.buildQuery();

		this.setCustomFieldIds();

		LinkedHashSet<ApplicationUser> developers = new LinkedHashSet<>();

		for (Issue issue : this.getQueryResults(query).getResults()) {

			developers.add((ApplicationUser) issue.getCustomFieldValue(devCustomField));

		}
		log.info("Developers" + developers);
		return developers;

	}

	// return all the developer statistics
	public List<DeveloperAttributesBean> getDataForProject(Long projectId, Long sprintId)
			throws SearchException, JSONException, IOException {
		List<DeveloperAttributesBean> developerValues = new ArrayList<>();
		SprintHelper sprintHelper = new SprintHelper(searchService, appUser, projectId);
		for (ApplicationUser developer : getAllDevelopersFromSubTasksUnderProject(projectId)) {

			if (getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_FIRSTNAME) != ""
					&& getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_LASTNAME) != "") {
				JqlQueryBuilder builderForAllSubStories = JqlQueryBuilder.newBuilder();
				if(sprintId != null) {
		        	if(!sprintId.equals(Long.MIN_VALUE)) {
		        		builderForAllSubStories.where().project(projectId).and().issueType(ReportsConstant.SUB_STORY).and()
						.customField(sprintHelper.getSprintCustomField().getIdAsLong()).eq(sprintId).and()
						.customField(getDeveloperId()).eq(developer.getName()).and().not()
						.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV); 
		            }
		        	else {
						builderForAllSubStories.where().project(projectId).and().issueType(ReportsConstant.SUB_STORY).and()
						.customField(getDeveloperId()).eq(developer.getName()).and().not()
						.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV);
					}
		        }
				else {
					builderForAllSubStories.where().project(projectId).and().issueType(ReportsConstant.SUB_STORY).and()
					.customField(getDeveloperId()).eq(developer.getName()).and().not()
					.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV);
				}
				Query queryForAllSubStories = builderForAllSubStories.buildQuery();

				JqlQueryBuilder builderConsideredSubDefects = JqlQueryBuilder.newBuilder();
				
				if(sprintId != null) {
		        	if(!sprintId.equals(Long.MIN_VALUE)) {
		        		builderConsideredSubDefects.where().issueType(ReportsConstant.SUB_DEFECT, ReportsConstant.BUG).and()
						.project(projectId).and().sub().sub()
						.addStringCondition(ReportsConstant.ISSUE_LINK, ReportsConstant.CAUSED_BY).and()
						.customField(sprintHelper.getSprintCustomField().getIdAsLong()).eq(sprintId).and()
						.customField(getCausedByDeveloperId()).eq(developer.getName()).endsub().or().sub()
						.customField(getDeveloperId()).eq(developer.getName()).and().customField(getDeveloperId())
						.isNotEmpty().endsub().endsub().and().sub().not().resolution(RESOLUTION_INVALID).or()
						.resolution().isEmpty().endsub();
		            }
		        	else {
		        		builderConsideredSubDefects.where().issueType(ReportsConstant.SUB_DEFECT, ReportsConstant.BUG).and()
						.project(projectId).and().sub().sub()
						.addStringCondition(ReportsConstant.ISSUE_LINK, ReportsConstant.CAUSED_BY).and()
						.customField(getCausedByDeveloperId()).eq(developer.getName()).endsub().or().sub()
						.customField(getDeveloperId()).eq(developer.getName()).and().customField(getDeveloperId())
						.isNotEmpty().endsub().endsub().and().sub().not().resolution(RESOLUTION_INVALID).or()
						.resolution().isEmpty().endsub();
		        	}
		        }
				else {
					builderConsideredSubDefects.where().issueType(ReportsConstant.SUB_DEFECT, ReportsConstant.BUG).and()
					.project(projectId).and().sub().sub()
					.addStringCondition(ReportsConstant.ISSUE_LINK, ReportsConstant.CAUSED_BY).and()
					.customField(getCausedByDeveloperId()).eq(developer.getName()).endsub().or().sub()
					.customField(getDeveloperId()).eq(developer.getName()).and().customField(getDeveloperId())
					.isNotEmpty().endsub().endsub().and().sub().not().resolution(RESOLUTION_INVALID).or()
					.resolution().isEmpty().endsub();
				}
				Query queryForSubDefects = builderConsideredSubDefects.buildQuery();

				JqlQueryBuilder builderConsideredSubStories = JqlQueryBuilder.newBuilder();
				if(sprintId != null) {
		        	if(!sprintId.equals(Long.MIN_VALUE)) {
		        		builderConsideredSubStories.where().project(projectId).and()
		        		.customField(sprintHelper.getSprintCustomField().getIdAsLong()).eq(sprintId).and()
		        		.issueType(ReportsConstant.SUB_STORY).and()
						.customField(getDeveloperId()).eq(developer.getName()).and().not()
						.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV).and()
						.originalEstimate().isNotEmpty().and().customField(getArchitectEstimatesId()).isNotEmpty();
		            }
		        	else {
		        		builderConsideredSubStories.where().project(projectId).and().issueType(ReportsConstant.SUB_STORY).and()
						.customField(getDeveloperId()).eq(developer.getName()).and().not()
						.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV).and()
						.originalEstimate().isNotEmpty().and().customField(getArchitectEstimatesId()).isNotEmpty();
		        	}
		        }
				else {
					builderConsideredSubStories.where().project(projectId).and().issueType(ReportsConstant.SUB_STORY).and()
					.customField(getDeveloperId()).eq(developer.getName()).and().not()
					.status(ReportsConstant.STATUS_OPEN).and().not().status(ReportsConstant.STATUS_DEV).and()
					.originalEstimate().isNotEmpty().and().customField(getArchitectEstimatesId()).isNotEmpty();
				}

				Query queryForSubStoriesConsidered = builderConsideredSubStories.buildQuery();

				archEstimatesTotal = 0.00d;
				defectCount = getQueryResultsSize(getQueryResults(queryForSubDefects).getResults());
				estimatesHours = 0l;
				actualHours = 0l;
				defectDensity = 0.00d;
				defectDensityArch = 0.00d;
				defectDensityActual = 0.00d;
				allDefects = 0l;

				for (Issue issue : this.getQueryResults(queryForSubStoriesConsidered).getResults()) {
					archEstimatesTotal += (double) issue.getCustomFieldValue(architectField);
					estimatesHours += issue.getOriginalEstimate();
					if (issue.getTimeSpent() != null) {
						actualHours += issue.getTimeSpent();
					} else {
						actualHours += 0;
					}
				}
				if (estimatesHours != 0l) {
					defectDensity = defectCount / getDefectDensityCalculated((double) getConvertedTime(estimatesHours));
				}
				if (archEstimatesTotal != 0.00d) {
					defectDensityArch = defectCount / getDefectDensityCalculated(archEstimatesTotal);
				}
				if (actualHours != 0l) {
					defectDensityActual = defectCount
							/ getDefectDensityCalculated((double) getConvertedTime(actualHours));
				}
				developerAttributesBean = new DeveloperAttributesBean(
						getQueryResultsSize(getQueryResults(queryForAllSubStories).getResults()),
						getQueryResultsSize(getQueryResults(queryForSubStoriesConsidered).getResults()),
						Precision.round(archEstimatesTotal, 1), getConvertedTime(estimatesHours),
						getConvertedTime(actualHours), defectCount, Precision.round(defectDensityArch, 1),
						Precision.round(defectDensity, 1), Precision.round(defectDensityActual, 1));
				developerAttributesBean
						.setDeveloperName(getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_FIRSTNAME)
								+ " " + getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_LASTNAME));
				developerAttributesBean.setJiraUserName(developer.getUsername());
				developerAttributesBean.setBand(getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_BAND));
				developerAttributesBean.setDesignation(getDeveloperData(developer.getUsername(), ReportsConstant.JSON_GET_DESIGNATION));

				developerValues.add(developerAttributesBean);
			}
		}
		Collections.sort(developerValues, (dev1, dev2) -> dev1.getDeveloperName().compareTo(dev2.getDeveloperName()));
		return developerValues;
	}

	public List<BugData> getNonDevBugCategoriesData(Long projectId) throws SearchException {
		List<BugData> bugList = new ArrayList<>();

		for (String bugCat : getBugCategories()) {
			if (!bugCat.equalsIgnoreCase(DEVELOPMENT_BUG) && !bugCat.equalsIgnoreCase(NO_BUG_CATEGORY)) {
				JqlQueryBuilder bugCount = JqlQueryBuilder.newBuilder();
				bugCount.where().project(projectId).and().issueType(ReportsConstant.BUG).and()
						.customField(getBugCategoryId()).eq(bugCat);
				Query queryForAllBugs = bugCount.buildQuery();
				bugList.add(new BugData(bugCat, getQueryResultsSize(getQueryResults(queryForAllBugs).getResults())));
			}
		}
		JqlQueryBuilder bugCatEmptyCount = JqlQueryBuilder.newBuilder();
		bugCatEmptyCount.where().project(projectId).and().issueType(ReportsConstant.BUG).and()
				.customField(getBugCategoryId()).isEmpty();
		Query queryForCatEmptyBugs = bugCatEmptyCount.buildQuery();
		bugList.add(
				new BugData(NO_BUG_CATEGORY, getQueryResultsSize(getQueryResults(queryForCatEmptyBugs).getResults())));
		Collections.sort(bugList, (bug1, bug2) -> bug1.getBugCategory().compareTo(bug2.getBugCategory()));
		return bugList;
	}

	public List<BugData> getBugDataForGroup(List<DeveloperAttributesBean> dataProjectGroup) {

		List<BugData> totalListData = new ArrayList<BugData>();
		long count;
		for (String cat : getBugCategories()) {
			count = 0l;
			for (DeveloperAttributesBean bean : dataProjectGroup) {
				for (BugData data : bean.getBugDataList()) {
					if (cat.equalsIgnoreCase(data.getBugCategory())) {
						count += data.getBugCount();
					}
				}

			}
			totalListData.add(new BugData(cat, count));
		}
		return totalListData;
	}

	public String getDeveloperData(String jiraUserName, String parameter) throws JSONException, IOException {

		HRMSApiCallService appService = new HRMSApiCallService();
		String value = "";
		JSONObject obj;
		if (!StringUtils.isEmpty(jiraUserName)) {
			JSONArray devArray = new JSONArray(appService.getDeveloperData(jiraUserName));
			if (!devArray.isEmpty()) {
				obj = devArray.getJSONObject(0);
				value = (String) obj.get(parameter);
			}
			return value;
		} else {
			return value;
		}
	}

	/********************************
	 * For Delivery Head Filters
	 *
	 *******************************/

	public Set<Project> getAllProjects(Long categoryId) {
		LinkedHashSet<Project> clientProjects = new LinkedHashSet<Project>();
		if (categoryId == Long.MAX_VALUE) {
			for (Project project : projectManager.getProjects()) {
				log.info("Project::" + project);

				if (project.getProjectCategory() != null && project.getProjectCategory().getName() != null
						&& project.getProjectCategory().getName().contains("Client.")) {
					clientProjects.add(project);
				}
			}
		} else {

			for (Project project : projectManager.getProjects()) {
				log.info("Project::" + project);

				if ((project.getProjectCategory() != null && project.getProjectCategory().getName() != null)
						&& project.getProjectCategory().getId().equals(categoryId)) {

					clientProjects.add(project);
				}
			}

		}
		log.info("Client Projects :::" + clientProjects);
		return clientProjects;

	}

	private void populateProjectStatus(DeveloperAttributesBean clientProjectsBean) throws SearchException {
		final JqlQueryBuilder queryBuilder = JqlQueryBuilder.newBuilder();
		final JqlClauseBuilder builder = queryBuilder.where().issueType(ReportsConstant.ISSUETYPE_RELEASE);
		builder.and().project(clientProjectsBean.getProjectId());
		final SearchResults<Issue> searchResults = searchService.search(appUser, queryBuilder.buildQuery(),
				PagerFilter.getUnlimitedFilter());
		int releaseCount = searchResults.getTotal();
		clientProjectsBean.setNumberOfRelease(releaseCount);
		if (releaseCount == 1) {
			clientProjectsBean.setProjectStatus(searchResults.getResults().get(0).getStatus().getSimpleStatus());
		}
	}

	public List<DeveloperAttributesBean> getDataforProjectGroup(Long categoryId)
			throws SearchException, JSONException, IOException {

		List<DeveloperAttributesBean> projectGroupStatistics = new ArrayList<>();

		for (Project project : getAllProjects(categoryId)) {

			List<DeveloperAttributesBean> dAttrBean;
			dAttrBean = getDataForProject(project.getId(), null);
			DeveloperAttributesBean projectData = getTotal(dAttrBean);

			projectData.setProject(project.getName());
			projectData.setProjectId(project.getId());
			projectData.setBugDataList(getNonDevBugCategoriesData(project.getId()));
			projectData.setNonDevBugCount(getBugTotal(getNonDevBugCategoriesData(project.getId())));
			projectData
					.setAllDefects(projectData.getDefects() + getBugTotal(getNonDevBugCategoriesData(project.getId())));

			if (project.getProjectCategory() != null && project.getProjectLead() != null) {

				projectData.setProjectCategory(project.getProjectCategory().getName());
				projectData.setProjectLead(project.getProjectLead().getName());

			} else if (project.getProjectCategory() != null && project.getProjectLead() == null) {

				projectData.setProjectCategory(project.getProjectCategory().getName());
				projectData.setProjectLead(NO_PROJECT_LEAD);

			} else if (project.getProjectCategory() == null && project.getProjectLead() != null) {

				projectData.setProjectCategory(NO_PROJECT_CATEGORY);
				projectData.setProjectLead(project.getProjectLead().getName());

			} else if (project.getProjectCategory() == null && project.getProjectLead() == null) {
				projectData.setProjectCategory(NO_PROJECT_CATEGORY);
				projectData.setProjectLead(NO_PROJECT_LEAD);
			}
			populateProjectStatus(projectData);
			projectGroupStatistics.add(projectData);

		}
		log.info("Client Projects Statistics" + projectGroupStatistics);

		return projectGroupStatistics;
	}

	/********************************
	 * Practice Managers
	 * 
	 * @throws ParseException
	 * 
	 *******************************/

	public List<DeveloperAttributesBean> getPracticeStatistics(String jsonList, String practice, String start,
			String end, String band) throws SearchException, ParseException {

		List<DeveloperAttributesBean> practiceList = new ArrayList<>();

		JSONArray jsonarray = new JSONArray(jsonList);
		ReportsConstant reportsConstant = new ReportsConstant();

		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject obj = jsonarray.getJSONObject(i);
			if (!reportsConstant.getNotRequiredList().contains(obj.get(ReportsConstant.JSON_GET_DESIGNATION).toString())
					&& (obj.get("bandName").toString().equalsIgnoreCase(band)
							|| (band.equalsIgnoreCase(ReportsConstant.ALL)))) {
				practiceList
						.add(getDeveloperEntryForPractice(obj.get(ReportsConstant.JSON_GET_LOWERUSERNAME).toString(),
								obj.get(ReportsConstant.JSON_GET_ID).toString(), practice,
								obj.get(ReportsConstant.JSON_GET_DESIGNATION).toString(), start, end,
								obj.get(ReportsConstant.JSON_GET_FIRSTNAME).toString() + " "
										+ obj.get(ReportsConstant.JSON_GET_LASTNAME),
								obj.get("bandName").toString()));
			}

		}

		Collections.sort(practiceList, (dev1, dev2) -> dev1.getDeveloperName().compareTo(dev2.getDeveloperName()));

		return practiceList;

	}

	public DeveloperAttributesBean getDeveloperEntryForPractice(String jiraName, String devId, String practice,
			String designation, String from, String to, String name, String band)
			throws SearchException, ParseException {

		JiraUtils jiraUtils = new JiraUtils();

		JqlQueryBuilder builderForAllSubStories = JqlQueryBuilder.newBuilder();
		builderForAllSubStories.where().issueType(ReportsConstant.SUB_STORY).and().customField(getDeveloperId())
				.eq(jiraName).and().status(ReportsConstant.STATUS_CLOSED).and().resolutionDateBetween(
						jiraUtils.getFormattedDate(from, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT),
						jiraUtils.getFormattedDate(to, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
		Query queryForAllSubStories = builderForAllSubStories.buildQuery();

		JqlQueryBuilder builderConsideredSubDefects = JqlQueryBuilder.newBuilder();
		builderConsideredSubDefects.where().issueType(ReportsConstant.SUB_DEFECT, ReportsConstant.BUG).and().sub().sub()
				.addStringCondition(ReportsConstant.ISSUE_LINK, ReportsConstant.CAUSED_BY).and()
				.customField(getCausedByDeveloperId()).eq(jiraName).endsub().or().sub().customField(getDeveloperId())
				.eq(jiraName).and().customField(getDeveloperId()).isNotEmpty().endsub().endsub().and()
				.createdBetween(
						jiraUtils.getFormattedDate(from, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT),
						jiraUtils.getFormattedDate(jiraUtils.getNextDay(to), ReportsConstant.DATE_FORMAT,
								ReportsConstant.JQL_DATE_FORMAT))
				.and().sub().not().resolution(RESOLUTION_INVALID).or().resolution().isEmpty().endsub();
		Query queryForSubDefects = builderConsideredSubDefects.buildQuery();

		JqlQueryBuilder builderConsideredSubStories = JqlQueryBuilder.newBuilder();
		builderConsideredSubStories.where().issueType(ReportsConstant.SUB_STORY).and().customField(getDeveloperId())
				.eq(jiraName).and().status(ReportsConstant.STATUS_CLOSED).and().originalEstimate().isNotEmpty().and()
				.customField(getArchitectEstimatesId()).isNotEmpty().and().resolutionDateBetween(
						jiraUtils.getFormattedDate(from, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT),
						jiraUtils.getFormattedDate(to, ReportsConstant.DATE_FORMAT, ReportsConstant.JQL_DATE_FORMAT));
		Query queryForSubStoriesConsidered = builderConsideredSubStories.buildQuery();

		size = getQueryResultsSize(getQueryResults(queryForAllSubStories).getResults());
		subStoriesConsidered = getQueryResultsSize(getQueryResults(queryForSubStoriesConsidered).getResults());
		archEstimatesTotal = 0.00d;
		defectCount = getQueryResultsSize(getQueryResults(queryForSubDefects).getResults());
		estimatesHours = 0l;
		actualHours = 0l;
		defectDensity = 0.00d;
		defectDensityArch = 0.00d;
		defectDensityActual = 0.00d;

		for (Issue issue : this.getQueryResults(queryForSubStoriesConsidered).getResults()) {
			archEstimatesTotal += (double) issue.getCustomFieldValue(architectField);
			estimatesHours += issue.getOriginalEstimate();
			if (issue.getTimeSpent() != null) {
				actualHours += issue.getTimeSpent();
			} else {
				actualHours += 0;
			}
		}

		if (estimatesHours != 0) {
			defectDensity = defectCount / getDefectDensityCalculated((double) getConvertedTime(estimatesHours));
		}
		if (archEstimatesTotal != 0) {
			defectDensityArch = defectCount / getDefectDensityCalculated(archEstimatesTotal);
		}
		if (actualHours != 0) {
			defectDensityActual = defectCount / getDefectDensityCalculated((double) getConvertedTime(actualHours));
		}

		DeveloperAttributesBean clientProjectsBean = new DeveloperAttributesBean(size, subStoriesConsidered,
				Precision.round(archEstimatesTotal, 1), getConvertedTime(estimatesHours), getConvertedTime(actualHours),
				defectCount, Precision.round(defectDensityArch, 1), Precision.round(defectDensity, 1),
				Precision.round(defectDensityActual, 1));
		clientProjectsBean.setDeveloperName(name);
		clientProjectsBean.setJiraUserName(jiraName);
		clientProjectsBean.setDeveloperId(devId);
		clientProjectsBean.setPractice(practice);
		clientProjectsBean.setBand(band);
		clientProjectsBean.setDesignation(designation);

		return clientProjectsBean;
	}

	private SearchResults<Issue> getQueryResults(Query query) throws SearchException {

		return searchService.search(appUser, query, PagerFilter.getUnlimitedFilter());

	}

	public Long getConvertedTime(Long time) {
		return time / 3600;
	}

	public Double getDefectDensityCalculated(Double value) {
		return value / 800l;

	}

	public long getQueryResultsSize(List<Issue> result) {
		return (long) result.size();
	}

	public DeveloperAttributesBean getTotal(List<DeveloperAttributesBean> allDataBean) {

		size = 0l;
		subStoriesConsidered = 0l;
		archEstimatesTotal = 0.00d;
		actualHours = 0l;
		defectDensityActual = 0.00d;
		defectCount = 0l;
		estimatesHours = 0l;
		defectDensity = 0.00d;
		defectDensityArch = 0.00d;
		allDefects = 0l;

		if (!allDataBean.isEmpty()) {

			for (DeveloperAttributesBean dataBean : allDataBean) {

				size = size + dataBean.getTotalStories();
				subStoriesConsidered += dataBean.getTotalSubStoriesConsidered();
				archEstimatesTotal = archEstimatesTotal + dataBean.getArchitectEstimates();
				defectCount = defectCount + dataBean.getDefects();
				estimatesHours = estimatesHours + dataBean.getOriginalEstimatesTotal();
				actualHours = actualHours + dataBean.getActualHours();
				if (dataBean.getAllDefects() != 0l) {
					allDefects += dataBean.getAllDefects();
				}
			}

		}
		if (estimatesHours != 0) {
			defectDensity = defectCount / getDefectDensityCalculated((double) estimatesHours);
		}
		if (archEstimatesTotal != 0) {
			defectDensityArch = defectCount / getDefectDensityCalculated(archEstimatesTotal);
		}
		if (actualHours != 0) {
			defectDensityActual = defectCount / getDefectDensityCalculated((double) actualHours);
		}

		DeveloperAttributesBean bean = new DeveloperAttributesBean(size, subStoriesConsidered,
				Precision.round(archEstimatesTotal, 1), estimatesHours, actualHours, defectCount,
				Precision.round(defectDensityArch, 1), Precision.round(defectDensity, 1),
				Precision.round(defectDensityActual, 1));
		bean.setAllDefects(allDefects);
		return bean;
	}

	public List<String> getBugCategories() {
		List<String> categoryList = new ArrayList<String>();
		Options categories = ComponentAccessor.getOptionsManager()
				.getOptions(bugCategory.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());

		for (Option bugCat : categories) {
			if (!bugCat.getValue().equalsIgnoreCase(DEVELOPMENT_BUG)
					&& !bugCat.getValue().equalsIgnoreCase(ReportsConstant.NONE)) {
				categoryList.add(bugCat.getValue());
			}
		}
		categoryList.add(NO_BUG_CATEGORY);
		Collections.sort(categoryList, (cat1, cat2) -> cat1.compareTo(cat2));
		return categoryList;

	}

	public long getBugTotal(List<BugData> bugData) {
		long total = 0l;
		for (BugData data : bugData) {
			total += data.getBugCount();
		}
		return total;
	}

	public Long getBugCategoryId() {
		return bugCategoryId;
	}

	public void setBugCategoryId(Long bugCategoryId) {
		this.bugCategoryId = bugCategoryId;
	}
}
