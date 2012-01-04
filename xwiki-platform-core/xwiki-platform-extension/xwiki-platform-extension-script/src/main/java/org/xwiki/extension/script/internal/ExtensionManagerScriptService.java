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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobException;
import org.xwiki.extension.job.JobManager;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.event.status.JobStatus;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.unmodifiable.UnmodifiableExtensionPlan;
import org.xwiki.extension.unmodifiable.UnmodifiableJobStatus;
import org.xwiki.extension.unmodifiable.UnmodifiableUtils;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.VersionRange;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersionRange;
import org.xwiki.script.service.ScriptService;

/**
 * Entry point of extension manager from scripts.
 * <p>
 * Namespaces are ways to isolate extensions in a particular context, they are generally prefixed with the type of
 * context. For example to install an extension in a namespace linked to a particular wiki the namespace is prefixed
 * with <code>wiki:</code> which gives for the wiki <code>wiki1</code>: <code>wiki:wiki1</code>.
 * 
 * @version $Id$
 */
@Component
@Named("extension")
@Singleton
public class ExtensionManagerScriptService implements ScriptService
{
    /** The key under which the last encountered error is stored in the current execution context. */
    private static final String EXTENSIONERROR_KEY = "scriptservice.extension.error";

    /** The real extension manager bridged by this script service. */
    @Inject
    private ExtensionManager extensionManager;

    /** Needed for checking programming rights. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** The repository with custom installed extensions. */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /** The repository with core modules provided by the platform. */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /** Repository manager, needed for cross-repository operations. */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /** Handles and provides status feedback on extension operations (installation, upgrade, removal). */
    @Inject
    private JobManager jobManager;

    /** Provides access to the current context. */
    @Inject
    private Execution execution;

    // Extensions

    /**
     * Search among all {@link org.xwiki.extension.repository.search.Searchable} repositories for extensions matching
     * the search terms.
     * 
     * @param pattern the words to search for
     * @param offset the offset from where to start returning search results, 0-based
     * @param nb the maximum number of search results to return. -1 indicate no limit. 0 indicate that no result will be
     *            returned but it can be used to get the total hits.
     * @return the found extensions descriptors, empty list if nothing could be found
     * @see org.xwiki.extension.repository.search.Searchable
     */
    public IterableResult<Extension> search(String pattern, int offset, int nb)
    {
        return this.repositoryManager.search(pattern, offset, nb);
    }

    /**
     * Get the extension handler corresponding to the given extension ID and version. The returned handler can be used
     * to get more information about the extension, such as the authors, an extension description, its license...
     * 
     * @param id the extension id or provided feature (virtual extension) of the extension to resolve
     * @param version the specific version to resolve
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension couldn't
     *         be resolved, in which case {@link #getLastError()} contains the failure reason
     */
    public Extension resolve(String id, String version)
    {
        setError(null);

        Extension extension = null;

        try {
            extension =
                UnmodifiableUtils.unmodifiableExtension(this.extensionManager.resolveExtension(new ExtensionId(id,
                    version)));
        } catch (Exception e) {
            setError(e);
        }

        return extension;
    }

    /**
     * Get the extension handler corresponding to the given extension ID and version. The returned handler can be used
     * to get more information about the extension, such as the authors, an extension description, its license...
     * 
     * @param extensionDependency the extension dependency to resolve
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension couldn't
     *         be resolved, in which case {@link #getLastError()} contains the failure reason
     */
    public Extension resolve(ExtensionDependency extensionDependency)
    {
        setError(null);

        Extension extension = null;

        try {
            extension =
                UnmodifiableUtils.unmodifiableExtension(this.extensionManager.resolveExtension(extensionDependency));
        } catch (Exception e) {
            setError(e);
        }

        return extension;
    }

    /**
     * Return ordered (ascendent) versions for the provided extension id.
     * 
     * @param id the id of the extensions for which to return versions
     * @param offset the offset from where to start returning versions
     * @param nb the maximum number of versions to return
     * @return the versions of the provided extension id
     * @throws ResolveException fail to find extension for provided id
     */
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        setError(null);

        IterableResult<Version> versions = null;

