package org.xwiki.extension.internal.script;

import java.util.List;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.script.service.ScriptService;

@Component("extension")
public class ExtensionManagerScriptService implements ScriptService
{
    @Requirement
    private ExtensionManager extensionManager;

    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Requirement
    private VersionManager versionManager;

    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    public LocalExtension install(String id, String version) throws InstallException
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            return null;
        }

        return this.extensionManager.installExtension(new ExtensionId(id, version));
    }

    public Extension resolve(String id, String version) throws ResolveException
    {
        return this.extensionManager.resolveExtension(new ExtensionId(id, version));
    }

    public void uninstall(String id) throws UninstallException
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            return;
        }

        this.extensionManager.uninstallExtension(id);
    }

    public List<LocalExtension> getBackwardDependencies(String id) throws ResolveException
    {
        return this.localExtensionRepository.getBackwardDependencies(id);
    }

    public List<LocalExtension> getInstalledExtensions()
    {
        return this.localExtensionRepository.getLocalExtensions();
    }
    
    public LocalExtension getInstalledExtension(String id)
    {
        return this.localExtensionRepository.getLocalExtension(id);
    }

    public VersionManager getVersionManager()
    {
        return this.versionManager;
    }
}
