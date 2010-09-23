package org.xwiki.extension.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.test.AbstractComponentTestCase;

public class AetherDefaultRepositoryManagerTest extends AbstractComponentTestCase
{
    private ExtensionRepositoryManager repositoryManager;

    private ExtensionId rubyArtifactId;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        getConfigurationSource().setProperty("extension.aether.localRepository",
            "target/AetherDefaultRepositoryManagerTest/test-aether-repository");

        File testDirectory = new File("target/AetherDefaultRepositoryManagerTest");
        if (testDirectory.exists()) {
            FileUtils.deleteDirectory(testDirectory);
        }

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);

        this.repositoryManager.addRepository(new ExtensionRepositoryId("central", "maven", new URI(
            "http://repo1.maven.org/maven2/")));
        this.repositoryManager.addRepository(new ExtensionRepositoryId("xwiki-releases", "maven", new URI(
            "http://maven.xwiki.org/releases/")));

        this.rubyArtifactId = new ExtensionId("org.xwiki.platform:xwiki-core-rendering-macro-ruby", "2.4");
    }

    @Test
    public void testResolve() throws ResolveException
    {
        Extension artifact = this.repositoryManager.resolve(this.rubyArtifactId);

        Assert.assertNotNull(artifact);
        Assert.assertEquals("org.xwiki.platform:xwiki-core-rendering-macro-ruby", artifact.getId());
        Assert.assertEquals("2.4", artifact.getVersion());
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
