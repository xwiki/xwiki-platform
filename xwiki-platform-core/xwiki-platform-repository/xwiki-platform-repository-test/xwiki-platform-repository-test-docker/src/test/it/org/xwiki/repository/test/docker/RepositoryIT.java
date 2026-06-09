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
package org.xwiki.repository.test.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extensions;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.repository.test.RepositoryTestUtils;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.repository.test.TestExtension;
import org.xwiki.repository.test.po.ExtensionImportPage;
import org.xwiki.repository.test.po.ExtensionPage;
import org.xwiki.repository.test.po.ExtensionSupportPage;
import org.xwiki.repository.test.po.ExtensionSupportPlanPage;
import org.xwiki.repository.test.po.ExtensionSupporterPage;
import org.xwiki.repository.test.po.ExtensionsLiveTableElement;
import org.xwiki.repository.test.po.ExtensionsPage;
import org.xwiki.repository.test.po.RepositoryAdminPage;
import org.xwiki.repository.test.po.edit.ExtensionInlinePage;
import org.xwiki.repository.test.po.edit.ExtensionSupportPlanInlinePage;
import org.xwiki.repository.test.po.edit.ExtensionSupporterInlinePage;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Repository Test.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest(
    // Provides the XWikiExtensionRepositoryFactory-equivalent for the "maven" repository type used by the maven-test
    // repository. It must be in WEB-INF/lib at startup so that the maven-test repository (configured by
    // DynamicTestConfigurationExtension) can be registered.
    extraJARs = {
        "org.xwiki.commons:xwiki-commons-extension-repository-maven"
    }
)
class RepositoryIT
{
    private static final String USER_NAME = "Author";

    private static final String USER_PASSWORD = "password";

    private static final String IDPREFIX = "prefix-";

    private static RepositoryTestUtils repositoryTestUtils;

    private TestUtils setup;

    private TestExtension baseExtension;

    private ExtensionLicense baseLicense;

    private DefaultExtensionAuthor baseAuthor;

    private long sizeOfFile;

