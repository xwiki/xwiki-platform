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
package org.xwiki.extension.xar.script;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.AbstractExtensionScriptService;
import org.xwiki.extension.script.ExtensionManagerScriptService;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobManager;

/**
 * Various XAR oriented APIs for scripts.
 * 
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named("extension.xar")
@Singleton
public class XarExtensionScriptService extends AbstractExtensionScriptService
{
    /**
     * Needed for checking programming rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Handles and provides status feedback on extension operations (installation, upgrade, removal).
     */
    @Inject
    private JobManager jobManager;

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
