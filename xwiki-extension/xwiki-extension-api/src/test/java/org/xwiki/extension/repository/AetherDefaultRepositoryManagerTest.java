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
package org.xwiki.extension.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.test.AbstractComponentTestCase;

public class AetherDefaultRepositoryManagerTest extends AbstractComponentTestCase
{
    private ExtensionRepositoryManager repositoryManager;

    private ExtensionId rubyArtifactId;

    private RepositoryUtil repositoryUtil;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        this.rubyArtifactId = new ExtensionId("org.xwiki.platform:xwiki-core-rendering-macro-ruby", "2.4");

        // lookup

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);

        this.repositoryManager.addRepository(new ExtensionRepositoryId("central", "maven", new URI(
            "http://repo1.maven.org/maven2/")));
        this.repositoryManager.addRepository(new ExtensionRepositoryId("xwiki-releases", "maven", new URI(
            "http://maven.xwiki.org/releases/")));

    }

    @Test
    public void testResolve() throws ResolveException
    {
        Extension artifact = this.repositoryManager.resolve(this.rubyArtifactId);

        Assert.assertNotNull(artifact);
        Assert.assertEquals("org.xwiki.platform:xwiki-core-rendering-macro-ruby", artifact.getId().getId());
        Assert.assertEquals("2.4", artifact.getId().getVersion());
        Assert.assertEquals("jar", artifact.getType());
        Assert.assertEquals("xwiki-releases", artifact.getRepository().getId().getId());

        ExtensionDependency dependency = artifact.getDependencies().get(1);
        Assert.assertEquals("org.jruby:jruby", dependency.getId());
        Assert.assertEquals("1.5.0", dependency.getVersion());

        // check that a new resolve of an already resolved extension provide the proper repository
        artifact = this.repositoryManager.resolve(this.rubyArtifactId);
        Assert.assertEquals("xwiki-releases", artifact.getRepository().getId().getId());
    }

    @Test
    public void testDownload() throws ExtensionException, IOException
    {
        Extension artifact = this.repositoryManager.resolve(this.rubyArtifactId);

        File file = new File("target/downloaded/rubymacro.jar");

        if (file.exists()) {
            file.delete();
        }

        artifact.download(file);

        Assert.assertTrue("File has not been downloaded", file.exists());

        ZipInputStream zis = new ZipInputStream(new FileInputStream(file));

        boolean found = false;

        for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
            if (entry.getName().equals("org/xwiki/rendering/internal/macro/ruby/RubyMacro.class")) {
                found = true;
                break;
            }
        }

        if (!found) {
            Assert.fail("Does not seems to be the right file");
        }

        zis.close();
    }
}
