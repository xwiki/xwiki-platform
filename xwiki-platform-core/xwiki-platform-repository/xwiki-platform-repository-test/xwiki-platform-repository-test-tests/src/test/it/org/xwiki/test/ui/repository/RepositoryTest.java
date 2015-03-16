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
package org.xwiki.test.ui.repository;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
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
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionDependency;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.repository.Resources;
import org.xwiki.repository.test.TestExtension;
import org.xwiki.repository.test.po.ExtensionImportPage;
import org.xwiki.repository.test.po.ExtensionPage;
import org.xwiki.repository.test.po.ExtensionsLiveTableElement;
import org.xwiki.repository.test.po.ExtensionsPage;
import org.xwiki.repository.test.po.RepositoryAdminPage;
import org.xwiki.repository.test.po.editor.ExtensionInlinePage;
import org.xwiki.test.ui.AbstractExtensionAdminAuthenticatedTest;

/**
 * Repository Test.
 * 
 * @version $Id$
 */
public class RepositoryTest extends AbstractExtensionAdminAuthenticatedTest
{
    public static final UsernamePasswordCredentials USER_CREDENTIALS = new UsernamePasswordCredentials("Author",
        "password");

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
            new DefaultExtensionAuthor("User Name", new URL(getUtil().getURL("XWiki", USER_CREDENTIALS.getUserName())));
        this.baseExtension.addAuthor(this.baseAuthor);

        this.baseExtension.addDependency(new DefaultExtensionDependency("dependencyid1", new DefaultVersionConstraint(
            "1.0")));
        this.baseExtension.addDependency(new DefaultExtensionDependency("dependencyid2", new DefaultVersionConstraint(
            "2.0")));

