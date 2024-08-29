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
package org.xwiki.repository.test.ui.repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.repository.xwiki.model.jaxb.AbstractExtension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.XWikiRepositoryModel;
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
import org.xwiki.repository.test.ui.AbstractExtensionAdminAuthenticatedIT;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Repository Test.
 * 
 * @version $Id$
 */
public class RepositoryIT extends AbstractExtensionAdminAuthenticatedIT
{
    public static final UsernamePasswordCredentials USER_CREDENTIALS =
        new UsernamePasswordCredentials("Author", "password");

    private static final String IDPREFIX = "prefix-";

    private TestExtension baseExtension;

    private ExtensionLicense baseLicense;

    private DefaultExtensionAuthor baseAuthor;

    private long sizeOfFile;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // base extension informations

        this.baseExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId(IDPREFIX + "macro-jar-extension", "1.0"), "jar");

        this.baseExtension.setName("Macro JAR extension");
        this.baseExtension.setDescription("extension description");
        this.baseExtension.setSummary("extension summary, **not bold**");

        this.baseLicense = new ExtensionLicense("Do What The Fuck You Want To Public License 2", null);
        this.baseExtension.addLicense(this.baseLicense);

        this.baseAuthor =
            new DefaultExtensionAuthor("User Name", getUtil().getURL("XWiki", USER_CREDENTIALS.getUserName()));
        this.baseExtension.addAuthor(this.baseAuthor);

        this.baseExtension
            .addDependency(new DefaultExtensionDependency("dependencyid1", new DefaultVersionConstraint("1.0")));
        this.baseExtension
            .addDependency(new DefaultExtensionDependency("dependencyid2", new DefaultVersionConstraint("2.0")));

        this.sizeOfFile = FileUtils.sizeOf(this.baseExtension.getFile().getFile());

        getRepositoryTestUtils().deleteExtension(baseExtension);
    }

    @Test
    public void validateAllFeatures() throws Exception
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

        getUtil().createUserAndLogin(USER_CREDENTIALS.getUserName(), USER_CREDENTIALS.getPassword(), "first_name",
            "User", "last_name", "Name");

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
        getDriver().findElementsWithoutWaiting(By.xpath("//tt[text()=\"" + this.baseExtension.getSummary() + "\"]"));

        assertFalse(extensionPage.isValidExtension());

        // Add versions
        // TODO: add XR UI to manipulate versions
        getRepositoryTestUtils().addVersionObject(this.baseExtension);
        getRepositoryTestUtils().addVersionObject(this.baseExtension, "10.0", getUtil().getAttachmentURL("Extension",
            this.baseExtension.getName(), this.baseExtension.getFile().getName()));
        getRepositoryTestUtils().addVersionObject(this.baseExtension, "2.0",
            "attach:" + this.baseExtension.getFile().getName());

        // Add dependencies
        // TODO: add XR UI to manipulate dependencies
        getRepositoryTestUtils().addDependencies(this.baseExtension, "10.0");

        // Add attachment
        getRepositoryTestUtils().attachFile(this.baseExtension);

        // Wait until all asynchronous tasks are done
        getRepositoryTestUtils().waitUntilReady();

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
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "1.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("1.0", extension.getVersion());

        assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "1.0").length);

        // //////////////////////////////////////////
        // 2.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "2.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("2.0", extension.getVersion());

        assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "2.0").length);

        // //////////////////////////////////////////
        // 10.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "10.0");

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        ExtensionDependency dependency1 = extension.getDependencies().get(0);
        assertEquals("dependencyid1", dependency1.getId());
        assertEquals("1.0", dependency1.getConstraint());
        ExtensionDependency dependency2 = extension.getDependencies().get(1);
        assertEquals("dependencyid2", dependency2.getId());
        assertEquals("2.0", dependency2.getConstraint());

        // File

        assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), this.baseExtension.getId().getVersion().getValue()).length);

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
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        // TODO: add support for dependencies in XR search

        // Search pattern

        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"macro"});

        ExtensionsSearchResult result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(1, result.getTotalHits());
        assertEquals(0, result.getOffset());
        extension = result.getExtensions().get(0);

        assertEquals(this.baseExtension.getId().getId(), extension.getId());
        assertEquals(this.baseExtension.getType(), extension.getType());
        assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals("10.0", extension.getVersion());

        // Wrong search pattern

        queryParams.clear();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"notexisting"});

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(0, result.getTotalHits());
        assertEquals(0, result.getOffset());
        assertEquals(0, result.getExtensions().size());

        // Search limit offset

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_START, new Object[] {1});

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        assertEquals(1, result.getOffset());
        assertEquals(result.getTotalHits() - 1, result.getExtensions().size());

        // Search limit nb

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_NUMBER, new Object[] {0});

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        assertTrue(result.getTotalHits() >= 1);
        assertEquals(0, result.getOffset());
        assertEquals(0, result.getExtensions().size());
    }

    private ExtensionVersion searchExtension(String id) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        ExtensionsSearchResult result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

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
        getUtil().rest().delete(new LocalDocumentReference(List.of("Extension", "name"), "WebHome"));

        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionImportPage importPage = extensionsPage.clickImport();

        importPage.setExtensionId("maven:extension");
        importPage.setSourceRepository("maven-test");
        ExtensionPage extensionPage = importPage.clickImport();

        // Check

        assertEquals("1.1", extensionPage.getMetaDataValue("version"));
        assertTrue(extensionPage.isValidExtension());

        testRestAccessToImportedExtension();

        // Import again

        extensionPage = extensionPage.updateExtension();

        assertEquals("1.1", extensionPage.getMetaDataValue("version"));

        return extensionPage;
    }

    private void testRestAccessToImportedExtension() throws Exception
    {
        // 2.0

        TestExtension emptyExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId("emptyjar", "1.0"), "jar");

        long fileSize = FileUtils.sizeOf(emptyExtension.getFile().getFile());

        ExtensionVersion extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "2.0");

        assertEquals("maven:extension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("2.0", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary2", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        assertEquals("2.0", extension.getExtensionFeatures().get(0).getVersion());
        assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());
        assertEquals("org.xwiki.rendering.macro.Macro/mymacro1\norg.xwiki.rendering.macro.Macro/mymacro2",
            getProperty("xwiki.extension.components", extension));

        assertEquals(fileSize,
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "2.0").length);

        // 1.0

        extension = getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "1.0");

        assertEquals("maven:extension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("1.0", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary2", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        assertEquals("1.0", extension.getExtensionFeatures().get(0).getVersion());
        assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        assertEquals(FileUtils.sizeOf(emptyExtension.getFile().getFile()),
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "1.0").length);

        // 0.9

        extension = getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "0.9");

        assertEquals("maven:extension", extension.getId());
        assertEquals("jar", extension.getType());
        assertEquals("0.9", extension.getVersion());
        assertEquals("name", extension.getName());
        assertEquals("summary2", extension.getSummary());
        assertEquals("summary2\n      some more details", extension.getDescription());
        assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        assertEquals(Arrays.asList(), extension.getFeatures());
        assertEquals(Arrays.asList(), extension.getExtensionFeatures());
        assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        assertEquals(fileSize,
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "0.9").length);

    }

    private void enableProxying(ExtensionPage extensionPage) throws Exception
    {

        String IMPORTED_EXTENSION_NAME = "name";
        // assert that this test is going to make sense at all
        assertTrue(getNumberOfExtensionVersionsObjects(IMPORTED_EXTENSION_NAME) > 1);
        assertTrue(getNumberOfExtensionVersionsDependenciesObjects(IMPORTED_EXTENSION_NAME) > 1);

        // set higher proxy level: "version" - in previously imported extension
        getUtil().updateObject(Arrays.asList("Extension", IMPORTED_EXTENSION_NAME), "WebHome",
            XWikiRepositoryModel.EXTENSIONPROXY_CLASSNAME, 0, "proxyLevel", "history");

        // refresh extension
        extensionPage.updateExtension();

        // assert that the object to be proxied are now absent
        assertEquals(1, getNumberOfExtensionVersionsObjects(IMPORTED_EXTENSION_NAME));
        assertEquals(1, getNumberOfExtensionVersionsDependenciesObjects(IMPORTED_EXTENSION_NAME));

        // in rest access nothing should change after enabling proxy
        testRestAccessToImportedExtension();
    }

    private int getNumberOfExtensionVersionsObjects(String extensionName)
    {
        ObjectEditPage objectEditPage = goToObjectEditPage(extensionName);
        List<ObjectEditPane> versionObjects =
            objectEditPage.getObjectsOfClass(XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME);
        return versionObjects.size();
    }

    private int getNumberOfExtensionVersionsDependenciesObjects(String extensionName)
    {
        ObjectEditPage objectEditPage = goToObjectEditPage(extensionName);
        List<ObjectEditPane> dependenciesObjects =
            objectEditPage.getObjectsOfClass(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSNAME);
        return dependenciesObjects.size();
    }

    private ObjectEditPage goToObjectEditPage(String extensionName)
    {
        return getRepositoryTestUtils().gotoExtensionObjectsEditPage(extensionName);
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
        getUtil().loginAsSuperAdmin();
        getUtil().updateObject("ExtensionCode", "RepositoryConfig", "ExtensionCode.RepositoryConfigClass", 0,
            "useRecommendations", 1);
        getUtil().login(USER_CREDENTIALS.getUserName(), USER_CREDENTIALS.getPassword());

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
