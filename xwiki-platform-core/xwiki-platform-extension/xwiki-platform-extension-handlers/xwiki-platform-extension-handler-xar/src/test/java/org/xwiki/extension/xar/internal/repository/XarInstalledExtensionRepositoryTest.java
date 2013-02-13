package org.xwiki.extension.xar.internal.repository;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;
import org.xwiki.extension.test.RepositoryUtil;
import org.xwiki.test.jmock.AbstractComponentTestCase;

public class XarInstalledExtensionRepositoryTest extends AbstractComponentTestCase
{
    private XarInstalledExtensionRepository installedExtensionRepository;

    private RepositoryUtil repositoryUtil;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtil(getComponentManager(), getMockery());
        this.repositoryUtil.getExtensionPackager().setDefaultDirectory(this.repositoryUtil.getLocalRepository());
        this.repositoryUtil.setup();

        // lookup

        this.installedExtensionRepository =
            getComponentManager().getInstance(InstalledExtensionRepository.class, "xar");
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        registerComponent(ConfigurableDefaultCoreExtensionRepository.class);
    }

    // Tests

    @Test
    public void testInit() throws ResolveException
    {
        Assert.assertTrue(this.installedExtensionRepository.countExtensions() == 1);

        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension(new ExtensionId(
            "xarinstalledextension", "1.0")));

        Assert
            .assertNotNull(this.installedExtensionRepository.resolve(new ExtensionId("xarinstalledextension", "1.0")));
    }
}
