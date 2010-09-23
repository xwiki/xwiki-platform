package org.xwiki.extension.repository;

import java.io.File;
import java.net.URI;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.internal.DefaultLocalExtensionRepository;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.test.AbstractComponentTestCase;

public class DefaultExtensionManagerTest extends AbstractComponentTestCase
{
    private ExtensionRepositoryManager repositoryManager;

    private ExtensionManager extensionManager;

    private ExtensionId rubyArtifactId;

    private ConfigurableDefaultCoreExtensionRepository coreExtensionRepository;

    private LocalExtensionRepository localExtensionRepository;
    
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        getConfigurationSource().setProperty("extension.aether.localRepository",
            "target/DefaultExtensionManagerTest/test-aether-repository");
        getConfigurationSource().setProperty("extension.localRepository",
            "target/DefaultExtensionManagerTest/test-repository");

        File testDirectory = new File("target/DefaultExtensionManagerTest");
        if (testDirectory.exists()) {
            FileUtils.deleteDirectory(testDirectory);
        }

        this.repositoryManager = getComponentManager().lookup(ExtensionRepositoryManager.class);

        this.repositoryManager.addRepository(new ExtensionRepositoryId("xwiki-releases", "maven", new URI(
            "http://maven.xwiki.org/releases/")));
        this.repositoryManager.addRepository(new ExtensionRepositoryId("central", "maven", new URI(
            "http://repo1.maven.org/maven2/")));

        this.rubyArtifactId = new ExtensionId("org.xwiki.platform:xwiki-core-rendering-macro-ruby", "2.4");

        this.extensionManager = getComponentManager().lookup(ExtensionManager.class);
        this.coreExtensionRepository =
            (ConfigurableDefaultCoreExtensionRepository) getComponentManager().lookup(CoreExtensionRepository.class);
        this.localExtensionRepository =
            (DefaultLocalExtensionRepository) getComponentManager().lookup(LocalExtensionRepository.class);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        ConfigurableDefaultCoreExtensionRepository.register(getComponentManager());
    }

    @Test
    public void testInstallAndUninstallExtension() throws Exception
    {
        // way too big for a unit test so lets skip it
        this.coreExtensionRepository.addExtensions("org.jruby:jruby", "1.5");

        // emulate environment
        registerMockComponent(DocumentAccessBridge.class);
        registerMockComponent(AttachmentReferenceResolver.class, "current");

        // actual test
        LocalExtension localExtension = this.extensionManager.installExtension(this.rubyArtifactId);

        Assert.assertNotNull(localExtension);
        Assert.assertNotNull(localExtension.getFile());
        Assert.assertTrue(localExtension.getFile().exists());

        Macro rubyMacro = getComponentManager().lookup(Macro.class, "ruby");

        Assert.assertNotNull(rubyMacro);

        try {
            this.extensionManager.installExtension(this.rubyArtifactId);
            Assert.fail("installExtension should have failed");
        } catch (InstallException expected) {
            // expected
        }

        this.extensionManager.uninstallExtension(this.rubyArtifactId.getId());

        Assert.assertNull(this.localExtensionRepository.getLocalExtension(this.rubyArtifactId.getId()));
        
        try {
            getComponentManager().lookup(Macro.class, "ruby");
            Assert.fail("the extension has not been uninstalled");
        } catch (ComponentLookupException expected) {
            // expected
        }
    }
}
