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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.script.AbstractExtensionScriptService;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.xar.XarExtensionExtension;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.job.DiffXarJob;
import org.xwiki.extension.xar.internal.job.RepairXarJob;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.job.diff.DiffXarJobStatus;
import org.xwiki.extension.xar.job.diff.DocumentVersionReference;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.xar.XarException;

import com.xpn.xwiki.api.Document;

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

    @Inject
    private Packager packager;

    @Inject
    private AuthorizationManager genericAuthorization;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedXARs;

    private XarInstalledExtensionRepository getXarInstalledExtensionRepository()
    {
        return (XarInstalledExtensionRepository) this.installedXARs;
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

        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            setError(new JobException("Need programming right to repair a XAR"));
            return null;
        }

        String namespace = getWikiNamespace(wiki);

        InstallRequest installRequest = new InstallRequest();
        installRequest.setId(ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, id, namespace));
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
     * Get the id of the previously (or currently) computed differences, in unified format, between the documents of an
     * installed XAR extension and the document from the wiki..
     * 
     * @param feature the identifier of a XAR extension (or one of its features)
     * @param namespace the namespace where the XAR extension is installed
     * @return the id of the {@link Job}
     * @since 9.3RC1
     */
    public List<String> getDiffJobId(String feature, String namespace)
    {
        return ExtensionRequest.getJobId(ExtensionRequest.JOBID_ACTION_PREFIX, feature, namespace);
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
    public Job diff(String feature, String wiki)
    {
        setError(null);

        String namespace = getWikiNamespace(wiki);

        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(new ExtensionId(feature, (Version) null));
        if (namespace != null) {
            installRequest.addNamespace(namespace);
        }
        installRequest.setId(getDiffJobId(feature, namespace));

        try {
            return this.jobExecutor.execute(DiffXarJob.JOB_TYPE, installRequest);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    private String getWikiNamespace(String wiki)
    {
        return StringUtils.isNotBlank(wiki) ? "wiki:" + wiki : null;
    }

    /**
     * @return the possible conflicts
     * @since 9.2RC1
     */
    public ConflictQuestion.ConflictType[] getConflictTypes()
    {
        return ConflictQuestion.ConflictType.values();
    }

    /**
     * @param reference the reference of the document to reset to its standard state (what it looks like in the
     *            extension XAR)
     * @param extensionId the installed extension from which to get the standard version of the document
     * @param jobId the id of the job which computed the diff if any
     * @return true if the reset actually did something, false otherwise (any produced error can be accessed using
     *         {@link #getLastError()})
     * @since 9.3RC1
     */
    public boolean reset(DocumentReference reference, ExtensionId extensionId, List<String> jobId)
    {
        return reset(new DocumentVersionReference(reference, extensionId), jobId);
    }

    /**
     * @param reference the reference of the document to reset to its standard state (what it looks like in the
     *            extension XAR)
     * @param jobId the id of the job which computed the diff if any
     * @return true if the reset actually did something, false otherwise (any produced error can be accessed using
     *         {@link #getLastError()})
     * @since 9.3RC1
     */
    public boolean reset(DocumentReference reference, List<String> jobId)
    {
        setError(null);

        try {
            // Only current author is allowed to modify (and so reset) the target document
            this.genericAuthorization.checkAccess(Right.EDIT, this.xcontextProvider.get().getAuthorReference(),
                reference);

            // Reset the document in the DB
            this.packager.reset(reference, this.xcontextProvider.get().getUserReference());

            // Update the existing job status if any
            if (jobId != null) {
                JobStatus jobStatus = getJobStatus(jobId);
                if (jobStatus != null && jobStatus instanceof DiffXarJobStatus) {
                    ((DiffXarJobStatus) jobStatus).reset(reference);
                }
            }

            return true;
        } catch (Exception e) {
            setError(e);
        }

        return false;
    }

    /**
     * @param reference the reference of the document
     * @return the installed XAR extensions in which this document can be found
     * @since 9.3RC1
     */
    public Collection<InstalledExtension> getInstalledExtensions(DocumentReference reference)
    {
        return safe((Collection) getXarInstalledExtensionRepository().getXarInstalledExtensions(reference));
    }

    /**
     * @param reference the reference of the document
     * @return a Document instance of passed document when extracted from the standard extension matching this
     *         reference. Null if none could be found.
     * @throws XarExtensionExtension when failing to get the document
     * @since 9.3RC1
     */
    public Document getInstalledExtensionDocument(DocumentReference reference) throws XarExtensionExtension
    {
        try {
            return safe(this.packager.getXWikiDocument(reference));
        } catch (IOException | XarException e) {
            throw new XarExtensionExtension(String.format("Failed to get standard version of document [%s]", reference),
                e);
        }
    }

    /**
     * @param reference the reference of the document
     * @param extensionId the id of the extension from which to get the standard version of the document
     * @return a Document instance of passed document when extracted from the standard extension matching this
     *         reference. Null if none could be found.
     * @throws XarExtensionExtension when failing to get the document
     * @since 9.3RC1
     */
    public Document getInstalledExtensionDocument(DocumentReference reference, ExtensionId extensionId)
        throws XarExtensionExtension
    {
        try {
            return safe(this.packager.getXWikiDocument(reference, extensionId));
        } catch (IOException | XarException e) {
            throw new XarExtensionExtension(
                String.format("Failed to get standard version of document [%s] from extension with id [%s]", reference,
                    extensionId),
                e);
        }
    }

    /**
     * @param reference the reference of the document
     * @param extension the extension from which to get the standard version of the document
     * @return a Document instance of passed document when extracted from the standard extension matching this
     *         reference. Null if none could be found.
     * @throws XarExtensionExtension when failing to get the document
     * @since 9.3RC1
     */
    public Document getInstalledExtensionDocument(DocumentReference reference, XarInstalledExtension extension)
        throws XarExtensionExtension
    {
        try {
            return safe(this.packager.getXWikiDocument(reference, extension));
        } catch (IOException | XarException e) {
            throw new XarExtensionExtension(String.format(
                "Failed to get standard version of document [%s] from extension [%s]", reference, extension), e);
        }
    }

    /**
     * @param documentReference the reference of the document
     * @return true if edit is allowed on the passed document
     * @since 10.3RC1
     */
    public boolean isEditAllowed(DocumentReference documentReference)
    {
        return getXarInstalledExtensionRepository().isAllowed(documentReference, Right.EDIT);
    }

    /**
     * @param documentReference the reference of the document
     * @return true if edit is allowed on the passed document
     * @since 10.3RC1
     */
    public boolean isDeleteAllowed(DocumentReference documentReference)
    {
        return getXarInstalledExtensionRepository().isAllowed(documentReference, Right.DELETE);
    }
}