        this.sizeOfFile = FileUtils.sizeOf(this.baseExtension.getFile().getFile());
    }

    @Test
    public void testAddExtension() throws Exception
    {
        // Set id prefix

        RepositoryAdminPage repositoryAdminPage = RepositoryAdminPage.gotoPage();

        repositoryAdminPage.setDefaultIdPrefix(IDPREFIX);
        repositoryAdminPage.clickUpdateButton();

        // Create extension

        getUtil().createUserAndLogin(USER_CREDENTIALS.getUserName(), USER_CREDENTIALS.getPassword(), null,
            "first_name", "User", "last_name", "Name");

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
        getRepositoryTestUtils().addVersionObject(
            this.baseExtension,
            "10.0",
            getUtil().getAttachmentURL("Extension", this.baseExtension.getName(),
                this.baseExtension.getFile().getName()));
        getRepositoryTestUtils().addVersionObject(this.baseExtension, "2.0",
            "attach:" + this.baseExtension.getFile().getName());

        // Add dependencies
        // TODO: add XR UI to manipulate dependencies
        getRepositoryTestUtils().addDependencies(this.baseExtension, "10.0");

        // Add attachment
        getRepositoryTestUtils().attachFile(this.baseExtension, USER_CREDENTIALS);

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
            getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "1.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("1.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL("Extension", this.baseExtension.getName()), extension.getWebsite());

        // File

        Assert
            .assertEquals(
                this.sizeOfFile,
                getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, this.baseExtension.getId().getId(),
                    "1.0").length);

        // //////////////////////////////////////////
        // 2.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "2.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("2.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL("Extension", this.baseExtension.getName()), extension.getWebsite());

        // File

        Assert
            .assertEquals(
                this.sizeOfFile,
                getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, this.baseExtension.getId().getId(),
                    "2.0").length);

        // //////////////////////////////////////////
        // 10.0
        // //////////////////////////////////////////

        // Resolve

        extension =
            getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, this.baseExtension.getId().getId(), "10.0");

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        Assert.assertEquals(getUtil().getURL("Extension", this.baseExtension.getName()), extension.getWebsite());

        ExtensionDependency dependency1 = extension.getDependencies().get(0);
        Assert.assertEquals("dependencyid1", dependency1.getId());
        Assert.assertEquals("1.0", dependency1.getConstraint());
        ExtensionDependency dependency2 = extension.getDependencies().get(1);
        Assert.assertEquals("dependencyid2", dependency2.getId());
        Assert.assertEquals("2.0", dependency2.getConstraint());

        // File

        Assert.assertEquals(
            this.sizeOfFile,
            getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, this.baseExtension.getId().getId(),
                this.baseExtension.getId().getVersion().getValue()).length);

        // //////////////////////////////////////////
        // Search
        // //////////////////////////////////////////

        // Empty search

        Map<String, Object[]> queryParams = new HashMap<String, Object[]>();
        ExtensionsSearchResult result = getUtil().getRESTResource(Resources.SEARCH, queryParams);

        Assert.assertTrue(result.getTotalHits() >= 0);
        Assert.assertEquals(0, result.getOffset());

        extension = null;
        for (ExtensionVersion extensionVersion : result.getExtensions()) {
            if (extensionVersion.getId().equals(this.baseExtension.getId().getId())) {
                extension = extensionVersion;
                break;
            }
        }
        if (extension == null) {
            Assert.fail("Count not find extension [" + this.baseExtension.getId().getId() + "]");
        }

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        // TODO: add support for dependencies in XR search

        // Search pattern

        queryParams.clear();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"macro"});

        result = getUtil().getRESTResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(1, result.getTotalHits());
        Assert.assertEquals(0, result.getOffset());
        extension = result.getExtensions().get(0);

        Assert.assertEquals(this.baseExtension.getId().getId(), extension.getId());
        Assert.assertEquals(this.baseExtension.getType(), extension.getType());
        Assert.assertEquals(this.baseExtension.getSummary(), extension.getSummary());
        Assert.assertEquals(this.baseLicense.getName(), extension.getLicenses().get(0).getName());
        Assert.assertEquals(this.baseExtension.getDescription(), extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals("10.0", extension.getVersion());

        // Wrong search pattern

        queryParams.clear();
        queryParams.put(Resources.QPARAM_SEARCH_QUERY, new Object[] {"notexisting"});

        result = getUtil().getRESTResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(0, result.getTotalHits());
        Assert.assertEquals(0, result.getOffset());
        Assert.assertEquals(0, result.getExtensions().size());

        // Search limit offset

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_START, new Object[] {1});

        result = getUtil().getRESTResource(Resources.SEARCH, queryParams);

        Assert.assertEquals(1, result.getOffset());
        Assert.assertEquals(result.getTotalHits() - 1, result.getExtensions().size());

        // Search limit nb

        queryParams.clear();
        queryParams.put(Resources.QPARAM_LIST_NUMBER, new Object[] {0});

        result = getUtil().getRESTResource(Resources.SEARCH, queryParams);

        Assert.assertTrue(result.getTotalHits() >= 1);
        Assert.assertEquals(0, result.getOffset());
        Assert.assertEquals(0, result.getExtensions().size());
    }

    @Test
    public void testImportExtension() throws Exception
    {
        getUtil().createUser(USER_CREDENTIALS.getUserName(), USER_CREDENTIALS.getPassword(), null, "first_name",
            "User", "last_name", "Name");

        ExtensionsPage extensionsPage = ExtensionsPage.gotoPage();

        ExtensionImportPage importPage = extensionsPage.clickImport();

        importPage.setExtensionId("maven:extension");
        importPage.setSourceRepository("maven-test");
        ExtensionPage extensionPage = importPage.clickImport();

        // Check

        Assert.assertEquals("1.1", extensionPage.getMetaDataValue("version"));
        Assert.assertTrue(extensionPage.isValidExtension());

        // 2.0

        TestExtension emptyExtension =
            getRepositoryTestUtils().getTestExtension(new ExtensionId("emptyjar", "1.0"), "jar");

        long fileSize = FileUtils.sizeOf(emptyExtension.getFile().getFile());

        ExtensionVersion extension =
            getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, "maven:extension", "2.0");

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("2.0", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList("maven:oldextension"), extension.getFeatures());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        Assert.assertEquals(fileSize,
            getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "2.0").length);

        // 1.0

        extension = getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, "maven:extension", "1.0");

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("1.0", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList("maven:oldextension"), extension.getFeatures());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        Assert.assertEquals(FileUtils.sizeOf(emptyExtension.getFile().getFile()),
            getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "1.0").length);

        // 0.9

        extension = getUtil().getRESTResource(Resources.EXTENSION_VERSION, null, "maven:extension", "0.9");

        Assert.assertEquals("maven:extension", extension.getId());
        Assert.assertEquals("jar", extension.getType());
        Assert.assertEquals("0.9", extension.getVersion());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary2", extension.getSummary());
        Assert.assertEquals("summary2\n      some more details", extension.getDescription());
        Assert.assertEquals(this.baseAuthor.getName(), extension.getAuthors().get(0).getName());
        Assert.assertEquals(this.baseAuthor.getURL().toString(), extension.getAuthors().get(0).getUrl());
        Assert.assertEquals(Arrays.asList("maven:oldextension"), extension.getFeatures());
        Assert.assertEquals("GNU Lesser General Public License 2.1", extension.getLicenses().get(0).getName());

        Assert.assertEquals(fileSize,
            getUtil().getRESTBuffer(Resources.EXTENSION_VERSION_FILE, null, "maven:extension", "0.9").length);

        // Import again

        extensionPage = extensionPage.updateExtension();

        Assert.assertEquals("1.1", extensionPage.getMetaDataValue("version"));
    }
}
