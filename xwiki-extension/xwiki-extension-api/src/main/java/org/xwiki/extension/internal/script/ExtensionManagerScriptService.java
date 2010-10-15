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
package org.xwiki.extension.internal.script;

import java.util.List;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
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

    @Requirement
    private CoreExtensionRepository coreExtensionRepository;

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

    public List<CoreExtension> getCoreExtensions()
    {
        return this.coreExtensionRepository.getCoreExtensions();
    }

    public CoreExtension getCoreExtension(String id)
    {
        return this.coreExtensionRepository.getCoreExtension(id);
    }

    public VersionManager getVersionManager()
    {
        return this.versionManager;
    }
}
