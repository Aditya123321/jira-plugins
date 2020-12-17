/*
 * package it.com.tadigital.jira.plugin.importer;
 * 
 * import com.atlassian.jira.pageobjects.JiraTestedProduct; import
 * com.atlassian.jira.pageobjects.config.EnvironmentBasedProductInstance; import
 * com.atlassian.jira.plugins.importer.po.ExternalImportPage; import
 * com.atlassian.jira.plugins.importer.po.common.ImporterLinksPage; import
 * com.atlassian.jira.plugins.importer.po.common.ImporterProjectsMappingsPage;
 * import com.atlassian.jira.testkit.client.Backdoor; import
 * com.atlassian.jira.testkit.client.restclient.SearchRequest; import
 * com.atlassian.jira.testkit.client.restclient.SearchResult; import
 * com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData; import
 * com.tadigital.jira.plugin.storiesimporter.po.SimpleCsvSetupPage; import
 * org.junit.Before; import org.junit.Test;
 * 
 * import static org.junit.Assert.*; import static
 * org.junit.matchers.JUnitMatchers.hasItem;
 * 
 * public class TestSimpleCsvImporter { private JiraTestedProduct jira; private
 * Backdoor backdoor;
 * 
 * @Before public void setUp() { backdoor = new Backdoor(new
 * TestKitLocalEnvironmentData()); backdoor.restoreBlankInstance(); jira = new
 * JiraTestedProduct(null, new EnvironmentBasedProductInstance()); }
 * 
 * @Test public void testSimpleCsvImporterAttachedToConfigPage() {
 * assertThat(jira.gotoLoginPage().loginAsSysAdmin(ExternalImportPage.class).
 * getImportersOrder(), hasItem("SimpleCSVImporter")); }
 * 
 * @Test public void testSimpleCsvImporterWizard() {
 * backdoor.issueLinking().enable();
 * backdoor.issueLinking().createIssueLinkType("Related", "related",
 * "related to"); final SimpleCsvSetupPage setupPage =
 * jira.gotoLoginPage().loginAsSysAdmin(SimpleCsvSetupPage.class); final String
 * file = getClass().getResource("/test.csv").getFile(); final
 * ImporterProjectsMappingsPage projectsPage =
 * setupPage.setFilePath(file).next(); projectsPage.createProject("project",
 * "JIRA Project", "PRJ"); final ImporterLinksPage linksPage =
 * projectsPage.next().next().next().next(); linksPage.setSelect("link",
 * "Related"); assertTrue(linksPage.next().waitUntilFinished().isSuccess());
 * final SearchResult search = backdoor.search().getSearch(new
 * SearchRequest().jql("")); assertEquals((Integer) 4, search.total); } }
 */