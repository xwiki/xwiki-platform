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
package org.xwiki.extension.script.internal;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.internal.VersionManager;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.extension.task.Task;
import org.xwiki.extension.task.TaskException;
import org.xwiki.extension.task.TaskManager;
import org.xwiki.extension.task.UninstallRequest;
import org.xwiki.script.service.ScriptService;

@Component
@Named("extension")
@Singleton
public class ExtensionManagerScriptService implements ScriptService
{
    private static final String EXTENSIONERROR_KEY = "extensionerror";

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private VersionManager versionManager;

    @Inject
    private LocalExtensionRepository localExtensionRepository;

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private TaskManager taskManager;

    @Inject
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
        } catch (Exception e) {
            setError(e);

            extension = null;
        }

        return extension;
    }

    public Map<String, Collection<LocalExtension>> getBackwardDependencies(String id, String version)
    {
        setError(null);

        Map<String, Collection<LocalExtension>> extensions;

        try {
            extensions = this.localExtensionRepository.getBackwardDependencies(new ExtensionId(id, version));
        } catch (Exception e) {
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
            setError(new TaskException("Need programming right to get current task"));

            return null;
        }

        return this.taskManager.getCurrentTask();
    }

    public Task install(String id, String version, String wiki)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new TaskException("Need programming right to install an extension"));

            return null;
        }

        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(id, version));
        if (wiki != null) {
            installRequest.addNamespace(wiki);
        }

        Task task;
        try {
            task = this.taskManager.install(installRequest);
        } catch (TaskException e) {
            setError(e);

            task = null;
        }

        return task;
    }

    public Task uninstall(String id, String version)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new TaskException("Need programming right to uninstall an extension"));

            return null;
        }

        setError(null);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(new ExtensionId(id, version));

        Task task;
        try {
            task = this.taskManager.uninstall(uninstallRequest);
        } catch (Exception e) {
            setError(e);

            task = null;
        }

        return task;
    }
}
