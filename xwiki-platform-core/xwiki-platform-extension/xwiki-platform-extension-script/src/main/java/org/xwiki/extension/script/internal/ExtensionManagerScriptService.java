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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobException;
import org.xwiki.extension.job.JobManager;
import org.xwiki.extension.job.JobStatus;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.wrap.WrappingCoreExtension;
import org.xwiki.extension.wrap.WrappingExtension;
import org.xwiki.extension.wrap.WrappingJobStatus;
import org.xwiki.extension.wrap.WrappingLocalExtension;
import org.xwiki.script.service.ScriptService;

/**
 * Entry point of extension manager from scripts.
 * 
 * @version $Id$
 */
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
    private ExtensionRepositoryManager reporitoryManager;

    @Inject
    private JobManager taskManager;

    @Inject
    private Execution execution;

    private <K> Map<K, Collection<LocalExtension>> wrapExtensions(Map<K, Collection<LocalExtension>> extensions)
    {
        Map<K, Collection<LocalExtension>> wrappedExtensions = new LinkedHashMap<K, Collection<LocalExtension>>();

        for (Map.Entry<K, Collection<LocalExtension>> entry : extensions.entrySet()) {
            wrappedExtensions.put(entry.getKey(), this.<LocalExtension> wrapExtensions(entry.getValue()));
        }

        return wrappedExtensions;
    }

    private <T extends Extension> Collection<T> wrapExtensions(Collection< ? extends Extension> extensions)
    {
        List<T> wrappedExtensions = new ArrayList<T>(extensions.size());

        for (Extension extension : extensions) {
            wrappedExtensions.add(this.<T> wrapExtension(extension));
        }

        return wrappedExtensions;
    }

    private <T extends Extension> T wrapExtension(Extension extension)
    {
        T wrappedExtension;

        if (extension instanceof CoreExtension) {
            wrappedExtension = (T) new WrappingCoreExtension<CoreExtension>((CoreExtension) extension);
        } else if (extension instanceof LocalExtension) {
            wrappedExtension = (T) new WrappingLocalExtension<LocalExtension>((LocalExtension) extension);
        } else if (extension != null) {
            wrappedExtension = (T) new WrappingExtension<Extension>(extension);
        } else {
            wrappedExtension = null;
        }

        return wrappedExtension;
    }

    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(EXTENSIONERROR_KEY);
    }

    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(EXTENSIONERROR_KEY, e);
    }

    public Extension resolve(String id, String version)
    {
        setError(null);

        Extension extension;

        try {
            extension = wrapExtension(this.extensionManager.resolveExtension(new ExtensionId(id, version)));
        } catch (Exception e) {
            setError(e);

            extension = null;
        }

        return extension;
    }

    public Collection<Extension> search(String pattern, int offset, int nb)
    {
        return wrapExtensions(this.reporitoryManager.search(pattern, offset, nb));
    }

    public Map<String, Collection<LocalExtension>> getBackwardDependencies(String id, String version)
    {
        setError(null);

        Map<String, Collection<LocalExtension>> extensions;

        try {
            extensions =
                this.<String> wrapExtensions(this.localExtensionRepository.getBackwardDependencies(new ExtensionId(id,
                    version)));
        } catch (Exception e) {
            setError(e);

            extensions = null;
        }

        return extensions;
    }

    public LocalExtension getInstalledExtension(String id, String namespace)
    {
        return wrapExtension(this.localExtensionRepository.getInstalledExtension(id, namespace));
    }

    public Collection<LocalExtension> getInstalledExtensions(String namespace)
    {
        return wrapExtensions(this.localExtensionRepository.getInstalledExtensions(namespace));
    }

    public Collection<LocalExtension> getInstalledExtensions()
    {
        return wrapExtensions(this.localExtensionRepository.getInstalledExtensions());
    }

    public Collection<CoreExtension> getCoreExtensions()
    {
        return wrapExtensions(this.coreExtensionRepository.getCoreExtensions());
    }

    public Collection<LocalExtension> getLocalExtensions()
    {
        return wrapExtensions(this.localExtensionRepository.getLocalExtensions());
    }

    public CoreExtension getCoreExtension(String id)
    {
        return wrapExtension(this.coreExtensionRepository.getCoreExtension(id));
    }

    public VersionManager getVersionManager()
    {
        return this.versionManager;
    }

    // Jobs

    public Job getCurrentJob()
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to get current task"));

            return null;
        }

        return this.taskManager.getCurrentJob();
    }

    public JobStatus getCurrentJobStatus()
    {
        Job job = this.taskManager.getCurrentJob();

        JobStatus jobStatus;
        if (job != null) {
            jobStatus = job.getStatus();
            if (!this.documentAccessBridge.hasProgrammingRights()) {
                jobStatus = new WrappingJobStatus(jobStatus);
            }
        } else {
            jobStatus = null;
        }

        return jobStatus;
    }

    public Job install(String id, String version, String wiki)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to install an extension"));

            return null;
        }

        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(id, version));
        if (wiki != null) {
            installRequest.addNamespace(wiki);
        }

        Job task;
        try {
            task = this.taskManager.install(installRequest);
        } catch (JobException e) {
            setError(e);

            task = null;
        }

        return task;
    }

    public Job uninstall(String id, String version)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to uninstall an extension"));

            return null;
        }

        setError(null);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(new ExtensionId(id, version));

        Job task;
        try {
            task = this.taskManager.uninstall(uninstallRequest);
        } catch (Exception e) {
            setError(e);

            task = null;
        }

        return task;
    }
}
