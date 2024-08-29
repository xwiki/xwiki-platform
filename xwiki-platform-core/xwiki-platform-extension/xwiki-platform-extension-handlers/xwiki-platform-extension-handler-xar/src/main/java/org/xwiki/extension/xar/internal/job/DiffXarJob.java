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
package org.xwiki.extension.xar.internal.job;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.AbstractExtensionJob;
import org.xwiki.extension.xar.internal.handler.UnsupportedNamespaceException;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.XarHandlerUtils;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.job.diff.DocumentUnifiedDiffBuilder;
import org.xwiki.extension.xar.job.diff.DiffXarJobStatus;
import org.xwiki.extension.xar.job.diff.DocumentUnifiedDiff;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarFile;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Computes the differences between the documents provided by a XAR extension and the documents from the database.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
@Component
@Named(DiffXarJob.JOB_TYPE)
public class DiffXarJob extends AbstractExtensionJob<InstallRequest, DiffXarJobStatus>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "diffXar";

    /**
     * Used to get the documents from the XAR.
     */
    @Inject
    private Packager packager;

    /**
     * Used to get the documents from the database.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to compute the differences.
     */
    @Inject
    private DocumentUnifiedDiffBuilder documentDiffBuilder;

    /**
     * The set of features that have been compared. We try to avoid comparing the same feature twice. We assume all the
     * features are compared on the same namespace.
     */
    private Set<String> comparedFeatures = new HashSet<>();

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected DiffXarJobStatus createNewStatus(InstallRequest request)
    {
        return new DiffXarJobStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        InstallRequest request = getRequest();
        // There must be only one namespace specified because we compute the differences for only one wiki.
        if (!request.hasNamespaces() || request.getNamespaces().size() != 1) {
            return;
        }

        String namespace = request.getNamespaces().iterator().next();

        Collection<ExtensionId> extensionIds = request.getExtensions();
        this.progressManager.pushLevelProgress(extensionIds.size(), this);
        try {
            for (ExtensionId extensionId : extensionIds) {
                this.progressManager.startStep(this);

                InstalledExtension installedExtension = getInstalledExtension(extensionId, namespace);
                // Make sure the specified extension is installed on the specified namespace.
                if (installedExtension != null && installedExtension.isInstalled(namespace)) {
                    diff(extensionId.getId(), namespace, new HashSet<>());
                }

                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private InstalledExtension getInstalledExtension(ExtensionId extensionId, String namespace)
    {
        if (extensionId.getVersion() != null) {
            return this.installedExtensionRepository.getInstalledExtension(extensionId);
        } else {
            return this.installedExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        }
    }

    private void diff(String feature, String namespace, Set<LocalDocumentReference> alreadydone)
    {
        if (this.comparedFeatures.contains(feature)) {
            // We already looked at this feature.
            return;
        }
        this.comparedFeatures.add(feature);

        InstalledExtension installedExtension =
            this.installedExtensionRepository.getInstalledExtension(feature, namespace);
        if (installedExtension != null) {
            diff(installedExtension, namespace, alreadydone);

            Collection<? extends ExtensionDependency> dependencies = installedExtension.getDependencies();

            this.progressManager.pushLevelProgress(dependencies.size(), this);
            try {
                for (ExtensionDependency dependency : dependencies) {
                    this.progressManager.startStep(this);

                    diff(dependency.getId(), namespace, new HashSet<>(alreadydone));
                }
            } finally {
                this.progressManager.popLevelProgress(this);
            }
        }
    }

    private void diff(InstalledExtension installedExtension, String namespace, Set<LocalDocumentReference> alreadydone)
    {
        Collection<ExtensionId> excludedExtensions = getRequest().getExcludedExtensions();
        if (XarExtensionHandler.TYPE.equals(installedExtension.getType())
            && (excludedExtensions == null || !excludedExtensions.contains(installedExtension.getId()))) {
            if (getRequest().isVerbose()) {
                this.logger.info("Computing differences for [{}] on namespace [{}]", installedExtension.getId(),
                    namespace);
            }
            try {
                WikiReference wikiReference = new WikiReference(XarHandlerUtils.getWikiFromNamespace(namespace));
                diff(new XarFile(new File(installedExtension.getFile().getAbsolutePath())), wikiReference,
                    installedExtension.getId(), alreadydone);
            } catch (UnsupportedNamespaceException e) {
                this.logger.error("Failed to extract the wiki id from the namespace [{}].", namespace, e);
            } catch (IOException e) {
                this.logger.error("Failed to read the XAR file of the extension [{}].", installedExtension.getId(), e);
            } catch (XarException e) {
                this.logger.error("Failed to parse the XAR file of the extension [{}].", installedExtension.getId(), e);
            }
        }
    }

    private void diff(XarFile xarFile, WikiReference wikiReference, ExtensionId extensionId,
        Set<LocalDocumentReference> alreadydone)
    {
        Collection<XarEntry> xarEntries = xarFile.getEntries();
        this.progressManager.pushLevelProgress(xarEntries.size(), this);
        try {
            for (XarEntry xarEntry : xarEntries) {
                this.progressManager.startStep(this);

                if (!alreadydone.contains(xarEntry)) {
                    try {
                        diff(this.packager.getXWikiDocument(xarFile.getInputStream(xarEntry), wikiReference),
                            extensionId);
                    } catch (Exception e) {
                        // Skip this document and continue.
                        this.logger.error("Failed to parse document [{}] from XAR.", xarEntry.getDocumentName(), e);
                    }

                    alreadydone.add(xarEntry);
                }
            }
        } finally {
            try {
                xarFile.close();
            } catch (IOException e) {
                // Ignore.
            }
            this.progressManager.popLevelProgress(this);
        }
    }

    private void diff(XWikiDocument document, ExtensionId extensionId)
    {
        if (getRequest().isVerbose()) {
            this.logger.info("Computing differences for document [{}]", document.getDocumentReferenceWithLocale());
        }
        // Use the extension id as the document version.
        XWikiDocument previousDocument =
            document.duplicate(new DocumentVersionReference(document.getDocumentReference(), extensionId));
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            XWikiDocument nextDocument =
                xcontext.getWiki().getDocument(document.getDocumentReferenceWithLocale(), xcontext);
            if (nextDocument.isNew()) {
                nextDocument = null;
            }
            maybeAddDocumentDiff(this.documentDiffBuilder.diff(previousDocument, nextDocument));
        } catch (XWikiException e) {
            this.logger.error("Failed to get document [{}] from the database.", document.getDocumentReference(), e);
        }
    }

    private void maybeAddDocumentDiff(DocumentUnifiedDiff documentDiff)
    {
        int differencesCount = documentDiff.size() + documentDiff.getAttachmentDiffs().size()
            + documentDiff.getObjectDiffs().size() + documentDiff.getClassPropertyDiffs().size();
        if (getRequest().isVerbose()) {
            if (documentDiff.getNextReference() == null) {
                this.logger.info("The document [{}] has been deleted", documentDiff.getPreviousReference());
            } else if (documentDiff.getPreviousReference() == null) {
                this.logger.info("The document [{}] has been added", documentDiff.getNextReference());
            } else if (differencesCount > 0) {
                this.logger.info("The document [{}] has [{}] changes", documentDiff.getPreviousReference(),
                    differencesCount);
            } else {
                this.logger.info("The document [{}] has no changes", documentDiff.getPreviousReference());
            }
        }
        if (differencesCount > 0) {
            getStatus().getDocumentDiffs().add(documentDiff);
        }
    }
}
