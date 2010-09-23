package org.xwiki.extension.repository.internal.aether.configuration;

import java.io.File;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.extension.ExtensionManagerConfiguration;

@Component
public class DefaultAetherConfiguration extends AbstractLogEnabled implements AetherConfiguration
{
    @Requirement("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Requirement
    private ExtensionManagerConfiguration extensionManagerConfiguration;

    public File getLocalRepository()
    {
        String localRepositoryPath = this.configurationSource.getProperty("extension.aether.localRepository");

        return localRepositoryPath != null ? new File(localRepositoryPath) : new File(this.extensionManagerConfiguration
            .getLocalRepository().getParent(), "aether-repository");
    }
}
