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
import org.junit.Assert;
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
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.repository.test.TestExtension;
import org.xwiki.repository.test.po.ExtensionImportPage;
import org.xwiki.repository.test.po.ExtensionPage;
import org.xwiki.repository.test.po.ExtensionsLiveTableElement;
import org.xwiki.repository.test.po.ExtensionsPage;
import org.xwiki.repository.test.po.RepositoryAdminPage;
import org.xwiki.repository.test.po.editor.ExtensionInlinePage;
import org.xwiki.repository.test.ui.AbstractExtensionAdminAuthenticatedIT;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

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
        addExtension();
        ExtensionPage importedExtensionPage = importExtension();
        enableProxying(importedExtensionPage);
        validateRecommendations();
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

    private void addExtension() throws Exception
    {
        // Create extension

        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionInlinePage extensionInline = extensionsPage.contributeExtension(this.baseExtension.getName());

        Assert.assertEquals(this.baseExtension.getName(), extensionInline.getName());

        extensionInline.setDescription(this.baseExtension.getDescription());
        extensionInline.setInstallation("extension installation");
        extensionInline.setLicenseName(this.baseLicense.getName());
        extensionInline.setSource("http://source");
        extensionInline.setSummary(this.baseExtension.getSummary());
        extensionInline.setType(this.baseExtension.getType());

        ExtensionPage extensionPage = extensionInline.clickSaveAndView();

        // Test summary
        getDriver().findElementsWithoutWaiting(By.xpath("//tt[text()=\"" + this.baseExtension.getSummary() + "\"]"));

        Assert.assertFalse(extensionPage.isValidExtension());

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

        Assert.assertTrue(extensionPage.isValidExtension());

        // //////////////////////////////////////////
        // Validate REST
        // //////////////////////////////////////////

        // //////////////////////////////////////////
        // 1.0
        // //////////////////////////////////////////

        // Resolve

        ExtensionVersion extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "1.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("1.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        Assert.assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "1.0").length);

        // //////////////////////////////////////////
        // 2.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "2.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("2.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        // File

        Assert.assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), "2.0").length);

        // //////////////////////////////////////////
        // 10.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "10.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL(Arrays.asList("Extension", this.baseExtension.getName()), ""),
            extension.getWebsite());

        ExtensionDependency dependency1 = extension.getDependencies().get(0);
        Assert.assertEquals("dependencyid1", dependency1.getId());
        Assert.assertEquals("1.0", dependency1.getConstraint());
        ExtensionDependency dependency2 = extension.getDependencies().get(1);
        Assert.assertEquals("dependencyid2", dependency2.getId());
        Assert.assertEquals("2.0", dependency2.getConstraint());

        // File

        Assert.assertEquals(this.sizeOfFile, getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null,
            this.baseExtension.getId().getId(), this.baseExtension.getId().getVersion().getValue()).length);

        // //////////////////////////////////////////
        // Search
        // //////////////////////////////////////////

        // Empty search
        extension = searchExtension(this.baseExtension.getId().getId());

        if (extension == null) {
            Assert.fail("Could not find extension [" + this.baseExtension.getId().getId() + "]");
        }

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        // TODO: add support for dependencies in XR search

        // Search pattern

        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] { "macro" });

        ExtensionsSearchResult result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(1, result.getTotalHits());
        Assert.assertEquals(0, result.getOffset());
        extension = result.getExtensions().get(0);

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        // Wrong search pattern

        queryParams.clear();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] { "notexisting" });

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(0, result.getTotalHits());
        Assert.assertEquals(0, result.getOffset());
        Assert.assertEquals(0, result.getExtensions().size());

        // Search limit offset

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_START, new Object[] { 1 });

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(1, result.getOffset());
        Assert.assertEquals(result.getTotalHits() - 1, result.getExtensions().size());

        // Search limit nb

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_NUMBER, new Object[] { 0 });

        result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        Assert.assertTrue(result.getTotalHits() >= 1);
        Assert.assertEquals(0, result.getOffset());
        Assert.assertEquals(0, result.getExtensions().size());
    }

    private ExtensionVersion searchExtension(String id) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        ExtensionsSearchResult result = getUtil().rest().getResource(Resources.SEARCH, queryParams);

        Assert.assertTrue(result.getTotalHits() >= 0);
        Assert.assertEquals(0, result.getOffset());

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
        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionImportPage importPage = extensionsPage.clickImport();

        importPage.setExtensionId("maven:extension");
        importPage.setSourceRepository("maven-test");
        ExtensionPage extensionPage = importPage.clickImport();

        // Check

        Assert.assertEquals("1.1", extensionPage.getMetaDataValue("version"));
        Assert.assertTrue(extensionPage.isValidExtension());

        testRestAccessToImportedExtension();

        // Import again

        extensionPage = extensionPage.updateExtension();

        Assert.assertEquals("1.1", extensionPage.getMetaDataValue("version"));

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

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("2.0", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        Assert.assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        Assert.assertEquals("2.0", extension.getExtensionFeatures().get(0).getVersion());
        Assert.assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        Assert.assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());
        Assert.assertEquals("org.xwiki.rendering.macro.Macro/mymacro1\norg.xwiki.rendering.macro.Macro/mymacro2",
            getProperty("xwiki.extension.components", extension));

        Assert.assertEquals(fileSize,
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "2.0").length);

        // 1.0

        extension = getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "1.0");

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("1.0", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList("maven:oldextension", "maven:oldversionnedextension"),
            extension.getFeatures());
        Assert.assertEquals("maven:oldextension", extension.getExtensionFeatures().get(0).getId());
        Assert.assertEquals("1.0", extension.getExtensionFeatures().get(0).getVersion());
        Assert.assertEquals("maven:oldversionnedextension", extension.getExtensionFeatures().get(1).getId());
        Assert.assertEquals("10.0", extension.getExtensionFeatures().get(1).getVersion());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        Assert.assertEquals(FileUtils.sizeOf(emptyExtension.getFile().getFile()),
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "1.0").length);

        // 0.9

        extension = getUtil().rest().getResource(Resources.EXTENSION_VERSION, null, "maven:extension", "0.9");

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("0.9", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList(), extension.getFeatures());
        Assert.assertEquals(Arrays.asList(), extension.getExtensionFeatures());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        Assert.assertEquals(fileSize,
            getUtil().rest().getBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "0.9").length);

    }

    private void enableProxying(ExtensionPage extensionPage) throws Exception
    {
        String importedExtensionName = "name";

        LocalDocumentReference extensionPageReference =
            new LocalDocumentReference(List.of("Extension", importedExtensionName), "WebHome");

        // assert that this test is going to make sense at all
        Assert.assertTrue(getNumberOfExtensionVersionsObjects(IMPORTED_EXTENSION_NAME) > 1);
        Assert.assertTrue(getNumberOfExtensionVersionsDependenciesObjects(IMPORTED_EXTENSION_NAME) > 1);

        // indicate that the history of the extension should be proxied
        getUtil().updateObject(extensionPageReference, XWikiRepositoryModel.EXTENSIONPROXY_CLASSNAME, 0, "proxyLevel",
            "history");

        // refresh extension
        extensionPage.updateExtension();

        // assert that the object to be proxied are now absent
        Assert.assertEquals(1, getNumberOfExtensionVersionsObjects(IMPORTED_EXTENSION_NAME));
        Assert.assertEquals(1, getNumberOfExtensionVersionsDependenciesObjects(IMPORTED_EXTENSION_NAME));

        // Remember the page version
        Page restPage = getUtil().rest().get(extensionPageReference);
        String extensionPageVersion = restPage.getVersion();

        // in rest access nothing should change after enabling proxy
        testRestAccessToImportedExtension();

        // Make sure the REST access does not modify the document
        restPage = getUtil().rest().get(extensionPageReference);
        Assert.assertEquals(extensionPageVersion, restPage.getVersion());
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

    private void validateRecommendations() throws Exception
    {
        // At this stage we assume that we have 2 Extensions in the Repository app and we'll make one of them
        // Recommended.
        // Then we'll turn on Recommendations and verify that the home page lists only the Recommended one.
        // Then we'll click on the Browse button to list all extensions and verify we have 2 in the list.
        // Then we'll click again on the Browse button to list only Recommended extensions and verify we have 1
        // in the list.
        // Last we'll navigate to an extension and verify that when clicking the breadcrumb we're still on the
        // Recommended list.

        // Note: We don't yet offer a UI for changing the Recommended flag for an Extension.
        getUtil().updateObject(Arrays.asList("Extension", "Macro JAR extension"), "WebHome",
            "ExtensionCode.ExtensionClass", 0, "recommended", 1);

        // Turn on Recommendation. There's also no Admin UI yet for this.
        getUtil().updateObject("ExtensionCode", "RepositoryConfig", "ExtensionCode.RepositoryConfigClass", 0,
            "useRecommendations", 1);

        // Verify that the home page now lists only the Recommended extension.
        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();
        ExtensionsLiveTableElement livetable = extensionsPage.getLiveTable();
        Assert.assertEquals(1, livetable.getRowCount());

        // Click on Browse button to list All Extensions
        extensionsPage = extensionsPage.clickBrowse();

        // Verify that the home page now lists all extensions.
        livetable = extensionsPage.getLiveTable();
        Assert.assertEquals(2, livetable.getRowCount());

        // Click on Browse button to list Recommended Extensions
        extensionsPage = extensionsPage.clickBrowse();

        // Verify that the home page now lists only Recommended extensions.
        livetable = extensionsPage.getLiveTable();
        Assert.assertEquals(1, livetable.getRowCount());

        // Navigate to the extension and click the breadcrumb and verify that we're listing only Recommended extensions
        ExtensionPage extensionPage = livetable.clickExtensionName("Macro JAR extension");
        extensionPage.clickBreadcrumbLink("Extensions");
        extensionsPage = new ExtensionsPage();
        livetable = extensionsPage.getLiveTable();
        Assert.assertEquals(1, livetable.getRowCount());
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
