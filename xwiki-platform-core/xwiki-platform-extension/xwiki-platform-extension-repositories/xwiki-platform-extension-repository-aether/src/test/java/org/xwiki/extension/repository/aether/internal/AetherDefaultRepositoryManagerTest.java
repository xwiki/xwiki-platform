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

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.test.AbstractComponentTestCase;

public class AetherDefaultRepositoryManagerTest extends AbstractComponentTestCase
{
    private ExtensionRepositoryManager repositoryManager;

    private ExtensionId extensionId;

    private ExtensionId dependencyExtensionId;

    private RepositoryUtil repositoryUtil;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        this.extensionId = new ExtensionId("groupid:artifactid", "version");
        this.dependencyExtensionId = new ExtensionId("dgroupid:dartifactid", "dversion");

        // lookup

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);
    }

    @Test
    public void testResolve() throws ResolveException
    {
        Extension artifact = this.repositoryManager.resolve(this.extensionId);

        Assert.assertNotNull(artifact);
        Assert.assertEquals(this.extensionId.getId(), artifact.getId().getId());
        Assert.assertEquals(this.extensionId.getVersion(), artifact.getId().getVersion());
        Assert.assertEquals("type", artifact.getType());
        Assert.assertEquals(this.repositoryUtil.getRemoteRepositoryId(), artifact.getRepository().getId().getId());
        Assert.assertEquals("description", artifact.getDescription());
        Assert.assertEquals("http://website", artifact.getWebSite());
        
        ExtensionDependency dependency = artifact.getDependencies().get(0);
        Assert.assertEquals(this.dependencyExtensionId.getId(), dependency.getId());
        Assert.assertEquals(this.dependencyExtensionId.getVersion(), dependency.getVersion());

        // check that a new resolve of an already resolved extension provide the proper repository
        artifact = this.repositoryManager.resolve(this.extensionId);
        Assert.assertEquals(this.repositoryUtil.getRemoteRepositoryId(), artifact.getRepository().getId().getId());
    }

    @Test
    public void testDownload() throws ExtensionException, IOException
    {
        Extension artifact = this.repositoryManager.resolve(this.extensionId);

        File file = new File("target/downloaded/" + this.extensionId.getId() + "." + artifact.getType());

        if (file.exists()) {
            file.delete();
        }

        artifact.download(file);

        Assert.assertTrue("File has not been downloaded", file.exists());

        Assert.assertEquals("content", FileUtils.readFileToString(file));
    }
}
