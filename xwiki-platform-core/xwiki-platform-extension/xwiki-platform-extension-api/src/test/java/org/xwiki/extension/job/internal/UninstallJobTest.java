package org.xwiki.extension.job.internal;

import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.test.AbstractExtensionHandlerTest;
import org.xwiki.extension.test.ConfigurableDefaultCoreExtensionRepository;

public class UninstallJobTest extends AbstractExtensionHandlerTest
{
    private ExtensionId remoteExtensionId;

    private ExtensionId existingExtensionId;

    private ExtensionId existingExtensionDependencyId;

    private ConfigurableDefaultCoreExtensionRepository coreRepository;

    private LocalExtension existingExtension;

    private LocalExtension existingExtensionDependency;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // lookup

        this.coreRepository =
            (ConfigurableDefaultCoreExtensionRepository) getComponentManager().lookup(CoreExtensionRepository.class);

        // resources

        this.remoteExtensionId = new ExtensionId("remoteextension", "version");
        this.existingExtensionId = new ExtensionId("existingextension", "version");
        this.existingExtensionDependencyId = new ExtensionId("existingextensiondependency", "version");

        this.existingExtension = (LocalExtension) this.localExtensionRepository.resolve(this.existingExtensionId);
        this.existingExtensionDependency =
            (LocalExtension) this.localExtensionRepository.resolve(this.existingExtensionDependencyId);
    }

    @Test
    public void testInstallTwice() throws Throwable
    {
        uninstall(this.existingExtensionId);

        try {
            uninstall(this.existingExtensionId);
        } catch (UninstallException expected) {
            // expected
        }
    }
}
