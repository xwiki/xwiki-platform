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
package org.xwiki.extension.script;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceValidator;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.InstallPlanJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.job.internal.UninstallPlanJob;
import org.xwiki.extension.job.internal.UpgradePlanJob;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.VersionRange;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersionRange;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Entry point of extension manager from scripts.
 * <p>
 * Namespaces are ways to isolate extensions in a particular context, they are generally prefixed with the type of
 * context. For example to install an extension in a namespace linked to a particular wiki the namespace is prefixed
 * with <code>wiki:</code> which gives for the wiki <code>wiki1</code>: <code>wiki:wiki1</code>.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Named(ExtensionManagerScriptService.ROLEHINT)
@Singleton
public class ExtensionManagerScriptService extends AbstractExtensionScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "extension";

    /**
     * The prefix put behind all job ids.
     * 
     * @deprecated since 8.2RC1, use {@link ExtensionRequest#JOBID_PREFIX} instead
     */
    @Deprecated
    public static final String EXTENSION_JOBID_PREFIX = ROLEHINT;

    /**
     * The prefix put behind all job ids which are actual actions.
     * 
     * @deprecated since 8.2RC1, use {@link ExtensionRequest#JOBID_ACTION_PREFIX} instead
     */
    @Deprecated
    public static final String EXTENSIONACTION_JOBID_PREFIX = "action";

    /**
     * The prefix put behind all job ids which are information gathering.
     * 
     * @deprecated since 8.2RC1, use {@link ExtensionRequest#JOBID_PLAN_PREFIX} instead
     */
    @Deprecated
    public static final String EXTENSIONPLAN_JOBID_PREFIX = "plan";

    /**
     * This property is set on requests to create an install or uninstall plan in order to specify which type of job
     * generated the plan.
     */
    private static final String PROPERTY_JOB_TYPE = "job.type";

    /**
     * The real extension manager bridged by this script service.
     */
    @Inject
    private ExtensionManager extensionManager;

    /**
     * Repository manager, needed for cross-repository operations.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private NamespaceValidator namespaceResolver;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ExtensionManagerScriptService.ROLEHINT + '.' + serviceName);
    }

    // Repositories

    /**
     * @return all the remote repositories
     */
    public Collection<ExtensionRepository> getRepositories()
    {
        return safe(this.repositoryManager.getRepositories());
    }

    /**
     * @param repositoryId the identifier of the remote repository
     * @return the repository
     */
    public ExtensionRepository getRepository(String repositoryId)
    {
        return safe(this.extensionManager.getRepository(repositoryId));
    }

    // Extensions

    /**
     * Search among all remote (those listed in xwiki.properties)
     * {@link org.xwiki.extension.repository.search.Searchable} repositories for extensions matching the search terms.
     * 
     * @param pattern the words to search for
     * @param offset the offset from where to start returning search results, 0-based
     * @param nb the maximum number of search results to return. -1 indicate no limit. 0 indicate that no result will be
     *            returned but it can be used to get the total hits.
     * @return the found extensions descriptors, empty list if nothing could be found and null if an expected error has
     *         been catched
     * @see org.xwiki.extension.repository.search.Searchable
     */
    public IterableResult<Extension> search(String pattern, int offset, int nb)
    {
        setError(null);

        IterableResult<Extension> result = null;

        try {
            return this.repositoryManager.search(pattern, offset, nb);
        } catch (Exception e) {
            setError(e);
        }

        return result;
    }

    /**
     * Search among all remote (those listed in xwiki.properties)
     * {@link org.xwiki.extension.repository.search.AdvancedSearchable} repositories for extensions matching the search
     * query.
     * 
     * @param query the search query
     * @return the found extensions descriptors, empty list if nothing could be found and null if an expected error has
     *         been catched
     * @see org.xwiki.extension.repository.search.Searchable
     * @since 7.1RC1
     */
    public IterableResult<Extension> search(ExtensionQuery query)
    {
        setError(null);

        IterableResult<Extension> result = null;

        try {
            result = this.repositoryManager.search(query);
        } catch (Exception e) {
            setError(e);
        }

        return result;
    }

    /**
     * Create a new instance of a {@link ExtensionQuery} to be used in other APIs.
     * 
     * @param query the query to execute
     * @return a {@link ExtensionQuery} instance
     * @since 7.1RC1
     */
    public ExtensionQuery newQuery(String query)
    {
        return new ExtensionQuery(query);
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
            extension = safe(this.extensionManager.resolveExtension(new ExtensionId(id, version)));
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
     * @since 3.4M1
     * @deprecated since 5.3M1, use {@link #resolve(ExtensionDependency, String)} instead
     */
    @Deprecated
    public Extension resolve(ExtensionDependency extensionDependency)
    {
        setError(null);

        Extension extension = null;

        try {
            extension = safe(this.extensionManager.resolveExtension(extensionDependency));
        } catch (Exception e) {
            setError(e);
        }

        return extension;
    }

    /**
     * Search the provided extension as a dependency of another extension among all repositories including core and
     * local repositories.
     * <p>
     * The search is done in the following order:
     * <ul>
     * <li>Is it a core extension ?</li>
     * <li>Is it a local extension ?</li>
     * <li>Is this feature installed in current namespace or parent ?</li>
     * <li>Is it a remote extension in one of the configured remote repositories ?</li>
     * </ul>
     * The first one found is returned.
     * 
     * @param extensionDependency the extension dependency to resolve
     * @param namespace the namespace where to search for the dependency
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension couldn't
     *         be resolved, in which case {@link #getLastError()} contains the failure reason
     * @since 5.3M1
     */
    public Extension resolve(ExtensionDependency extensionDependency, String namespace)
    {
        setError(null);

        Extension extension = null;

        try {
            extension = safe(this.extensionManager.resolveExtension(extensionDependency, namespace));
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
     */
    public IterableResult<Version> resolveVersions(String id, int offset, int nb)
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

    // Actions

    /**
     * Create an {@link InstallRequest} instance based on passed parameters.
     * 
     * @param id the identifier of the extension to install
     * @param version the version to install
     * @param namespace the (optional) namespace where to install the extension; if {@code null} or empty, the extension
     *            will be installed globally
     * @return the {@link InstallRequest}
     */
    public InstallRequest createInstallRequest(String id, String version, String namespace)
    {
        InstallRequest installRequest = createInstallPlanRequest(id, version, namespace);

        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, id, namespace));
        installRequest.setInteractive(true);
        installRequest.setProperty(PROPERTY_JOB_TYPE, InstallJob.JOBTYPE);
        DocumentReference currentUserReference = this.documentAccessBridge.getCurrentUserReference();
        if (currentUserReference != null) {
            // We set the string value because the extension repository doesn't know how to serialize/parse an extension
            // property whose value is a DocumentReference, and adding support for it requires considerable refactoring
            // because ExtensionPropertySerializers are not components (they are currently hard-coded).
            installRequest.setExtensionProperty(PROPERTY_USERREFERENCE, currentUserReference.toString());
        }

        return installRequest;
    }

    /**
     * Start the asynchronous installation process for an extension if the context document has programming rights.
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
        return install(createInstallRequest(id, version, namespace));
    }

    /**
     * Start the asynchronous installation process for an extension if the context document has programming rights.
     * 
     * @param installRequest installation instructions
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job install(InstallRequest installRequest)
    {
        setError(null);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Make sure only PR user can remove the right checking or change the users
            setRightsProperties(installRequest);
        }

        Job job = null;
        try {
            job = this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Create an {@link InstallRequest} instance based on given parameters, to be used to create the install plan.
     * 
     * @param id the identifier of the extension to install
     * @param version the version to install
     * @param namespace the (optional) namespace where to install the extension; if {@code null} or empty, the extension
     *            will be installed globally
     * @return the {@link InstallRequest}
     */
    public InstallRequest createInstallPlanRequest(String id, String version, String namespace)
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, id, namespace));
        installRequest.addExtension(new ExtensionId(id, version));
        if (StringUtils.isNotBlank(namespace)) {
            installRequest.addNamespace(namespace);
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        // Indicate if it's allowed to do modification on root namespace
        installRequest.setRootModificationsAllowed(namespace == null || xcontext.isMainWiki(toWikiId(namespace)));

        // Provide informations on what started the job
        installRequest.setProperty(PROPERTY_CONTEXT_WIKI, xcontext.getWikiId());
        installRequest.setProperty(PROPERTY_CONTEXT_ACTION, xcontext.getAction());

        setRightsProperties(installRequest);

        installRequest.setProperty(PROPERTY_JOB_TYPE, InstallPlanJob.JOBTYPE);

        return installRequest;
    }

    /**
     * Start the asynchronous installation plan creation process for an extension.
     * 
     * @param installRequest installation instructions
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job createInstallPlan(InstallRequest installRequest)
    {
        setError(null);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Make sure only PR user can remove the right checking or change the users
            setRightsProperties(installRequest);
        }

        Job job = null;
        try {
            job = this.jobExecutor.execute(InstallPlanJob.JOBTYPE, installRequest);
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Start the asynchronous installation plan creation process for an extension.
     * 
     * @param id the identifier of the extension to install
     * @param version the version to install
     * @param namespace the (optional) namespace where to install the extension; if {@code null} or empty, the extension
     *            will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job createInstallPlan(String id, String version, String namespace)
    {
        return createInstallPlan(createInstallPlanRequest(id, version, namespace));
    }

    /**
     * Start the asynchronous uninstall process for an extension if the context document has programming rights.
     * <p>
     * Only uninstall from the provided namespace.
     * 
     * @param id the identifier of the extension to remove
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be installed globally
     * @return the {@link Job} object which can be used to monitor the progress of the uninstallation process, or
     *         {@code null} in case of failure
     */
    public Job uninstall(String id, String namespace)
    {
        return uninstall(createUninstallRequest(id, namespace));
    }

    /**
     * Start the asynchronous uninstall process for an extension if the context document has programming rights.
     * <p>
     * Uninstall from all namespaces.
     * 
     * @param extensionId the identifier of the extension to remove
     * @return the {@link Job} object which can be used to monitor the progress of the uninstallation process, or
     *         {@code null} in case of failure
     */
    public Job uninstall(ExtensionId extensionId)
    {
        return uninstall(createUninstallRequest(extensionId, null));
    }

    /**
     * Adds a new job to the job queue to perform the given uninstall request.
     * <p>
     * This method requires programming rights.
     * 
     * @param uninstallRequest the uninstall request to perform
     * @return the {@link Job} object which can be used to monitor the progress of the uninstall process, or
     *         {@code null} in case of failure
     */
    public Job uninstall(UninstallRequest uninstallRequest)
    {
        setError(null);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Make sure only PR user can remove the right checking or change the users
            setRightsProperties(uninstallRequest);
        }

        Job job = null;
        try {
            job = this.jobExecutor.execute(UninstallJob.JOBTYPE, uninstallRequest);
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Create an {@link UninstallRequest} instance based on passed parameters.
     * 
     * @param id the identifier of the extension to uninstall
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be uninstalled globally
     * @return the {@link UninstallRequest}
     */
    public UninstallRequest createUninstallRequest(String id, String namespace)
    {
        return createUninstallRequest(new ExtensionId(id, (Version) null), namespace);
    }

    private UninstallRequest createUninstallRequest(ExtensionId extensionId, String namespace)
    {
        UninstallRequest uninstallRequest = createUninstallPlanRequest(extensionId, namespace);

        uninstallRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId.getId(), namespace));
        uninstallRequest.setInteractive(true);
        uninstallRequest.setProperty(PROPERTY_JOB_TYPE, UninstallJob.JOBTYPE);

        return uninstallRequest;
    }

    /**
     * Create an {@link UninstallRequest} instance based on passed parameters.
     * 
     * @param extensionId the identifier of the extension to uninstall
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be uninstalled globally
     * @return the {@link UninstallRequest}
     */
    private UninstallRequest createUninstallPlanRequest(ExtensionId extensionId, String namespace)
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest
            .setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, extensionId.getId(), namespace));
        uninstallRequest.addExtension(extensionId);
        if (StringUtils.isNotBlank(namespace)) {
            uninstallRequest.addNamespace(namespace);
        }

        // Indicate if it's allowed to do modification on root namespace
        uninstallRequest.setRootModificationsAllowed(
            namespace == null || this.xcontextProvider.get().isMainWiki(toWikiId(namespace)));

        // Provide informations on what started the job
        uninstallRequest.setProperty(PROPERTY_CONTEXT_WIKI, this.xcontextProvider.get().getWikiId());
        uninstallRequest.setProperty(PROPERTY_CONTEXT_ACTION, this.xcontextProvider.get().getAction());

        setRightsProperties(uninstallRequest);

        uninstallRequest.setProperty(PROPERTY_JOB_TYPE, UninstallPlanJob.JOBTYPE);

        return uninstallRequest;
    }

    /**
     * Start the asynchronous uninstallation plan creation process for an extension.
     * <p>
     * Only uninstall from the provided namespace.
     * 
     * @param id the identifier of the extension that is going to be removed
     * @param namespace the (optional) namespace from where to uninstall the extension; if {@code null} or empty, the
     *            extension will be removed from all namespaces
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job createUninstallPlan(String id, String namespace)
    {
        return createUninstallPlan(createUninstallPlanRequest(new ExtensionId(id, (Version) null), namespace));
    }

    /**
     * Start the asynchronous uninstallation plan creation process for an extension if no other job is in progress
     * already.
     * <p>
     * Uninstall from all namespaces.
     * 
     * @param extensionId the identifier of the extension that is going to be removed
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job createUninstallPlan(ExtensionId extensionId)
    {
        return createUninstallPlan(createUninstallPlanRequest(extensionId, null));
    }

    /**
     * Adds a new job to the job queue to perform the given uninstall plan request.
     * <p>
     * This method requires programming rights.
     * 
     * @param uninstallRequest the uninstall plan request to perform
     * @return the {@link Job} object which can be used to monitor the progress of the uninstall plan process, or
     *         {@code null} in case of failure
     */
    private Job createUninstallPlan(UninstallRequest uninstallRequest)
    {
        setError(null);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            // Make sure only PR user can remove the right checking or change the users
            setRightsProperties(uninstallRequest);
        }

        Job job = null;
        try {
            job = this.jobExecutor.execute(UninstallPlanJob.JOBTYPE, uninstallRequest);
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Create the default request used when asking for the upgrade plan on a namespace.
     * 
     * @param namespace the namespace to upgrade
     * @return the request to pass t the job
     */
    public InstallRequest createUpgradePlanRequest(String namespace)
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, null, namespace));
        installRequest.addNamespace(namespace);

        // Provide informations on what started the job
        installRequest.setProperty(PROPERTY_CONTEXT_WIKI, this.xcontextProvider.get().getWikiId());
        installRequest.setProperty(PROPERTY_CONTEXT_ACTION, this.xcontextProvider.get().getAction());

        return installRequest;
    }

    private InstallRequest createUpgradePlanRequest()
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, null, null));

        // Provide informations on what started the job
        installRequest.setProperty(PROPERTY_CONTEXT_WIKI, this.xcontextProvider.get().getWikiId());
        installRequest.setProperty(PROPERTY_CONTEXT_ACTION, this.xcontextProvider.get().getAction());

        return installRequest;
    }

    /**
     * Schedule the upgrade plan creation job.
     * 
     * @param request the request to pass to pass to the upgrade plan job
     * @return the {@link Job} object which can be used to monitor the progress of the upgrade plan creation process, or
     *         {@code null} in case of failure
     */
    public Job createUpgradePlan(InstallRequest request)
    {
        request.setProperty(PROPERTY_USERREFERENCE, this.documentAccessBridge.getCurrentUserReference());
        XWikiDocument callerDocument = getCallerDocument();
        if (callerDocument != null) {
            request.setProperty(PROPERTY_CALLERREFERENCE, callerDocument.getContentAuthorReference());
        }

        request.setProperty(PROPERTY_CHECKRIGHTS, true);

        Job job = null;
        try {
            job = safe(this.jobExecutor.execute(UpgradePlanJob.JOBTYPE, request));
        } catch (JobException e) {
            setError(e);
        }

        return job;
    }

    /**
     * Start the asynchronous upgrade plan creation process for the provided namespace.
     * 
     * @param namespace the namespace where to upgrade the extensions
     * @return the {@link Job} object which can be used to monitor the progress of the plan creation process, or
     *         {@code null} in case of failure
     */
    public Job createUpgradePlan(String namespace)
    {
        setError(null);

        return createUpgradePlan(createUpgradePlanRequest(namespace));
    }

    /**
     * Start the asynchronous upgrade plan creation process for all the namespaces.
     * 
     * @return the {@link Job} object which can be used to monitor the progress of the plan creation process, or
     *         {@code null} in case of failure
     */
    public Job createUpgradePlan()
    {
        setError(null);

        return createUpgradePlan(createUpgradePlanRequest());
    }

    // Jobs

    /**
     * Get a reference to the currently job executed.
     * <p>
     * Current here basically means the extension related job that is going to block any new job that would be
     * associated to the current namespace.
     * 
     * @return currently executing job, or {@code null} if no job is being executed
     */
    public Job getCurrentJob()
    {
        setError(null);

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            setError(new JobException("You need programming rights to get the current job."));
            return null;
        }

        return getCurrentJobInternal();
    }

    private Job getCurrentJobInternal()
    {
        // TODO: probably check current user namespace

        // Check current wiki namespace
        Job job = this.jobExecutor.getCurrentJob(new JobGroupPath(
            fromWikitoNamespace(this.xcontextProvider.get().getWikiId()), AbstractExtensionJob.ROOT_GROUP));

        // Check root namespace
        if (job == null) {
            job = this.jobExecutor.getCurrentJob(AbstractExtensionJob.ROOT_GROUP);
        }

        return job;
    }

    /**
     * Return job status corresponding to the provided extension id from the current executed job or stored history.
     * 
     * @param extensionId the extension identifier
     * @param namespace the namespace where the job is being or has been executed
     * @return the job status corresponding to the provided extension
     */
    public JobStatus getExtensionJobStatus(String extensionId, String namespace)
    {
        return getJobStatus(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, extensionId, namespace));
    }

    /**
     * Return extension plan corresponding to the provided extension id from the current executed job or stored history.
     * 
     * @param extensionId the extension identifier
     * @param namespace the namespace where the job is being or has been executed
     * @return the extension plan corresponding to the provided extension
     */
    public JobStatus getExtensionPlanJobStatus(String extensionId, String namespace)
    {
        return getJobStatus(ExtensionRequest.getJobId(ExtensionRequest.JOBID_PLAN_PREFIX, extensionId, namespace));
    }

    /**
     * Get the status of the currently executing job, if any.
     * 
     * @return status of the currently executing job, or {@code null} if no job is being executed
     */
    public JobStatus getCurrentJobStatus()
    {
        Job job = getCurrentJobInternal();

        JobStatus jobStatus;
        if (job != null) {
            jobStatus = job.getStatus();
            if (!this.authorization.hasAccess(Right.PROGRAM)) {
                jobStatus = safe(jobStatus);
            }
        } else {
            jobStatus = null;
        }

        return jobStatus;
    }

    // Version management

    /**
     * @param version the string to parse
     * @return the {@link Version} instance
     * @since 3.4M1
     */
    public Version parseVersion(String version)
    {
        return new DefaultVersion(version);
    }

    /**
     * @param versionRange the string to parse
     * @return the {@link VersionRange} instance
     * @since 3.4M1
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
     * @since 3.4M1
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

    /**
     * Creates an extension dependency object.
     * 
     * @param id the dependency identifier
     * @param versionConstraint the dependency version constraint
     * @return the extension dependency object
     */
    public ExtensionDependency createExtensionDependency(String id, String versionConstraint)
    {
        setError(null);

        try {
            return new DefaultExtensionDependency(id, new DefaultVersionConstraint(versionConstraint));
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param allowedNamespaces the allowed dynamic (or not) namespaces, null matches any namespace
     * @param namespace to validate against passed allowed namespaces
     * @return the namespace(s) corresponding to the passed dynamic namespaces
     * @since 8.0M1
     */
    public boolean isAllowed(Collection<String> allowedNamespaces, String namespace)
    {
        return this.namespaceResolver.isAllowed(allowedNamespaces, namespace);
    }

    /**
     * @param extension the extension to check with the passed namespace
     * @param namespace to validate against passed allowed namespaces
     * @return the namespace(s) corresponding to the passed dynamic namespaces
     * @since 8.0M1
     */
    public boolean isAllowed(Extension extension, String namespace)
    {
        return this.namespaceResolver.isAllowed(extension.getAllowedNamespaces(), namespace);
    }

    // Deprecated (generally moved to dedicated script services)

    /**
     * Get a list of all currently installed extensions. This doesn't include core extensions, only custom extensions
     * installed by the administrators.
     * 
     * @return a list of read-only handlers corresponding to the installed extensions, an empty list if nothing is
     *         installed
     * @deprecated since 5.3M1, use {@link InstalledExtensionScriptService#getInstalledExtensions()} instead
     */
    @Deprecated
    public Collection<InstalledExtension> getInstalledExtensions()
    {
        return this.<InstalledExtensionScriptService>get(InstalledExtensionScriptService.ID).getInstalledExtensions();
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
     * @deprecated since 5.3M1, use {@link InstalledExtensionScriptService#getInstalledExtensions(String)} instead
     */
    @Deprecated
    public Collection<InstalledExtension> getInstalledExtensions(String namespace)
    {
        return this.<InstalledExtensionScriptService>get(InstalledExtensionScriptService.ID)
            .getInstalledExtensions(namespace);
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
     * @deprecated since 5.3M1, use {@link InstalledExtensionScriptService#getInstalledExtension(String, String)}
     *             instead
     */
    @Deprecated
    public InstalledExtension getInstalledExtension(String feature, String namespace)
    {
        return this.<InstalledExtensionScriptService>get(InstalledExtensionScriptService.ID)
            .getInstalledExtension(feature, namespace);
    }

    /**
     * Get all the installed extensions that depend on the specified extension. The results are grouped by namespace, so
     * the same extension can appear multiple times, once for each namespace where it is installed.
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @param version the specific version to check
     * @return a map namespace -&gt; list of dependent extensions, or {@code null} if any error occurs while computing
     *         the result, in which case {@link #getLastError()} contains the failure reason
     * @deprecated since 5.3M1, use {@link InstalledExtensionScriptService#getBackwardDependencies(String)} instead
     */
    @Deprecated
    public Map<String, Collection<InstalledExtension>> getBackwardDependencies(String feature, String version)
    {
        return this.<InstalledExtensionScriptService>get(InstalledExtensionScriptService.ID)
            .getBackwardDependencies(feature);
    }

    /**
     * Get a list of core extensions provided by the current version of the platform.
     * 
     * @return a list of read-only handlers corresponding to the core extensions
     * @deprecated since 5.3M1, use {@link CoreExtensionScriptService#getCoreExtensions()} instead
     */
    @Deprecated
    public Collection<CoreExtension> getCoreExtensions()
    {
        return this.<CoreExtensionScriptService>get(CoreExtensionScriptService.ID).getCoreExtensions();
    }

    /**
     * Get the extension handler corresponding to the given core extension ID. The returned handler can be used to get
     * more information about the extension, such as the authors, an extension description, its license...
     * 
     * @param feature the extension id or provided feature (virtual extension) of the extension to resolve
     * @return the read-only handler corresponding to the requested extension, or {@code null} if the extension isn't
     *         provided by the platform
     * @deprecated since 5.3M1, use {@link CoreExtensionScriptService#getCoreExtension(String)} instead
     */
    @Deprecated
    public CoreExtension getCoreExtension(String feature)
    {
        return this.<CoreExtensionScriptService>get(CoreExtensionScriptService.ID).getCoreExtension(feature);
    }

    /**
     * Get a list of cached extensions from the local extension repository. This doesn't include core extensions, only
     * custom extensions fetched or installed.
     * 
     * @return a list of read-only handlers corresponding to the local extensions, an empty list if nothing is available
     *         in the local repository
     * @deprecated since 5.3M1, use {@link LocalExtensionScriptService#getLocalExtensions()}
     */
    @Deprecated
    public Collection<LocalExtension> getLocalExtensions()
    {
        return this.<LocalExtensionScriptService>get(LocalExtensionScriptService.ID).getLocalExtensions();
    }
}
