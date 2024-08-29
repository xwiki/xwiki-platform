/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.ui.extension;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.DefaultExtensionIssueManagement;
import org.xwiki.extension.DefaultExtensionScm;
import org.xwiki.extension.DefaultExtensionScmConnection;
import org.xwiki.extension.DefaultExtensionSupportPlan;
import org.xwiki.extension.DefaultExtensionSupportPlans;
import org.xwiki.extension.DefaultExtensionSupporter;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.test.po.AdvancedSearchPane;
import org.xwiki.extension.test.po.DependencyPane;
import org.xwiki.extension.test.po.ExtensionAdministrationPage;
import org.xwiki.extension.test.po.ExtensionDependenciesPane;
import org.xwiki.extension.test.po.ExtensionDescriptionPane;
import org.xwiki.extension.test.po.ExtensionPane;
import org.xwiki.extension.test.po.ExtensionProgressPane;
import org.xwiki.extension.test.po.LogItemPane;
import org.xwiki.extension.test.po.MergeConflictPane;
import org.xwiki.extension.test.po.PaginationFilterPane;
import org.xwiki.extension.test.po.ProgressBarPane;
import org.xwiki.extension.test.po.SearchResultsPane;
import org.xwiki.extension.test.po.SimpleSearchPane;
import org.xwiki.extension.test.po.UnusedPagesPane;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.repository.test.TestExtension;
import org.xwiki.test.ui.AbstractExtensionAdminAuthenticatedIT;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.diff.RawChanges;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the Extension Manager user interface.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionIT extends AbstractExtensionAdminAuthenticatedIT
{
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // Make sure the extensions we are playing with are not already installed.
        getExtensionTestUtils().finishCurrentJob();
        getExtensionTestUtils().uninstall("alice-xar-extension");
        getExtensionTestUtils().uninstall("bob-xar-extension");
        getExtensionTestUtils().uninstall("scriptServiceJarExtension");

        // Delete the pages that are provided by the XAR extensions we use in tests.
        getUtil().rest().deletePage("ExtensionTest", "Alice");
        assertFalse(getUtil().pageExists("ExtensionTest", "Alice"));
        getUtil().rest().deletePage("ExtensionTest", "Bob");
        assertFalse(getUtil().pageExists("ExtensionTest", "Bob"));

        // Delete from the repository the XAR extensions we use in tests.
        // The extension page name is either the extension name, if specified, or the extension id. Most of the tests
        // don't set the extension name but some do and we end up with two extensions (two pages) with the same id.
        getRepositoryTestUtils().deleteExtension("Alice Wiki Macro");
        getRepositoryTestUtils().deleteExtension("Bob Wiki Macro");
        getRepositoryTestUtils().deleteExtension("alice-xar-extension");
        getRepositoryTestUtils().deleteExtension("bob-xar-extension");
        getRepositoryTestUtils().deleteExtension("scriptServiceJarExtension");

        getRepositoryTestUtils().waitUntilReady();

        // Double check that the XWiki Extension Repository is empty.
        ExtensionsSearchResult searchResult =
            getUtil().rest().getResource("repository/search", Collections.singletonMap("number", new Object[] {1}));
        assertEquals(0, searchResult.getTotalHits());
    }

    /**
     * The extension search results pagination.
     */
    @Test
    public void testPagination()
    {
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoCoreExtensions();

        SearchResultsPane searchResults = adminPage.getSearchResults();
        assertNull(searchResults.getNoResultsMessage());
        assertEquals(20, searchResults.getDisplayedResultsCount());

        PaginationFilterPane pagination = searchResults.getPagination();
        assertEquals((pagination.getResultsCount() + 20 - 1) / 20, pagination.getPageCount());
        assertEquals("1 - 20", pagination.getCurrentRange());
        assertEquals(1, pagination.getCurrentPageIndex());
        assertFalse(pagination.hasPreviousPage());
        assertTrue(pagination.hasNextPage());
        assertTrue(pagination.getPageCount() > 5);
        assertTrue(pagination.getResultsCount() > 100);
        String firstExtensionName = searchResults.getExtension(0).getName();

        pagination = pagination.gotoPage(4);
        searchResults = new SearchResultsPane();
        assertEquals(20, searchResults.getDisplayedResultsCount());
        assertEquals("61 - 80", pagination.getCurrentRange());
        assertEquals(4, pagination.getCurrentPageIndex());
        assertTrue(pagination.hasNextPage());
        String secondExtensionName = searchResults.getExtension(0).getName();
        assertFalse(firstExtensionName.equals(secondExtensionName));

        pagination = pagination.previousPage();
        searchResults = new SearchResultsPane();
        assertEquals(20, searchResults.getDisplayedResultsCount());
        assertEquals("41 - 60", pagination.getCurrentRange());
        assertEquals(3, pagination.getCurrentPageIndex());
        String thirdExtensionName = searchResults.getExtension(0).getName();
        assertFalse(firstExtensionName.equals(thirdExtensionName));
        assertFalse(secondExtensionName.equals(thirdExtensionName));

        pagination = pagination.nextPage();
        searchResults = new SearchResultsPane();
        assertEquals(20, searchResults.getDisplayedResultsCount());
        assertEquals("61 - 80", pagination.getCurrentRange());
        assertEquals(4, pagination.getCurrentPageIndex());
        assertEquals(secondExtensionName, searchResults.getExtension(0).getName());

        pagination = pagination.gotoPage(pagination.getPageCount());
        searchResults = new SearchResultsPane();
        assertEquals(((pagination.getResultsCount() - 1) % 20) + 1, searchResults.getDisplayedResultsCount());
        assertEquals(pagination.getPageCount(), pagination.getCurrentPageIndex());
        assertFalse(pagination.hasNextPage());
        assertTrue(pagination.hasPreviousPage());
    }

    /**
     * Tests the simple search form.
     */
    @Test
    public void testSimpleSearch()
    {
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoCoreExtensions();
        int coreExtensionCount = adminPage.getSearchResults().getPagination().getResultsCount();
        SimpleSearchPane searchBar = adminPage.getSearchBar();

        // Check if the tip is displayed.
        assertEquals("search extension...", searchBar.getSearchInput().getAttribute("placeholder"));
        // Check that the input is empty
        assertEquals("", searchBar.getSearchInput().getAttribute("value"));

        SearchResultsPane searchResults = searchBar.search("XWiki Commons");
        assertTrue(searchResults.getPagination().getResultsCount() < coreExtensionCount);

        // Make sure the search input is not cleared.
        searchBar = new SimpleSearchPane();
        assertEquals("XWiki Commons", searchBar.getSearchInput().getAttribute("value"));

        assertNull(searchResults.getNoResultsMessage());

        // Check that the results match the search query.
        for (int i = 0; i < searchResults.getPagination().getPageCount(); i++) {
            ExtensionPane extension = searchResults.getExtension(i);
            assertTrue(
                "Can't find [commons] in the summary/id/name parts of extension [" + extension.getId() + "] ("
                    + extension.getSummary() + ")",
                extension.getSummary().toLowerCase().contains("commons")
                    || extension.getId().getId().toLowerCase().contains("commons")
                    || extension.getName().toLowerCase().contains("commons"));
            assertEquals("core", extension.getStatus());
        }

        // Test search query with no results.
        searchResults = new SimpleSearchPane().search("blahblah");
        assertEquals(0, searchResults.getDisplayedResultsCount());
        assertNull(searchResults.getPagination());
        assertEquals("There were no extensions found matching 'blahblah'. Try different keywords. "
            + "Alternatively, if you know the identifier and the version of the extension you're "
            + "looking for, you can use the Advanced Search form above.", searchResults.getNoResultsMessage());

        // Test a search query with only a few results (only one page).
        searchResults = searchBar.search("groovy");
        assertNull(searchResults.getNoResultsMessage());
        assertNull(searchResults.getPagination());
        assertTrue(searchResults.getDisplayedResultsCount() > 1);

        ExtensionPane extension = searchResults.getExtension(0);
        assertEquals("core", extension.getStatus());
        assertTrue("Can't find [groovy] in the name of the extension [" + extension.getId() + "] ("
            + extension.getName() + ")", extension.getName().toLowerCase().contains("groovy"));
    }

    /**
     * Tests the advanced search form.
     */
    @Test
    public void testAdvancedSearch()
    {
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoCoreExtensions();

        SearchResultsPane searchResults = adminPage.getSearchBar().search("groovy");
        String version = searchResults.getExtension(0).getVersion();

        searchResults = new SimpleSearchPane().clickAdvancedSearch().search("org.apache.groovy:groovy", version);
        assertEquals(1, searchResults.getDisplayedResultsCount());
        assertNull(searchResults.getNoResultsMessage());
        ExtensionPane extension = searchResults.getExtension(0);
        assertEquals("core", extension.getStatus());
        assertTrue(extension.getName().toLowerCase().contains("groovy"));
        assertEquals(version, extension.getVersion());

        searchResults = new SimpleSearchPane().clickAdvancedSearch().search("foo", "bar");
        assertEquals(0, searchResults.getDisplayedResultsCount());
        assertNull(searchResults.getPagination());
        assertEquals(
            "We couldn't find any extension with id 'foo' and version 'bar'. "
                + "Make sure you have the right extension repositories configured.",
            searchResults.getNoResultsMessage());

        // Test cancel advanced search.
        AdvancedSearchPane advancedSearchPane = new SimpleSearchPane().clickAdvancedSearch();
        advancedSearchPane.getIdInput().sendKeys("id");
        assertTrue(advancedSearchPane.getVersionInput().isDisplayed());
        advancedSearchPane.getCancelButton().click();
        assertFalse(advancedSearchPane.getVersionInput().isDisplayed());
    }

    /**
     * Tests how core extensions are displayed.
     */
    @Test
    public void testCoreExtensions()
    {
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoCoreExtensions();

        // Assert that the core extension repository is selected.
        SimpleSearchPane searchBar = adminPage.getSearchBar();
        assertEquals("Core extensions", searchBar.getRepositorySelect().getFirstSelectedOption().getText());

        ExtensionPane extension = adminPage.getSearchResults().getExtension(RandomUtils.nextInt(0, 20));
        assertEquals("core", extension.getStatus());
        assertEquals("Provided", extension.getStatusMessage());
        assertNull(extension.getInstallButton());
        assertNull(extension.getUninstallButton());
        assertNull(extension.getUpgradeButton());
        assertNull(extension.getDowngradeButton());
        // Just test that the button to show the extension details is present.
        assertEquals("core", extension.showDetails().getStatus());
    }

    /**
     * Tests the extension repository selector (all, core, installed, local).
     */
    @Test
    public void testRepositorySelector() throws Exception
    {
        // Setup the extension.
        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "xar");
        getRepositoryTestUtils().addExtension(extension);
        getRepositoryTestUtils().waitUntilReady();

        // Check that the Supported Extensions are displayed by default.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        Select repositorySelect = adminPage.getSearchBar().getRepositorySelect();
        assertEquals("Available Extensions", repositorySelect.getFirstSelectedOption().getText());

        // Check that a remote extension appears only in the list of "All Extensions".
        adminPage.getSearchBar().selectRepository("installed");
        SearchResultsPane searchResults = adminPage.getSearchBar().search("alice");
        assertNull(searchResults.getExtension(extensionId));

        adminPage.getSearchBar().selectRepository("");
        adminPage = new ExtensionAdministrationPage();
        // Test direct search
        adminPage = adminPage.setIndexed(false);
        // The value of the search input must be preserved when we switch the repository.
        assertEquals("alice", adminPage.getSearchBar().getSearchInput().getAttribute("value"));
        assertNotNull(adminPage.getSearchResults().getExtension(extensionId));
        assertNull(new SimpleSearchPane().selectRepository("local").getExtension(extensionId));

        // Check that an installed extension appears also in "Installed Extensions" and "Local Extensions".
        getExtensionTestUtils().install(extensionId);
        adminPage = ExtensionAdministrationPage.gotoPage();
        adminPage.getSearchBar().selectRepository("installed");
        searchResults = adminPage.getSearchBar().search("alice");
        assertNotNull(searchResults.getExtension(extensionId));
        assertNotNull(new SimpleSearchPane().selectRepository("local").getExtension(extensionId));

        adminPage.getSearchBar().selectRepository("");
        adminPage = new ExtensionAdministrationPage();
        // Test direct search
        adminPage = adminPage.setIndexed(false);
        assertNotNull(adminPage.getSearchBar().selectRepository("").getExtension(extensionId));

        // Check local extension.
        getExtensionTestUtils().uninstall(extensionId.getId(), true);
        adminPage = ExtensionAdministrationPage.gotoPage();
        adminPage.getSearchBar().selectRepository("installed");
        searchResults = adminPage.getSearchBar().search("alice");
        assertNull(searchResults.getExtension(extensionId));
        assertNotNull(new SimpleSearchPane().selectRepository("local").getExtension(extensionId));

        adminPage.getSearchBar().selectRepository("");
        adminPage = new ExtensionAdministrationPage();
        // Test direct search
        adminPage = adminPage.setIndexed(false);
        assertNotNull(adminPage.getSearchBar().selectRepository("").getExtension(extensionId));
    }

    /**
     * Tests the extension details (license, web site).
     */
    @Test
    public void testShowDetails() throws Exception
    {
        // Setup the extension.
        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "xar");
        extension.setName("Alice Wiki Macro");
        extension.setSummary("A **useless** macro");
        extension.addAuthor(new DefaultExtensionAuthor("Thomas", (String) null));
        extension.addAuthor(new DefaultExtensionAuthor("Marius", (String) null));
        extension.addFeature("alice-extension");
        extension.addLicense(new ExtensionLicense("My own license", null));
        extension.setWebsite("http://www.alice.com");
        extension.setScm(new DefaultExtensionScm("https://github.com/xwiki-contrib/alice-xar-extension",
            new DefaultExtensionScmConnection("git", "git://github.com/xwiki-contrib/alice-xar-extension.git"),
            new DefaultExtensionScmConnection("git", "git:git@github.com:xwiki-contrib/alice-xar-extension.git")));
        extension
            .setIssueManagement(new DefaultExtensionIssueManagement("jira", "https://jira.xwiki.org/browse/ALICE"));
        getRepositoryTestUtils().addExtension(extension);

        // Search the extension and assert the displayed information.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);
        assertEquals("remote", extensionPane.getStatus());
        assertNull(extensionPane.getStatusMessage());
        assertEquals(extension.getName(), extensionPane.getName());
        assertEquals(extensionId.getVersion().getValue(), extensionPane.getVersion());
        List<WebElement> authors = extensionPane.getAuthors();
        assertEquals(2, authors.size());
        assertEquals("Thomas", authors.get(0).getText());
        assertEquals("Marius", authors.get(1).getText());
        assertEquals(extension.getSummary(), extensionPane.getSummary());

        // Check the extension details.
        ExtensionDescriptionPane descriptionPane = extensionPane.openDescriptionSection();
        assertEquals(extension.getLicenses().iterator().next().getName(), descriptionPane.getLicense());
        assertEquals(extension.getId().getId(), descriptionPane.getId());
        assertEquals(extension.getFeatures().iterator().next(), descriptionPane.getFeatures().get(0));
        assertEquals(extension.getType(), descriptionPane.getType());

        WebElement webSiteLink = descriptionPane.getWebSite();
        assertEquals(extension.getWebSite().substring("http://".length()), webSiteLink.getText());
        assertEquals(extension.getWebSite() + '/', webSiteLink.getAttribute("href"));

        // Install the extension to check the details that are specific to installed extensions.
        Date beforeInstall = new Date();
        descriptionPane = extensionPane.install().confirm().openDescriptionSection();
        Date afterInstall = new Date();

        List<String> namespaces = descriptionPane.getNamespaces();
        assertEquals(1, namespaces.size());
        String prefix = "Home, by superadmin on ";
        assertTrue(namespaces.get(0).startsWith(prefix));
        Date installDate = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(namespaces.get(0).substring(prefix.length()));
        // Ignore the seconds as they are not displayed.
        assertTrue(String.format("Install date [%s] should be after [%s].", installDate, beforeInstall),
            installDate.getTime() / 60000 >= beforeInstall.getTime() / 60000);
        assertTrue(String.format("Install date [%s] should be before [%s].", installDate, afterInstall),
            installDate.before(afterInstall));
    }

    /**
     * Tests how extension dependencies are displayed (both direct and backward dependencies).
     */
    @Test
    public void testDependencies() throws Exception
    {
        // Setup the extension and its dependencies.
        ExtensionId dependencyId = new ExtensionId("bob-xar-extension", "2.5-milestone-2");
        TestExtension dependency = getRepositoryTestUtils().getTestExtension(dependencyId, "xar");
        dependency.setName("Bob Wiki Macro");
        dependency.setSummary("Required by Alice");
        getRepositoryTestUtils().addExtension(dependency);

        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "xar");
        extension.addDependency(new DefaultExtensionDependency(dependencyId.getId(),
            new DefaultVersionConstraint(dependencyId.getVersion().getValue())));
        extension
            .addDependency(new DefaultExtensionDependency("missing-dependency", new DefaultVersionConstraint("135")));
        extension.addDependency(new DefaultExtensionDependency("org.xwiki.platform:xwiki-platform-sheet-api",
            new DefaultVersionConstraint("[3.2,)")));
        extension.addDependency(new DefaultExtensionDependency("org.xwiki.commons:xwiki-commons-diff-api",
            new DefaultVersionConstraint("2.7")));
        extension.addDependency(new DefaultExtensionDependency("org.xwiki.platform:xwiki-platform-display-api",
            new DefaultVersionConstraint("100.1")));
        getRepositoryTestUtils().addExtension(extension);

        // Search the extension and assert the list of dependencies.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);
        ExtensionDependenciesPane dependenciesPane = extensionPane.openDependenciesSection();

        List<DependencyPane> directDependencies = dependenciesPane.getDirectDependencies();
        assertEquals(5, directDependencies.size());

        assertEquals(dependency.getName(), directDependencies.get(0).getName());
        assertEquals(dependencyId.getVersion().getValue(), directDependencies.get(0).getVersion());
        assertEquals("remote", directDependencies.get(0).getStatus());
        assertNull(directDependencies.get(0).getStatusMessage());

        assertNull(directDependencies.get(1).getLink());
        assertEquals("missing-dependency", directDependencies.get(1).getName());
        assertEquals("135", directDependencies.get(1).getVersion());
        assertEquals("unknown", directDependencies.get(1).getStatus());
        assertNull(directDependencies.get(1).getStatusMessage());

        assertNotNull(directDependencies.get(2).getLink());
        assertEquals("XWiki Platform - Sheet - API", directDependencies.get(2).getName());
        assertEquals("[3.2,)", directDependencies.get(2).getVersion());
        assertEquals("core", directDependencies.get(2).getStatus());
        assertEquals("Provided", directDependencies.get(2).getStatusMessage());

        assertNotNull(directDependencies.get(3).getLink());
        assertEquals("XWiki Commons - Diff API", directDependencies.get(3).getName());
        assertEquals("2.7", directDependencies.get(3).getVersion());
        assertEquals("remote-core", directDependencies.get(3).getStatus());
        assertTrue(directDependencies.get(3).getStatusMessage().matches("Version [^\\s]+ is provided"));

        assertEquals("XWiki Platform - Display API", directDependencies.get(4).getName());
        assertEquals("100.1", directDependencies.get(4).getVersion());
        assertEquals("remote-core-incompatible", directDependencies.get(4).getStatus());
        assertTrue(directDependencies.get(4).getStatusMessage().matches("Incompatible with provided version [^\\s]+"));

        assertTrue(dependenciesPane.getBackwardDependencies().isEmpty());

        // Follow the link to a dependency.
        directDependencies.get(0).getLink().click();
        adminPage = new ExtensionAdministrationPage();
        extensionPane = adminPage.getSearchResults().getExtension(0);
        assertEquals(dependency.getName(), extensionPane.getName());
        assertEquals(dependencyId.getVersion().getValue(), extensionPane.getVersion());
        assertEquals(dependency.getSummary(), extensionPane.getSummary());

        // Check that we are still in the administration.
        assertTrue(new AdministrationPage().hasSection("XWiki.Extensions"));
    }

    /**
     * Tests how an extension is installed.
     */
    @Test
    public void testInstall() throws Exception
    {
        // Setup the extension and its dependencies.
        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "xar");

        ExtensionId dependencyId = new ExtensionId("bob-xar-extension", "2.5-milestone-2");
        getRepositoryTestUtils().addExtension(getRepositoryTestUtils().getTestExtension(dependencyId, "xar"));
        extension.addDependency(new DefaultExtensionDependency(dependencyId.getId(),
            new DefaultVersionConstraint(dependencyId.getVersion().getValue())));

        extension.addDependency(new DefaultExtensionDependency("org.xwiki.platform:xwiki-platform-sheet-api",
            new DefaultVersionConstraint("[3.2,)")));
        getRepositoryTestUtils().addExtension(extension);

        // Search the extension and install it.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);
        extensionPane = extensionPane.install();

        // Assert the install plan.
        List<DependencyPane> installPlan = extensionPane.openProgressSection().getJobPlan();
        assertEquals(2, installPlan.size());
        assertEquals(dependencyId, installPlan.get(0).getId());
        assertEquals(extensionId, installPlan.get(1).getId());

        // Finish the install and assert the install log.
        List<LogItemPane> log = extensionPane.confirm().openProgressSection().getJobLog();
        int logSize = log.size();
        assertTrue(logSize > 1);
        assertEquals("info", log.get(0).getLevel());
        assertEquals(
            "Starting job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(0).getMessage());
        assertEquals("info", log.get(logSize - 1).getLevel());
        assertEquals(
            "Finished job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(logSize - 1).getMessage());

        // Test that both extensions are usable.
        ViewPage viewPage = getUtil().createPage(getTestClassName(), getTestMethodName(), "{{alice/}}\n\n{{bob/}}", "");
        String content = viewPage.getContent();
        assertTrue(content.contains("Alice says hello!"));
        assertTrue(content.contains("Bob says hi!"));

        // Check the list of installed extensions.
        adminPage = ExtensionAdministrationPage.gotoInstalledExtensions();

        SearchResultsPane searchResults = adminPage.getSearchBar().search("bob");
        assertEquals(1, searchResults.getDisplayedResultsCount());
        extensionPane = searchResults.getExtension(0);
        assertEquals("installed-dependency", extensionPane.getStatus());
        assertEquals("Installed as dependency", extensionPane.getStatusMessage());
        assertEquals(dependencyId, extensionPane.getId());
        assertNotNull(extensionPane.getUninstallButton());

        searchResults = new SimpleSearchPane().search("alice");
        assertEquals(1, searchResults.getDisplayedResultsCount());
        extensionPane = searchResults.getExtension(0);
        assertEquals("installed", extensionPane.getStatus());
        assertEquals("Installed", extensionPane.getStatusMessage());
        assertEquals(extensionId, extensionPane.getId());
        assertNotNull(extensionPane.getUninstallButton());

        // Check if the progress log is persisted.
        extensionPane = extensionPane.showDetails();
        log = extensionPane.openProgressSection().getJobLog();
        assertEquals(logSize, log.size());
        assertEquals("info", log.get(0).getLevel());
        assertEquals(
            "Starting job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(0).getMessage());
        assertEquals("info", log.get(logSize - 1).getLevel());
        assertEquals(
            "Finished job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(logSize - 1).getMessage());

        // Check if the dependency is properly listed as installed.
        List<DependencyPane> dependencies = extensionPane.openDependenciesSection().getDirectDependencies();
        assertEquals(2, dependencies.size());
        assertEquals(dependencyId, dependencies.get(0).getId());
        assertEquals("installed-dependency", dependencies.get(0).getStatus());
        assertEquals("Installed as dependency", dependencies.get(0).getStatusMessage());

        // Check the backward dependency.
        dependencies.get(0).getLink().click();
        extensionPane = new ExtensionAdministrationPage().getSearchResults().getExtension(0);
        dependencies = extensionPane.openDependenciesSection().getBackwardDependencies();
        assertEquals(1, dependencies.size());
        assertEquals(extensionId, dependencies.get(0).getId());
        assertEquals("installed", dependencies.get(0).getStatus());
        assertEquals("Installed", dependencies.get(0).getStatusMessage());
    }

    /**
     * Tests how an extension is uninstalled.
     */
    @Test
    public void testUninstall() throws Exception
    {
        // Setup the extension and its dependencies.
        ExtensionId dependencyId = new ExtensionId("bob-xar-extension", "2.5-milestone-2");
        getRepositoryTestUtils().addExtension(getRepositoryTestUtils().getTestExtension(dependencyId, "xar"));

        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "xar");
        extension.addDependency(new DefaultExtensionDependency(dependencyId.getId(),
            new DefaultVersionConstraint(dependencyId.getVersion().getValue())));
        getRepositoryTestUtils().addExtension(extension);

        // Install the extensions.
        getExtensionTestUtils().install(extensionId);

        // Check if the installed pages are present.
        assertTrue(getUtil().pageExists("ExtensionTest", "Alice"));
        assertTrue(getUtil().pageExists("ExtensionTest", "Bob"));

        // Uninstall the dependency.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoInstalledExtensions();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(dependencyId).getExtension(0);
        extensionPane = extensionPane.uninstall();

        // Check the uninstall plan. Both extensions should be included.
        List<DependencyPane> uninstallPlan = extensionPane.openProgressSection().getJobPlan();
        assertEquals(2, uninstallPlan.size());

        assertEquals(extensionId, uninstallPlan.get(0).getId());
        assertEquals("installed", uninstallPlan.get(0).getStatus());
        assertEquals("Installed", uninstallPlan.get(0).getStatusMessage());

        assertEquals(dependencyId, uninstallPlan.get(1).getId());
        assertEquals("installed-dependency", uninstallPlan.get(1).getStatus());
        assertEquals("Installed as dependency", uninstallPlan.get(1).getStatusMessage());

        // Check the confirmation to delete the unused wiki pages.
        extensionPane = extensionPane.confirm();
        UnusedPagesPane unusedPages = extensionPane.openProgressSection().getUnusedPages();
        assertTrue(unusedPages.contains("ExtensionTest", "Alice"));
        assertTrue(unusedPages.contains("ExtensionTest", "Bob"));

        // Finish the uninstall and check the log.
        extensionPane = extensionPane.confirm();
        List<LogItemPane> log = extensionPane.openProgressSection().getJobLog();
        assertTrue(log.size() > 2);
        assertEquals("info", log.get(2).getLevel());
        assertEquals("Resolving extension [bob-xar-extension 2.5-milestone-2] from namespace [Home]",
            log.get(2).getMessage());
        assertEquals("info", log.get(log.size() - 1).getLevel());
        assertEquals("Finished job of type [uninstall] with identifier [extension/action/bob-xar-extension/wiki:xwiki]",
            log.get(log.size() - 1).getMessage());

        // Check if the uninstalled pages have been deleted.
        assertFalse(getUtil().pageExists("ExtensionTest", "Alice"));
        assertFalse(getUtil().pageExists("ExtensionTest", "Bob"));

        // Install both extension again and uninstall only the one with the dependency.
        getExtensionTestUtils().install(extensionId);

        adminPage = ExtensionAdministrationPage.gotoInstalledExtensions();
        extensionPane = adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);
        extensionPane = extensionPane.uninstall();

        // Check the uninstall plan. Only one extension should be included.
        uninstallPlan = extensionPane.openProgressSection().getJobPlan();
        assertEquals(1, uninstallPlan.size());
        assertEquals(extensionId, uninstallPlan.get(0).getId());

        // Check the confirmation to delete the unused wiki pages.
        extensionPane = extensionPane.confirm();
        unusedPages = extensionPane.openProgressSection().getUnusedPages();
        assertTrue(unusedPages.contains("ExtensionTest", "Alice"));
        assertFalse(unusedPages.contains("ExtensionTest", "Bob"));

        // Finish the uninstall and check the log.
        log = extensionPane.confirm().openProgressSection().getJobLog();
        assertTrue(log.size() > 2);
        assertEquals("info", log.get(2).getLevel());
        assertEquals("Resolving extension [alice-xar-extension 1.3] from namespace [Home]", log.get(2).getMessage());
        assertEquals("info", log.get(log.size() - 1).getLevel());
        assertEquals(
            "Finished job of type [uninstall] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(log.size() - 1).getMessage());

        // Check if the uninstalled pages have been deleted.
        assertFalse(getUtil().pageExists("ExtensionTest", "Alice"));
        assertTrue(getUtil().pageExists("ExtensionTest", "Bob"));

        // Check the list of installed extensions. It should contain only the second extension.
        adminPage = ExtensionAdministrationPage.gotoInstalledExtensions();
        SearchResultsPane searchResults = adminPage.getSearchBar().search("alice");
        assertEquals(0, searchResults.getDisplayedResultsCount());
        assertNotNull(searchResults.getNoResultsMessage());

        searchResults = new SimpleSearchPane().search("bob");
        assertEquals(1, searchResults.getDisplayedResultsCount());
        extensionPane = searchResults.getExtension(0);
        assertEquals("installed-dependency", extensionPane.getStatus());
        assertEquals(dependencyId, extensionPane.getId());
    }

    /**
     * Tests that an extension can be installed and uninstalled without reloading the extension manager UI.
     */
    @Test
    public void testInstallAndUninstallWithoutReload() throws Exception
    {
        // Setup the extension.
        ExtensionId extensionId = new ExtensionId("alice-xar-extension", "1.3");
        getRepositoryTestUtils().addExtension(getRepositoryTestUtils().getTestExtension(extensionId, "xar"));

        // Search the extension to install.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);

        // Install and uninstall.
        extensionPane = extensionPane.install().confirm().uninstall().confirm().confirm().install();
        assertEquals("remote", extensionPane.getStatus());
    }

    /**
     * Tests how an extension is upgraded.
     */
    @Test
    public void testUpgrade() throws Exception
    {
        // Setup the extension.
        String extensionId = "alice-xar-extension";
        String oldVersion = "1.3";
        String newVersion = "2.1.4";
        TestExtension oldExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, oldVersion), "xar");
        getRepositoryTestUtils().addExtension(oldExtension);
        TestExtension newExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, newVersion), "xar");
        getRepositoryTestUtils().attachFile(newExtension);
        getRepositoryTestUtils().addVersionObject(newExtension, newVersion,
            "attach:" + newExtension.getFile().getName());

        // Make sure the old version is installed.
        getExtensionTestUtils().install(new ExtensionId(extensionId, oldVersion));

        // Upgrade the extension.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId, newVersion).getExtension(0);
        assertEquals("remote-installed", extensionPane.getStatus());
        assertEquals("Version 1.3 is installed", extensionPane.getStatusMessage());
        extensionPane = extensionPane.upgrade();

        // Check the upgrade plan.
        List<DependencyPane> upgradePlan = extensionPane.openProgressSection().getJobPlan();
        assertEquals(1, upgradePlan.size());
        assertEquals(extensionId, upgradePlan.get(0).getName());
        assertEquals(newVersion, upgradePlan.get(0).getVersion());
        assertEquals("remote-installed", upgradePlan.get(0).getStatus());
        assertEquals("Version 1.3 is installed", upgradePlan.get(0).getStatusMessage());

        // Finish the upgrade and check the upgrade log.
        extensionPane = extensionPane.confirm();
        assertEquals("installed", extensionPane.getStatus());
        assertEquals("Installed", extensionPane.getStatusMessage());
        List<LogItemPane> log = extensionPane.openProgressSection().getJobLog();
        assertTrue(log.size() > 2);
        assertEquals("info", log.get(2).getLevel());
        assertEquals("Resolving extension [alice-xar-extension 2.1.4] on namespace [Home]", log.get(2).getMessage());
        assertEquals("info", log.get(log.size() - 1).getLevel());
        assertEquals(
            "Finished job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(log.size() - 1).getMessage());

        // Assert the changes.
        ViewPage viewPage = getUtil().gotoPage("ExtensionTest", "Alice");
        assertEquals("Alice Wiki Macro (upgraded)", viewPage.getDocumentTitle());
        assertTrue(viewPage.getContent().contains("Alice says hi guys!"));
    }

    /**
     * Tests how an extension is upgraded when there is a merge conflict.
     */
    @Test
    public void testUpgradeWithMergeConflict() throws Exception
    {
        // Setup the extension.
        String extensionId = "alice-xar-extension";
        String oldVersion = "1.3";
        String newVersion = "2.1.4";
        TestExtension oldExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, oldVersion), "xar");
        getRepositoryTestUtils().addExtension(oldExtension);
        TestExtension newExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, newVersion), "xar");
        getRepositoryTestUtils().attachFile(newExtension);
        getRepositoryTestUtils().addVersionObject(newExtension, newVersion,
            "attach:" + newExtension.getFile().getName());

        // Make sure the old version is installed.
        getExtensionTestUtils().install(new ExtensionId(extensionId, oldVersion));

        // Edit the installed version so that we have a merge conflict.
        Map<String, String> queryParameters = new HashMap<String, String>();
        queryParameters.put("title", "Alice Extension");
        queryParameters.put("content",
            "== Usage ==\n\n{{code language=\"none\"}}\n" + "{{alice/}}\n{{/code}}\n\n== Output ==\n\n{{alice/}}");
        queryParameters.put("XWiki.WikiMacroClass_0_code", "{{info}}Alice says hello!{{/info}}");
        getUtil().gotoPage("ExtensionTest", "Alice", "save", queryParameters);

        // Initiate the upgrade process.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        SearchResultsPane searchResults =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId, newVersion);
        ExtensionPane extensionPane = searchResults.getExtension(0);
        extensionPane = extensionPane.upgrade().confirm();

        // Check the merge conflict UI.
        assertEquals("loading", extensionPane.getStatus());
        assertNull(extensionPane.getStatusMessage());

        ProgressBarPane progressBar = extensionPane.getProgressBar();
        assertEquals(66, progressBar.getPercent());
        assertEquals("Conflict between [@@ -1,1 +1,1 @@] and [@@ -1,1 +1,1 @@]", progressBar.getMessage());

        ExtensionProgressPane progressPane = extensionPane.openProgressSection();
        WebElement jobLogLabel = progressPane.getJobLogLabel();
        assertEquals("INSTALL LOG".toLowerCase(), jobLogLabel.getText().toLowerCase());
        // The job log is collapsed when the job is waiting for user input so we need to expand it before asserting its
        // content (otherwise #getText() returns the empty string because the text is not visible).
        jobLogLabel.click();
        List<LogItemPane> upgradeLog = progressPane.getJobLog();
        LogItemPane lastLogItem = upgradeLog.get(upgradeLog.size() - 1);
        assertEquals("loading", lastLogItem.getLevel());
        assertEquals(progressBar.getMessage(), lastLogItem.getMessage());

        MergeConflictPane mergeConflictPane = progressPane.getMergeConflict();
        RawChanges changesPane = mergeConflictPane.getChanges();
        assertEquals(Arrays.asList("Page properties", "XWiki.WikiMacroClass[0]"), changesPane.getChangedEntities());

        EntityDiff pagePropertiesDiff = changesPane.getEntityDiff("Page properties");
        assertEquals(Arrays.asList("Parent", "Content"), pagePropertiesDiff.getPropertyNames());
        assertFalse(pagePropertiesDiff.getDiff("Content").isEmpty());

        EntityDiff macroDiff = changesPane.getEntityDiff("XWiki.WikiMacroClass[0]");
        assertEquals(Arrays.asList("Macro description"), macroDiff.getPropertyNames());
        assertEquals(
            Arrays.asList("@@ -1,1 +1,1 @@", "-<del>Test</del> macro.", "+<ins>A</ins> <ins>cool </ins>macro."),
            macroDiff.getDiff("Macro description"));

        mergeConflictPane.getFromVersionSelect().selectByVisibleText("Previous version");
        mergeConflictPane.getToVersionSelect().selectByVisibleText("Current version");
        mergeConflictPane = mergeConflictPane.clickShowChanges();

        changesPane = mergeConflictPane.getChanges();
        List<String> expectedDiff = new ArrayList<>();
        expectedDiff.add("@@ -1,1 +1,1 @@");
        expectedDiff.add("-= Usage =");
        expectedDiff.add("+=<ins>=</ins> Usage =<ins>=</ins>");
        expectedDiff.add("[Conflict Resolution]");
        expectedDiff.add("@@ -2,8 +2,8 @@");
        expectedDiff.add(" ");
        expectedDiff.add("-{{code}}");
        expectedDiff.add("+{{code<ins> language=\"none\"</ins>}}");
        expectedDiff.add(" {{alice/}}");
        expectedDiff.add(" {{/code}}");
        expectedDiff.add(" ");
        expectedDiff.add("-= <del>Res</del>u<del>l</del>t =");
        expectedDiff.add("+=<ins>=</ins> <ins>O</ins>ut<ins>put</ins> =<ins>=</ins>");
        expectedDiff.add(" ");
        expectedDiff.add(" {{alice/}}");
        assertEquals(expectedDiff, changesPane.getEntityDiff("Page properties").getDiff("Content"));
        assertEquals(1, changesPane.getDiffSummary().toggleObjectsDetails().getModifiedObjects().size());
        assertEquals(
            Arrays.asList("@@ -1,1 +1,1 @@", "-Alice says hello!",
                "+<ins>{{info}}</ins>Alice says hello!<ins>{{/info}}</ins>"),
            changesPane.getEntityDiff("XWiki.WikiMacroClass[0]").getDiff("Macro code"));

        // Finish the merge.
        mergeConflictPane.getVersionToKeepSelect().selectByValue("NEXT");
        // FIXME: We get the extension pane from the search results because it is reloaded when we compare the versions.
        extensionPane = searchResults.getExtension(0).confirm();

        assertEquals("installed", extensionPane.getStatus());
        assertNull(extensionPane.getProgressBar());
        upgradeLog = extensionPane.openProgressSection().getJobLog();
        lastLogItem = upgradeLog.get(upgradeLog.size() - 1);
        assertEquals("info", lastLogItem.getLevel());
        assertEquals(
            "Finished job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            lastLogItem.getMessage());

        // Check the merge result.
        ViewPage mergedPage = getUtil().gotoPage("ExtensionTest", "Alice");
        assertEquals("Alice Wiki Macro (upgraded)", mergedPage.getDocumentTitle());
    }

    /**
     * Tests how an extension is downgraded.
     */
    @Test
    public void testDowngrade() throws Exception
    {
        // Setup the extension.
        String extensionId = "alice-xar-extension";
        String oldVersion = "1.3";
        String newVersion = "2.1.4";
        TestExtension oldExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, oldVersion), "xar");
        getRepositoryTestUtils().addExtension(oldExtension);
        TestExtension newExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(extensionId, newVersion), "xar");
        getRepositoryTestUtils().attachFile(newExtension);
        getRepositoryTestUtils().addVersionObject(newExtension, newVersion,
            "attach:" + newExtension.getFile().getName());

        // Make sure the new version is installed.
        getExtensionTestUtils().install(new ExtensionId(extensionId, newVersion));

        // Downgrade the extension.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId, oldVersion).getExtension(0);
        assertEquals("remote-installed", extensionPane.getStatus());
        assertEquals("Version 2.1.4 is installed", extensionPane.getStatusMessage());
        extensionPane = extensionPane.downgrade();

        // Check the downgrade plan.
        List<DependencyPane> downgradePlan = extensionPane.openProgressSection().getJobPlan();
        assertEquals(1, downgradePlan.size());
        assertEquals(extensionId, downgradePlan.get(0).getName());
        assertEquals(oldVersion, downgradePlan.get(0).getVersion());
        assertEquals("remote-installed", downgradePlan.get(0).getStatus());
        assertEquals("Version 2.1.4 is installed", downgradePlan.get(0).getStatusMessage());

        // Finish the downgrade and check the downgrade log.
        // Using 20s for the timeout since 10s seems to not always be enough
        extensionPane = extensionPane.confirm(20);
        assertEquals("installed", extensionPane.getStatus());
        assertEquals("Installed", extensionPane.getStatusMessage());
        List<LogItemPane> log = extensionPane.openProgressSection().getJobLog();
        assertTrue(log.size() > 2);
        assertEquals("info", log.get(2).getLevel());
        assertEquals("Resolving extension [alice-xar-extension 1.3] on namespace [Home]", log.get(2).getMessage());
        assertEquals("info", log.get(log.size() - 1).getLevel());
        assertEquals(
            "Finished job of type [install] with identifier " + "[extension/action/alice-xar-extension/wiki:xwiki]",
            log.get(log.size() - 1).getMessage());

        // Assert the changes.
        ViewPage viewPage = getUtil().gotoPage("ExtensionTest", "Alice");
        assertEquals("Alice Macro", viewPage.getDocumentTitle());
        assertTrue(viewPage.getContent().contains("Alice says hello!"));
    }

    /**
     * Tests if a Java component script service is properly installed.
     */
    @Test
    public void testInstallScriptService() throws Exception
    {
        // Make sure the script service is not available before the extension is installed.
        ViewPage viewPage = getUtil().createPage(getTestClassName(), getTestMethodName(),
            "{{velocity}}$services.greeter.greet('world') "
                + "$services.greeter.greet('XWiki', 'default'){{/velocity}}",
            "");
        assertFalse(viewPage.getContent().contains("Hello world! Hello XWiki!"));

        // Setup the extension.
        ExtensionId extensionId = new ExtensionId("scriptServiceJarExtension", "4.2-milestone-1");
        TestExtension extension = getRepositoryTestUtils().getTestExtension(extensionId, "jar");
        getRepositoryTestUtils().addExtension(extension);

        // Search the extension and install it.
        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();
        ExtensionPane extensionPane =
            adminPage.getSearchBar().clickAdvancedSearch().search(extensionId).getExtension(0);
        extensionPane.install().confirm();

        // Check the result.
        assertEquals("Hello world! Hello XWiki!",
            getUtil().gotoPage(getTestClassName(), getTestMethodName()).getContent());
    }

    /**
     * Make sure supported extensions are properly filtered.
     */
    @Test
    public void testFilterSupportedd() throws Exception
    {
        // Add supported extension
        ExtensionId supportedExtensionId = new ExtensionId("alice-xar-extension", "1.3");
        TestExtension supportedExtension = getRepositoryTestUtils().getTestExtension(supportedExtensionId, "xar");
        DefaultExtensionSupporter supporter = new DefaultExtensionSupporter("Supporter", null);
        DefaultExtensionSupportPlan supportPlan =
            new DefaultExtensionSupportPlan(supporter, "Support Plan", null, true);
        supportedExtension.setSupportPlans(new DefaultExtensionSupportPlans(List.of(supportPlan)));
        getRepositoryTestUtils().addExtension(supportedExtension);

        // Add not supported extension
        ExtensionId notSupportedExtensionId = new ExtensionId("bob-xar-extension", "2.5-milestone-2");
        TestExtension notSupportedExtension = getRepositoryTestUtils().getTestExtension(notSupportedExtensionId, "xar");
        getRepositoryTestUtils().addExtension(notSupportedExtension);

        // Make sure everything is ready
        getRepositoryTestUtils().waitUntilReady();

        ExtensionAdministrationPage adminPage = ExtensionAdministrationPage.gotoPage();

        // Test direct search
        adminPage = adminPage.setIndexed(false);

        // Empty search
        SearchResultsPane searchResults = adminPage.getSearchResults();
        assertNotNull(searchResults.getExtension(supportedExtensionId));
        assertNull(searchResults.getExtension(notSupportedExtensionId));

        // Search among supported extensions
        SimpleSearchPane searchBar = adminPage.getSearchBar();
        searchResults = searchBar.search("alice-xar-extension");
        assertNotNull(searchResults.getExtension(supportedExtensionId));
        assertNull(searchResults.getExtension(notSupportedExtensionId));

        // Fallback on all extensions
        searchResults = searchBar.search("bob-xar-extension");
        assertNull(searchResults.getExtension(supportedExtensionId));
        assertNotNull(searchResults.getExtension(notSupportedExtensionId));
    }
}
