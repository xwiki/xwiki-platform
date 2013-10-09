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
package org.xwiki.extension.xar.internal.script;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.safe.ScriptSafeProvider;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobManager;
import org.xwiki.script.service.ScriptService;

/**
 * Various XAR oriented APIs for scripts.
 * 
 * @version $Id$
 */
@Component
@Named("xarextension")
@Singleton
public class XarExtensionScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String EXTENSIONERROR_KEY = "scriptservice.xarextension.error";

    /**
     * Needed for checking programming rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * Provide safe wrapper for objects.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider scriptProvider;

    /**
     * Handles and provides status feedback on extension operations (installation, upgrade, removal).
     */
    @Inject
    private JobManager jobManager;

    /**
     * Used to manipulate the installed xar extensions.
     */
    @Inject
    private InstalledExtensionRepository xarRepository;

    /**
     * @param <T> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the passed object
     */
    @SuppressWarnings("unchecked")
    private <T> T safe(T unsafe)
    {
        return (T) this.scriptProvider.get(unsafe);
    }

    // Error management

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
     * Make sure the provided XAR extension properly is registered in the installed extensions index.
     * <p>
     * Start an asynchronous Job.
     * 
     * @param id the extension identifier
     * @param version the extension version
     * @param wiki the wiki where the extension is installed
     * @return the {@link Job} object which can be used to monitor the progress of the installation process, or
     *         {@code null} in case of failure
     */
    public Job repairInstalledExtension(String id, String version, String wiki)
    {
        setError(null);

        if (!this.documentAccessBridge.hasProgrammingRights()) {
            setError(new JobException("Need programming right to repair a XAR"));
            return null;
        }

        String namespace = "wiki:" + wiki;

        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(getJobId(ExtensionManagerScriptService.EXTENSIONACTION_JOBID_PREFIX, id, namespace));
        installRequest.addExtension(new ExtensionId(id, version));
        if (StringUtils.isNotBlank(namespace)) {
            installRequest.addNamespace(namespace);
        }

        Job job = null;
        try {
            job = this.jobManager.addJob(RepairXarJob.JOBTYPE, installRequest);
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * @param prefix the type of job
     * @param extensionId the identifier of the installed extension
     * @param namespace the namespace where to install
     * @return the job identifier
     */
    private List<String> getJobId(String prefix, String extensionId, String namespace)
    {
        List<String> jobId;

        if (namespace != null) {
            jobId = Arrays.asList(ExtensionManagerScriptService.EXTENSION_JOBID_PREFIX, prefix, extensionId, namespace);
        } else {
            jobId = Arrays.asList(ExtensionManagerScriptService.EXTENSION_JOBID_PREFIX, prefix, extensionId);
        }

        return jobId;
    }
}
