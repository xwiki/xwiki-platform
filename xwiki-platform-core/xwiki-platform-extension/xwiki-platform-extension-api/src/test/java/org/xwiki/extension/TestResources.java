package org.xwiki.extension;

import org.xwiki.extension.repository.LocalExtensionRepository;

public class TestResources
{
    // Core

    public static final ExtensionId CORE_ID = new ExtensionId("coreextension", "version");

    // Installed

    public static final ExtensionId INSTALLED_ID = new ExtensionId("installedextension", "version");

    public static final ExtensionId INSTALLED_DEPENDENCY_ID = new ExtensionId("installedextensiondependency", "version");

    public LocalExtension installed;

    public LocalExtension installedDependency;

    // Remote

    public static final ExtensionId REMOTE_SIMPLE_ID = new ExtensionId("rsimple", "version");

    public static final ExtensionId REMOTE_WITHRDEPENDENCY_ID = new ExtensionId("rwithrdependency", "version");

    public static final ExtensionId REMOTE_WITHCDEPENDENCY_ID = new ExtensionId("rwithcdependency", "version");

    public static final ExtensionId REMOTE_WITHLDEPENDENCY_ID = new ExtensionId("rwithldependency", "version");

    public static final ExtensionId REMOTE_WITHRANDCDEPENDENCIES_ID = new ExtensionId("rwithcrdependencies", "version");

    // Methods

    public void init(LocalExtensionRepository localExtensionRepository) throws ResolveException
    {
        this.installed = (LocalExtension) localExtensionRepository.resolve(INSTALLED_ID);
        this.installedDependency = (LocalExtension) localExtensionRepository.resolve(INSTALLED_DEPENDENCY_ID);
    }
}