        try {
            versions = this.repositoryManager.resolveVersions(id, offset, nb);
        } catch (Exception e) {
            setError(e);
        }

        return versions;
    }

    /**
     * Get a list of all currently installed extensions. This doesn't include core extensions, only custom extensions
     * installed by the administrators.
     * 
     * @return a list of read-only handlers corresponding to the installed extensions, an empty list if nothing is
     *         installed
     */
    public Collection<LocalExtension> getInstalledExtensions()
    {
        return UnmodifiableUtils.unmodifiableExtensions(this.localExtensionRepository.getInstalledExtensions());
    }

    /**
     * Return all the extensions available for the provide namespace. This also include root extension since namespaces
     * inherit from root.
     * <p>
     * This doesn't include core extensions, only extension installed through the API.
     * 
     * @param namespace the target namespace for which to retrieve the list of installed extensions
     * @return a list of read-only handlers corresponding to the installed extensions, an empty list if nothing is
     *         installed in the target namespace
     */
    public Collection<LocalExtension> getInstalledExtensions(String namespace)
    {
        return UnmodifiableUtils
            .unmodifiableExtensions(this.localExtensionRepository.getInstalledExtensions(namespace));
    }

    /**
     * Get the extension handler corresponding to the given installed extension ID or feature (virtual ID) provided by
     * the extension and namespace.
     * <p>
     * The returned handler can be used to get more information about the extension, such as the authors, an extension
     * description, its license...
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @param namespace the optional namespace where the extension should be installed
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension isn't
     *         installed in the target namespace
     */
    public LocalExtension getInstalledExtension(String feature, String namespace)
    {
        return UnmodifiableUtils.unmodifiableExtension(this.localExtensionRepository.getInstalledExtension(feature,
            namespace));
    }

    /**
     * Get a list of core extensions provided by the current version of the platform.
     * 
     * @return a list of read-only handlers corresponding to the core extensions
     */
    public Collection<CoreExtension> getCoreExtensions()
    {
        return UnmodifiableUtils.unmodifiableExtensions(this.coreExtensionRepository.getCoreExtensions());
    }

    /**
     * Get the extension handler corresponding to the given core extension ID. The returned handler can be used to get
     * more information about the extension, such as the authors, an extension description, its license...
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension isn't
     *         provided by the platform
     */
    public CoreExtension getCoreExtension(String feature)
    {
        return UnmodifiableUtils.unmodifiableExtension(this.coreExtensionRepository.getCoreExtension(feature));
    }

    /**
     * Get a list of cached extensions from the local extension repository. This doesn't include core extensions, only
     * custom extensions fetched or installed.
     * 
     * @return a list of read-only handlers corresponding to the local extensions, an empty list if nothing is available
     *         in the local repository
     */
    public Collection<LocalExtension> getLocalExtensions()
    {
        return UnmodifiableUtils.unmodifiableExtensions(this.localExtensionRepository.getLocalExtensions());
    }

    /**
     * Get all the installed extensions that depend on the specified extension. The results are grouped by namespace, so
     * the same extension can appear multiple times, once for each namespace where it is installed.
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @param version the specific version to check
     * @return a map namespace -&gt; list of dependent extensions, or {@code null} if any error occurs while computing
     *         the result, in which case {@link #getLastError()} contains the failure reason
     */
    public Map<String, Collection<LocalExtension>> getBackwardDependencies(String feature, String version)
    {
        setError(null);

        Map<String, Collection<LocalExtension>> extensions;

        try {
            extensions =
                UnmodifiableUtils.unmodifiableExtensions(this.localExtensionRepository
                    .getBackwardDependencies(new ExtensionId(feature, version)));
        } catch (Exception e) {
            setError(e);

            extensions = null;
        }

        return extensions;
    }

    // Actions

    /**
     * Start the asynchronous installation process for an extension if the context document has programming rights and
     * no other job is in progress already.
     * 
     * @param id the identifier of the extension to install
     * @param version the version to install
     * @param namespace the (optional) namespace where to install the extension; if {@code null} or empty, the extension
     *            will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job install(String id, String version, String namespace)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to install an extension"));

            return null;
        }

        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(id, version));
        if (StringUtils.isNotBlank(namespace)) {
            installRequest.addNamespace(namespace);
        }

        Job job;
        try {
            job = this.jobManager.install(installRequest);
        } catch (JobException e) {
            setError(e);

            job = null;
        }

        return job;
    }

    /**
     * Start the asynchronous installation plan creation process for an extension if no other job is in progress
     * already.
     * 
     * @param id the identifier of the extension to install
     * @param version the version to install
     * @param namespace the (optional) namespace where to install the extension; if {@code null} or empty, the extension
     *            will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public ExtensionPlan createInstallPlan(String id, String version, String namespace)
    {
        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(id, version));
        if (StringUtils.isNotBlank(namespace)) {
            installRequest.addNamespace(namespace);
        }

        ExtensionPlan status;
        try {
            status =
                new UnmodifiableExtensionPlan((ExtensionPlan) this.jobManager.executeJob("installplan", installRequest)
                    .getStatus());
        } catch (JobException e) {
            setError(e);

            status = null;
        }

        return status;
    }

    /**
     * Start the asynchronous uninstall process for an extension if the context document has programming rights and no
     * other job is in progress already.
     * 
     * @param id the identifier of the extension to remove
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the uninstallation process, or
     *         {@code null} in case of failure
     */
    public Job uninstall(String id, String namespace)
    {
        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to uninstall an extension"));

            return null;
        }

        setError(null);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(new ExtensionId(id, (Version) null));
        if (StringUtils.isNotBlank(namespace)) {
            uninstallRequest.addNamespace(namespace);
        }

        Job job;
        try {
            job = this.jobManager.uninstall(uninstallRequest);
        } catch (Exception e) {
            setError(e);

            job = null;
        }

        return job;
    }

    /**
     * Start the asynchronous uninstallation plan creation process for an extension if no other job is in progress
     * already.
     * 
     * @param id the identifier of the extension to install
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public ExtensionPlan createUninstallPlan(String id, String namespace)
    {
        setError(null);

        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(new ExtensionId(id, (Version) null));
        if (StringUtils.isNotBlank(namespace)) {
            uninstallRequest.addNamespace(namespace);
        }

        ExtensionPlan status;
        try {
            status =
                new UnmodifiableExtensionPlan((ExtensionPlan) this.jobManager.executeJob("uninstallplan",
                    uninstallRequest).getStatus());
        } catch (JobException e) {
            setError(e);

            status = null;
        }

        return status;
    }

    // Jobs

    /**
     * Get a reference to the currently executing job, if any.
     * 
     * @return currently executing job, or {@code null} if no job is being executed
     */
    public Job getCurrentJob()
    {
        setError(null);

        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to get current job"));
            return null;
        }

        return this.jobManager.getCurrentJob();
    }

    /**
     * Get the status of the currently executing job, if any.
     * 
     * @return status of the currently executing job, or {@code null} if no job is being executed
     */
    public JobStatus getCurrentJobStatus()
    {
        Job job = this.jobManager.getCurrentJob();
        JobStatus jobStatus;
        if (job != null) {
            jobStatus = job.getStatus();
            if (!this.documentAccessBridge.hasProgrammingRights()) {
                jobStatus = new UnmodifiableJobStatus<JobStatus>(jobStatus);
            }
        } else {
            jobStatus = null;
        }

        return jobStatus;
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(EXTENSIONERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(EXTENSIONERROR_KEY, e);
    }

    /**
     * @param version the string to parse
     * @return the {@link Version} instance
     */
    public Version parseVersion(String version)
    {
        return new DefaultVersion(version);
    }

    /**
     * @param versionRange the string to parse
     * @return the {@link VersionRange} instance
     */
    public VersionRange parseVersionRange(String versionRange)
    {
        setError(null);

        try {
            return new DefaultVersionRange(versionRange);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param versionConstraint the string to parse
     * @return the {@link VersionConstraint} instance
     */
    public VersionConstraint parseVersionConstraint(String versionConstraint)
    {
        setError(null);

        try {
            return new DefaultVersionConstraint(versionConstraint);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }
}
