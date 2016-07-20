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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.AbstractExtensionScriptService;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.xar.internal.job.DiffXarJob;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

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
     * The install request property that specifies which user triggered the XAR repair job.
     */
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

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

        String namespace = getWikiNamespace(wiki);

        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, id, namespace));
        DocumentReference currentUserReference = this.documentAccessBridge.getCurrentUserReference();
        if (currentUserReference != null) {
            installRequest.setProperty(PROPERTY_USER_REFERENCE, currentUserReference);
            // We set the string value because the extension repository doesn't know how to serialize/parse an extension
            // property whose value is a DocumentReference, and adding support for it requires considerable refactoring
            // because ExtensionPropertySerializers are not components (they are currently hard-coded).
            installRequest.setExtensionProperty(PROPERTY_USER_REFERENCE, currentUserReference.toString());
        }
        installRequest.addExtension(new ExtensionId(id, version));
        if (StringUtils.isNotBlank(namespace)) {
            installRequest.addNamespace(namespace);
        }

        Job job = null;
        try {
            job = this.jobExecutor.execute(RepairXarJob.JOBTYPE, installRequest);
        } catch (Exception e) {
            setError(e);
        }

        return job;
    }

    /**
     * Computes the differences, in unified format, between the documents of an installed XAR extension and the document
     * from the wiki.
     * 
     * @param feature the identifier of a XAR extension (or one of its features)
     * @param wiki the wiki where the XAR extension is installed
     * @return the {@link Job} object which can be used to monitor the progress while the differences are being
     *         computed, or {@code null} in case of failure
     * @since 7.0RC1
     */
    @Unstable
    public Job diff(String feature, String wiki)
    {
        setError(null);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(feature, (Version) null));
        if (StringUtils.isNotBlank(wiki)) {
            installRequest.addNamespace(getWikiNamespace(wiki));
        }
        installRequest.setId(getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, feature,
            installRequest.hasNamespaces() ? installRequest.getNamespaces().iterator().next() : null));

        try {
            return this.jobExecutor.execute(DiffXarJob.JOB_TYPE, installRequest);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    private String getWikiNamespace(String wiki)
    {
        return "wiki:" + wiki;
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
            jobId = Arrays.asList(ExtensionRequest.JOBID_PREFIX, prefix, extensionId, namespace);
        } else {
            jobId = Arrays.asList(ExtensionRequest.JOBID_PREFIX, prefix, extensionId);
        }

        return jobId;
    }
}
