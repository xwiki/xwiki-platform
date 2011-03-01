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

import java.util.Collection;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.extension.task.Task;
import org.xwiki.extension.task.TaskException;
import org.xwiki.extension.task.TaskManager;
import org.xwiki.extension.task.UninstallRequest;
import org.xwiki.script.service.ScriptService;

@Component("extension")
public class ExtensionManagerScriptService implements ScriptService
{
    private static final String EXTENSIONERROR_KEY = "extensionerror";

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

    @Requirement
    private TaskManager taskManager;

    @Requirement
    private Execution execution;

    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(EXTENSIONERROR_KEY);
    }

    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(EXTENSIONERROR_KEY, e);
    }

    public Extension resolve(String id, String version, String namespace)
    {
        setError(null);

        Extension extension;

        try {
            extension = this.extensionManager.resolveExtension(new ExtensionId(id, version), namespace);
        } catch (ResolveException e) {
            setError(e);

            extension = null;
        }

        return extension;
    }

    public Collection<LocalExtension> getBackwardDependencies(String id, String wiki)
    {
        setError(null);

        Collection<LocalExtension> extensions;

        try {
            extensions = this.localExtensionRepository.getBackwardDependencies(id, wiki);
        } catch (ResolveException e) {
            setError(e);

            extensions = null;
        }

        return extensions;
    }

    public LocalExtension getInstalledExtension(String id, String namespace)
    {
        return this.localExtensionRepository.getInstalledExtension(id, namespace);
    }

    public Collection<LocalExtension> getInstalledExtensions(String namespace)
    {
        return this.localExtensionRepository.getInstalledExtensions(namespace);
    }

    public Collection<LocalExtension> getInstalledExtensions()
    {
        return this.localExtensionRepository.getInstalledExtensions();
    }

    public Collection<CoreExtension> getCoreExtensions()
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

    // Tasks

    public Task getCurrentTask()
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            return null;
        }

        return this.taskManager.getCurrentTask();
    }

    public Task install(String id, String version, String wiki)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            return null;
        }

        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(id, null));
        installRequest.addNamespace(wiki);

        Task task;
        try {
            task = this.taskManager.install(installRequest);
        } catch (TaskException e) {
            setError(e);

            task = null;
        }

        return task;
    }

    public Task uninstall(String id, String wiki)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            return null;
        }

        setError(null);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(new ExtensionId(id, null));
        uninstallRequest.addNamespace(wiki);

        Task task;
        try {
            task = this.taskManager.uninstall(uninstallRequest);
        } catch (TaskException e) {
            setError(e);

            task = null;
        }

        return task;
    }
}
