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
package org.xwiki.extension.repository.aether.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.test.AbstractComponentTestCase;

public class AetherDefaultRepositoryManagerTest extends AbstractComponentTestCase
{
    private static final String GROUPID = "groupid";

    private static final String ARTIfACTID = "artifactid";

    private ExtensionRepositoryManager repositoryManager;

    private ExtensionId extensionId;

    private ExtensionId extensionDependencyId;

    private ExtensionId extensionIdClassifier;

    private ExtensionDependency dependencyExtensionId;

    private ExtensionDependency dependencyExtensionIdRange;

    private ExtensionLicenseManager extensionLicenseManager;

    private ExtensionId bundleExtensionId;

    private RepositoryUtil repositoryUtil;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        this.extensionId = new ExtensionId(GROUPID + ':' + ARTIfACTID, "version");
        this.extensionDependencyId = new ExtensionId("dgroupid:dartifactid", "dversion");
        
        this.extensionIdClassifier = new ExtensionId(GROUPID + ':' + ARTIfACTID + ":classifier", "version");
        this.dependencyExtensionId =
            new DefaultExtensionDependency(this.extensionDependencyId.getId(), new DefaultVersionConstraint(this.extensionDependencyId.getVersion().getValue()));
        this.dependencyExtensionIdRange =
            new DefaultExtensionDependency(this.extensionDependencyId.getId(), new DefaultVersionConstraint("[dversion,)"));

        this.bundleExtensionId = new ExtensionId("groupid:bundleartifactid", "version");

        // lookup

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);
        this.extensionLicenseManager = getComponentManager().lookup(ExtensionLicenseManager.class);
    }

    @Test
    public void testResolve() throws ResolveException, IOException
    {
        Extension extension = this.repositoryManager.resolve(this.extensionId);

        Assert.assertNotNull(extension);
        Assert.assertEquals(this.extensionId.getId(), extension.getId().getId());
        Assert.assertEquals(this.extensionId.getVersion(), extension.getId().getVersion());
        Assert.assertEquals("type", extension.getType());
        Assert.assertEquals(this.repositoryUtil.getRemoteRepositoryId(), extension.getRepository().getId().getId());
        Assert.assertEquals("name", extension.getName());
        Assert.assertEquals("summary", extension.getSummary());
        Assert.assertEquals("http://website", extension.getWebSite());
        Assert.assertEquals("Full Name", extension.getAuthors().get(0).getName());
        Assert.assertEquals(new URL("http://profile"), extension.getAuthors().get(0).getURL());
        Assert.assertEquals(Arrays.asList("groupid1:feature1", "groupid2:feature2"),
            new ArrayList<String>(extension.getFeatures()));
        Assert.assertSame(this.extensionLicenseManager.getLicense("GNU Lesser General Public License 2.1"), extension
            .getLicenses().iterator().next());

        ExtensionDependency dependency = extension.getDependencies().get(0);
        Assert.assertEquals(this.dependencyExtensionId.getId(), dependency.getId());
        Assert.assertEquals(this.dependencyExtensionId.getVersionConstraint(), dependency.getVersionConstraint());

        // check that a new resolve of an already resolved extension provide the proper repository
        extension = this.repositoryManager.resolve(this.extensionId);
        Assert.assertEquals(this.repositoryUtil.getRemoteRepositoryId(), extension.getRepository().getId().getId());

        // TODO: see http://jira.xwiki.org/browse/XWIKI-7163 // Modify the file on the descriptor on the repository
        // File pomFile =
        // new File(this.repositoryUtil.getMavenRepository(), this.extensionId.getId().replace('.', '/')
        // .replace(':', '/')
        // + '/' + this.extensionId.getVersion() + '/' + ARTIfACTID + '-' + this.extensionId.getVersion() + ".pom");
        // FileUtils.writeStringToFile(
        // pomFile,
        // FileUtils.readFileToString(pomFile, "UTF-8").replace("<description>summary</description>",
        // "<description>modified summary</description>"), "UTF-8");
        // extension = this.repositoryManager.resolve(this.extensionId);
        // Assert.assertEquals("modified description", extension.getSummary());
    }

    @Test
    public void testResolveVersionClassifier() throws ResolveException
    {
        Extension extension = this.repositoryManager.resolve(this.extensionIdClassifier);

        Assert.assertNotNull(extension);
        Assert.assertEquals(this.extensionIdClassifier.getId(), extension.getId().getId());
        Assert.assertEquals(this.extensionIdClassifier.getVersion(), extension.getId().getVersion());
    }

    @Test
    public void testResolveVersionRange() throws ResolveException
    {
        Extension extension = this.repositoryManager.resolve(this.dependencyExtensionIdRange);

        Assert.assertNotNull(extension);
        Assert.assertEquals(this.extensionDependencyId.getId(), extension.getId().getId());
        Assert.assertEquals(this.extensionDependencyId.getVersion(), extension.getId()
            .getVersion());
    }

    @Test
    public void testDownload() throws ExtensionException, IOException
    {
        Extension extension = this.repositoryManager.resolve(this.extensionId);

        InputStream is = extension.getFile().openStream();

        try {
            Assert.assertEquals("content", IOUtils.toString(is));
        } finally {
            is.close();
        }
    }

    @Test
    public void testDownloadClassifier() throws ExtensionException, IOException
    {
        Extension extension = this.repositoryManager.resolve(this.extensionIdClassifier);

        InputStream is = extension.getFile().openStream();

        try {
            Assert.assertEquals("classifier content", IOUtils.toString(is));
        } finally {
            is.close();
        }
    }

    @Test
    public void testDownloadBundle() throws ExtensionException, IOException
    {
        Extension extension = this.repositoryManager.resolve(this.bundleExtensionId);

        InputStream is = extension.getFile().openStream();

        try {
            Assert.assertEquals("content", IOUtils.toString(is));
        } finally {
            is.close();
        }
    }
}