    @BeforeAll
    static void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.recacheSecretToken();
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        // Reuse the RepositoryUtils initialized (before XWiki started) by DynamicTestConfigurationExtension so that the
        // maven repository location and the generated test extension files match what was configured in
        // xwiki.properties.
        repositoryTestUtils = new RepositoryTestUtils(setup, DynamicTestConfigurationExtension.getRepositoryUtils(),
            new SolrTestUtils(setup));
    }

    @BeforeEach
    void setUp(TestUtils setup) throws Exception
    {
        this.setup = setup;

        // Make sure to have the proper token and admin credentials.
        setup.loginAsSuperAdmin();
        setup.recacheSecretToken();
        setup.setDefaultCredentials(TestUtils.SUPER_ADMIN_CREDENTIALS);

        // base extension informations

        this.baseExtension =
            repositoryTestUtils.getTestExtension(new ExtensionId(IDPREFIX + "macro-jar-extension", "1.0"), "jar");

        this.baseExtension.setName("Macro JAR extension");
        this.baseExtension.setDescription("extension description");
        this.baseExtension.setSummary("extension summary, **not bold**");

        this.baseLicense = new ExtensionLicense("Do What The Fuck You Want To Public License 2", null);
        this.baseExtension.addLicense(this.baseLicense);

        this.baseAuthor = new DefaultExtensionAuthor("User Name", setup.getURL("XWiki", USER_NAME));
        this.baseExtension.addAuthor(this.baseAuthor);

        this.baseExtension
            .addDependency(new DefaultExtensionDependency("dependencyid1", new DefaultVersionConstraint("1.0")));
        this.baseExtension
            .addDependency(new DefaultExtensionDependency("dependencyid2", new DefaultVersionConstraint("2.0")));

        this.sizeOfFile = FileUtils.sizeOf(this.baseExtension.getFile().getFile());

        repositoryTestUtils.deleteExtension(this.baseExtension);
    }

    @Test
    void validateAllFeatures() throws Exception
    {
        setUpXWiki();
        addSupportPlans();
        addExtension();
        ExtensionPage importedExtensionPage = importExtension();
        enableProxying(importedExtensionPage);
        validateSupport();
    }

    private void setUpXWiki()
    {
        // Set id prefix

        RepositoryAdminPage repositoryAdminPage = RepositoryAdminPage.gotoPage();

        repositoryAdminPage.setDefaultIdPrefix(IDPREFIX);
        repositoryAdminPage.clickUpdateButton();

        this.setup.createUserAndLogin(USER_NAME, USER_PASSWORD, "first_name", "User", "last_name", "Name");
    }

    private void addSupportPlans()
    {
        ExtensionSupportPage supportPage = ExtensionSupportPage.gotoPage();

        supportPage.setSupporterName("Supporter 1");
        ExtensionSupporterInlinePage supporterEditPage = supportPage.clickRegister();

        supporterEditPage.setActive(true);
        ExtensionSupporterPage supporterViewPage = supporterEditPage.clickSaveAndView();
        String supporterURL = supporterViewPage.getPageURL();

        addSupportPlan(supporterViewPage, "Support Plan 1.1", true);
        supporterViewPage = ExtensionSupporterPage.gotoPage(supporterURL);
        addSupportPlan(supporterViewPage, "Support Plan 1.2", true);
    }

    private ExtensionSupportPlanPage addSupportPlan(ExtensionSupporterPage supporterViewPage, String name,
        boolean active)
    {
        supporterViewPage.setSupportPlanName(name);
        ExtensionSupportPlanInlinePage supportPlanEditPage = supporterViewPage.clickAdd();

        supportPlanEditPage.setActive(active);
        return supportPlanEditPage.clickSaveAndView();
    }

    private void addExtension() throws Exception
    {
        // Create extension

        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionInlinePage extensionEdit = extensionsPage.contributeExtension(this.baseExtension.getName());

        assertEquals(this.baseExtension.getName(), extensionEdit.getName());

        extensionEdit.setDescription(this.baseExtension.getDescription());
        extensionEdit.setInstallation("extension installation");
        extensionEdit.setLicenseName(this.baseLicense.getName());
        extensionEdit.setSource("http://source");
        extensionEdit.setSummary(this.baseExtension.getSummary());
        extensionEdit.setType(this.baseExtension.getType());

        ExtensionPage extensionPage = extensionEdit.clickSaveAndView();

        // Test summary
        this.setup.getDriver()
            .findElementsWithoutWaiting(By.xpath("//tt[text()=\"" + this.baseExtension.getSummary() + "\"]"));

        assertFalse(extensionPage.isValidExtension());

        // Add versions
        // TODO: add XR UI to manipulate versions
        repositoryTestUtils.addVersionObject(this.baseExtension);
        repositoryTestUtils.addVersionObject(this.baseExtension, "10.0", this.setup.getAttachmentURL("Extension",
            this.baseExtension.getName(), this.baseExtension.getFile().getName()));
        repositoryTestUtils.addVersionObject(this.baseExtension, "2.0",
            "attach:" + this.baseExtension.getFile().getName());

        // Add dependencies
        // TODO: add XR UI to manipulate dependencies
        repositoryTestUtils.addDependencies(this.baseExtension, "10.0");

        // Add attachment
        repositoryTestUtils.attachFile(this.baseExtension);

        // Wait until all asynchronous tasks are done
        repositoryTestUtils.waitUntilReady();

        // Check livetable

        extensionsPage = ExtensionsPage.gotoPage();

        ExtensionsLiveTableElement livetable = extensionsPage.getLiveTable();

        livetable.filterName(this.baseExtension.getName());

        extensionPage = livetable.clickExtensionName(this.baseExtension.getName());

        // Validate extension state

        assertTrue(extensionPage.isValidExtension());

        // //////////////////////////////////////////
        // Validate REST
        // //////////////////////////////////////////

        // //////////////////////////////////////////
        // 1.0
        // //////////////////////////////////////////

        // Resolve

        ExtensionVersion extension =
            this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "1.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("1.0", extension.getVersion());

        assertSameURL(this.setup.getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        assertEquals(this.sizeOfFile, this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "1.0").length);

        // //////////////////////////////////////////
        // 2.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "2.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("2.0", extension.getVersion());

        assertSameURL(this.setup.getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        assertEquals(this.sizeOfFile, this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "2.0").length);

        // //////////////////////////////////////////
        // 10.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "10.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        assertSameURL(this.setup.getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        ExtensionDependency dependency1 = extension.getDependencies().get(0);
        assertEquals("dependencyid1", dependency1.getId());
        assertEquals("1.0", dependency1.getConstraint());
        ExtensionDependency dependency2 = extension.getDependencies().get(1);
        assertEquals("dependencyid2", dependency2.getId());
        assertEquals("2.0", dependency2.getConstraint());

        // File

        assertEquals(this.sizeOfFile, this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), this.baseExtension.getId().getVersion().getValue()).length);

        // //////////////////////////////////////////
        // Extensions
        // //////////////////////////////////////////

        Extensions extensions = this.setup.rest().getResource(Resources.EXTENSIONS, Map.of());

        assertEquals(1, extensions.getTotalHits());
        assertEquals(this.baseExtension.getId().getId(), extensions.getExtensionSummaries().get(0).getId());
        assertEquals(this.baseExtension.getType(), extensions.getExtensionSummaries().get(0).getType());
        assertEquals(this.baseExtension.getName(), extensions.getExtensionSummaries().get(0).getName());

        // //////////////////////////////////////////
        // Search
        // //////////////////////////////////////////

        // Empty search
        extension = searchExtension(this.baseExtension.getId().getId());

        if (extension == null) {
            fail("Could not find extension [" + this.baseExtension.getId().getId() + "]");
        }

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        // TODO: add support for dependencies in XR search

        // Search pattern

        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"macro"});

        ExtensionsSearchResult result = this.setup.rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(1, result.getTotalHits());
        assertEquals(0, result.getOffset());
        extension = result.getExtensions().get(0);

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        // Wrong search pattern

        queryParams.clear();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"notexisting"});

        result = this.setup.rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(0, result.getTotalHits());
        assertEquals(0, result.getOffset());
        assertEquals(0, result.getExtensions().size());

        // Search limit offset

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_START, new Object[] {1});

        result = this.setup.rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(1, result.getOffset());
        assertEquals(result.getTotalHits() - 1, result.getExtensions().size());

        // Search limit nb

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_NUMBER, new Object[] {0});

        result = this.setup.rest().getResource(Resources.SEARCH, queryParams);

        assertTrue(result.getTotalHits() >= 1);
        assertEquals(0, result.getOffset());
        assertEquals(0, result.getExtensions().size());
    }

    private ExtensionVersion searchExtension(String id) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        ExtensionsSearchResult result = this.setup.rest().getResource(Resources.SEARCH, queryParams);

        assertTrue(result.getTotalHits() >= 0);
        assertEquals(0, result.getOffset());

        ExtensionVersion extension = null;
        for (ExtensionVersion extensionVersion : result.getExtensions()) {
            if (extensionVersion.getId().equals(id)) {
                extension = extensionVersion;
                break;
            }
        }

        return extension;
    }

    private ExtensionPage importExtension() throws Exception
    {
        // Make sure to clean the extension if it's already there
        this.setup.rest().delete(new LocalDocumentReference(List.of("Extension", "name"), "WebHome"));

        // Use an account without script right
        this.setup.login(USER_NAME, USER_PASSWORD);

        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionImportPage importPage = extensionsPage.clickImport();

        importPage.setExtensionId("maven:extension");
        importPage.setSourceRepository("maven-test");
        ExtensionPage extensionPage = importPage.clickImport();

        // Check

        assertEquals("1.1", extensionPage.getMetaDataValue("version"));
        assertTrue(extensionPage.isValidExtension());

        testRestAccessToImportedExtension(false);

        // Import again

        extensionPage = extensionPage.updateExtension();

        assertEquals("1.1", extensionPage.getMetaDataValue("version"));

        return extensionPage;
    }

    private void testRestAccessToImportedExtension(boolean proxy) throws Exception
    {
        // 2.0

        TestExtension emptyExtension =
            repositoryTestUtils.getTestExtension(new ExtensionId("emptyjar", "1.0"), "jar");

        long fileSize = FileUtils.sizeOf(emptyExtension.getFile().getFile());

        ExtensionVersion extension =
            this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "2.0");

        assertEquals("maven:extension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("2.0", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary2", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertSameURL(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        assertEquals("2.0", extension.getExtensionFeatures().get(0).getVersion());
        assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());
        assertEquals("org.xwiki.rendering.macro.Macro/mymacro1\norg.xwiki.rendering.macro.Macro/mymacro2",
            getProperty("xwiki.extension.components", extension));
        assertEquals(1, extension.getDependencies().size());
        assertEquals("maven:dependency2", extension.getDependencies().get(0).getId());
        assertEquals("2.0", extension.getDependencies().get(0).getConstraint());
        assertEquals(1, extension.getDependencies().get(0).getExclusions().size());
        assertEquals("groupid:artifactid", extension.getDependencies().get(0).getExclusions().get(0));

        assertEquals(fileSize,
            this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "2.0").length);

        // 1.0

        extension = this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "1.0");

        assertEquals("maven:extension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("1.0", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals("Previous Name", extension.getAuthors().get(0).getName());
        assertNull(extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        assertEquals("1.0", extension.getExtensionFeatures().get(0).getVersion());
        assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());
        // TODO: remove the if when https://jira.xwiki.org/browse/XWIKI-23998 is fixed
        if (!proxy) {
            assertEquals(1, extension.getDependencies().size());
            assertEquals("maven:dependency1", extension.getDependencies().get(0).getId());
            assertEquals("1.0", extension.getDependencies().get(0).getConstraint());
            assertEquals(0, extension.getDependencies().get(0).getExclusions().size());
        }

        assertEquals(FileUtils.sizeOf(emptyExtension.getFile().getFile()),
            this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "1.0").length);

        // 0.9

        extension = this.setup.rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "0.9");

        assertEquals("maven:oldextension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("0.9", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary2", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals("Old Name", extension.getAuthors().get(0).getName());
        assertNull(extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList(), extension.getFeatures());
        assertEquals(Arrays.asList(), extension.getExtensionFeatures());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());
        // TODO: remove the if when https://jira.xwiki.org/browse/XWIKI-23998 is fixed
        if (!proxy) {
            assertEquals(1, extension.getDependencies().size());
            assertEquals("oldmaven:olddependency", extension.getDependencies().get(0).getId());
            assertEquals("oldversion", extension.getDependencies().get(0).getConstraint());
            assertEquals(0, extension.getDependencies().get(0).getExclusions().size());
        }

        assertEquals(fileSize,
            this.setup.rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "0.9").length);

    }

    private void enableProxying(ExtensionPage extensionPage) throws Exception
    {
        String importedExtensionName = "name";

        LocalDocumentReference extensionPageReference =
            new LocalDocumentReference(List.of("Extension", importedExtensionName), "WebHome");

        // assert that this test is going to make sense at all
        assertTrue(getNumberOfExtensionVersionsPages(extensionPageReference) > 1);

        // indicate that the history of the extension should be proxied
        this.setup.updateObject(extensionPageReference, XWikiRepositoryModel.EXTENSIONPROXY_CLASSNAME, 0, "proxyLevel",
            "history");

        // refresh extension
        extensionPage.updateExtension();

        // assert that the version to be proxied are now absent
        assertEquals(1, getNumberOfExtensionVersionsPages(extensionPageReference));

        // Remember the page version
        Page restPage = this.setup.rest().get(extensionPageReference);
        String extensionPageVersion = restPage.getVersion();

        // in rest access nothing should change after enabling proxy
        testRestAccessToImportedExtension(true);

        // Make sure the REST access does not modify the document
        restPage = this.setup.rest().get(extensionPageReference);
        assertEquals(extensionPageVersion, restPage.getVersion());
    }

    private int getNumberOfExtensionVersionsPages(LocalDocumentReference extensionPageReference) throws Exception
    {
        EntityReference versionReference = new EntityReference(XWikiRepositoryModel.EXTENSIONVERSIONS_SPACENAME,
            EntityType.SPACE, extensionPageReference.getParent());
        String result = this.setup.executeWikiPlain(
            "{{velocity}}$services.query.xwql(\"select space.reference from Space space where space.parent='"
                + this.setup.serializeLocalReference(versionReference) + "'\").execute().size(){{/velocity}}",
            Syntax.XWIKI_2_1);

        return Integer.parseInt(result);
    }

    private void validateSupport() throws Exception
    {
        // At this stage we assume that we have 2 Extensions in the Repository app and we'll make one of them
        // Supported.
        // Then we'll turn on Recommendations and verify that the home page lists only the Supported one.
        // Then we'll click on the Browse button to list all extensions and verify we have 2 in the list.
        // Then we'll click again on the Browse button to list only Supported extensions and verify we
        // have 1 in the list.
        // Last we'll navigate to an extension and verify that when clicking the breadcrumb we're still on the
        // Supported list.

        // Add a support plan to an extension
        ExtensionPage.gotoPage("Macro JAR extension").edit();
        ExtensionInlinePage extentionInlinePage = new ExtensionInlinePage();
        extentionInlinePage.selectSupportPlan("Supporter1", "SupportPlan11", true);
        extentionInlinePage.clickSaveAndContinue();

        // Turn on Recommendation/Support filtering. There's no Admin UI yet for this.
        this.setup.loginAsSuperAdmin();
        this.setup.updateObject("ExtensionCode", "RepositoryConfig", "ExtensionCode.RepositoryConfigClass", 0,
            "useRecommendations", 1);
        this.setup.login(USER_NAME, USER_PASSWORD);

        // Verify that the home page now lists only the Supported extension.
        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();
        ExtensionsLiveTableElement livetable = extensionsPage.getLiveTable();
        assertEquals(1, livetable.getRowCount());

        // Click on Browse button to list All Extensions
        extensionsPage = extensionsPage.clickBrowse();

        // Verify that the home page now lists all extensions.
        livetable = extensionsPage.getLiveTable();
        assertEquals(2, livetable.getRowCount());

        // Click on Browse button to list Supported Extensions
        extensionsPage = extensionsPage.clickBrowse();

        // Verify that the home page now lists only Supported extensions.
        livetable = extensionsPage.getLiveTable();
        assertEquals(1, livetable.getRowCount());

        // Navigate to the extension and click the breadcrumb and verify that we're listing only Supported extensions
        ExtensionPage extensionPage = livetable.clickExtensionName("Macro JAR extension");
        extensionPage.clickBreadcrumbLink("Extensions");
        extensionsPage = new ExtensionsPage();
        livetable = extensionsPage.getLiveTable();
        assertEquals(1, livetable.getRowCount());
    }

    /**
     * Asserts that two URLs are equal, ignoring the scheme and host parts. In the Docker test topology the in-container
     * browser reaches the host XWiki through {@code host.testcontainers.internal} (so {@link TestUtils#getURL} builds
     * URLs with that host) while REST responses are generated server-side using {@code localhost}. Only the host part
     * differs, so we compare the path part only.
     */
    private static void assertSameURL(String expected, String actual)
    {
        assertEquals(stripHost(expected), stripHost(actual),
            String.format("expected <%s> but was <%s>", expected, actual));
    }

    private static String stripHost(String url)
    {
        return url == null ? null : url.replaceFirst("^https?://[^/]+", "");
    }

    private String getProperty(String key, AbstractExtension extension)
    {
        for (Property property : extension.getProperties()) {
            if (property.getKey().equals(key)) {
                return property.getStringValue();
            }
        }

        return null;
    }
}
