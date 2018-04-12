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
package org.xwiki.extension.xar.internal.handler.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.XarExtensionExtension;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.XarExtensionPlan;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.job.diff.DocumentVersionReference;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.logging.marker.BeginTranslationMarker;
import org.xwiki.logging.marker.EndTranslationMarker;
import org.xwiki.logging.marker.TranslationMarker;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarFile;
import org.xwiki.xar.internal.model.XarModel;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;

/**
 * Default implementation of {@link Packager}.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component(roles = Packager.class)
@Singleton
public class Packager
{
    private static final BeginTranslationMarker LOG_INSTALLDOCUMENT_BEGIN =
        new BeginTranslationMarker("extension.xar.log.install.document.begin");

    private static final EndTranslationMarker LOG_INSTALLDOCUMENT_SUCCESS_END =
        new EndTranslationMarker("extension.xar.log.install.document.success.end");

    private static final EndTranslationMarker LOG_INSTALLDOCUMENT_FAILURE_END =
        new EndTranslationMarker("extension.xar.log.install.document.failure.end");

    private static final TranslationMarker LOG_DELETEDDOCUMENT =
        new TranslationMarker("extension.xar.log.delete.document");

    private static final TranslationMarker LOG_DELETEDDOCUMENT_FAILURE =
        new TranslationMarker("extension.xar.log.delete.document.failure");

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<EntityReference> resolver;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentMergeImporter importer;

    @Inject
    private MandatoryDocumentInitializerManager initializerManager;

    @Inject
    private XWikiDocumentFilterUtils documentImporter;

    @Inject
    private WikiDescriptorManager wikiDescriptors;

    @Inject
    @Named(XarExtensionHandler.TYPE)
    private InstalledExtensionRepository installedXARs;

    private XarInstalledExtensionRepository getXarInstalledExtensionRepository()
    {
        return (XarInstalledExtensionRepository) this.installedXARs;
    }

    public void importXAR(String comment, File xarFile, PackageConfiguration configuration)
        throws IOException, XWikiException, XarException, WikiManagerException
    {
        if (configuration.getWiki() == null) {
            Collection<String> wikis = this.wikiDescriptors.getAllIds();

            for (String subwiki : wikis) {
                importXARToWiki(comment, xarFile, new WikiReference(subwiki), configuration);
            }
        } else {
            importXARToWiki(comment, xarFile, new WikiReference(configuration.getWiki()), configuration);
        }
    }

    private void importXARToWiki(String comment, File xarFile, WikiReference wikiReference,
        PackageConfiguration configuration) throws IOException, XarException, XWikiException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        try {
            importXARToWiki(comment, fis, wikiReference, configuration);
        } finally {
            fis.close();
        }
    }

    private void importXARToWiki(String comment, InputStream xarInputStream, WikiReference wikiReference,
        PackageConfiguration configuration) throws IOException, XarException, XWikiException
    {
        ZipArchiveInputStream zis = new ZipArchiveInputStream(xarInputStream);

        XWikiContext xcontext = this.xcontextProvider.get();

        String currentWiki = xcontext.getWikiId();
        try {
            xcontext.setWikiId(wikiReference.getName());

            this.observation.notify(new XARImportingEvent(), null, xcontext);

            for (ArchiveEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                // Only import what should be imported
                if (!entry.isDirectory() && !entry.getName().equals(XarModel.PATH_PACKAGE)) {
                    if (configuration.getEntriesToImport() != null) {
                        XarEntry xarEntry = configuration.getEntriesToImport().get(entry.getName());
                        if (xarEntry != null) {
                            configuration.setXarEntry(xarEntry);

                            importDocumentToWiki(comment, wikiReference, zis, configuration);
                        }
                    } else {
                        configuration.setXarEntry(null);
                        importDocumentToWiki(comment, wikiReference, zis, configuration);
                    }
                }
            }
        } finally {
            this.observation.notify(new XARImportedEvent(), null, xcontext);

            xcontext.setWikiId(currentWiki);
        }
    }

    private void importDocumentToWiki(String comment, WikiReference wikiReference, InputStream inputStream,
        PackageConfiguration configuration) throws XWikiException, XarException, IOException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument nextDocument;
        try {
            nextDocument = getXWikiDocument(inputStream, wikiReference);
        } catch (Exception e) {
            this.logger.error("Failed to parse document", e);

            return;
        }

        DocumentReference reference = nextDocument.getDocumentReferenceWithLocale();
        XWikiDocument currentDocument = xcontext.getWiki().getDocument(reference, xcontext);
        currentDocument.loadAttachmentsContentSafe(xcontext);
        XWikiDocument previousDocument;
        XarExtensionPlan xarExtensionPlan = configuration.getXarExtensionPlan();
        if (xarExtensionPlan != null) {
            previousDocument = xarExtensionPlan.getPreviousXWikiDocument(reference, this);
        } else {
            previousDocument = null;
        }

        if (configuration.isVerbose()) {
            this.logger.info(LOG_INSTALLDOCUMENT_BEGIN, "Installing document [{}]",
                nextDocument.getDocumentReferenceWithLocale());
        }

        try {
            this.importer.importDocument(comment, previousDocument, currentDocument, nextDocument, configuration);

            if (configuration.isVerbose()) {
                this.logger.info(LOG_INSTALLDOCUMENT_SUCCESS_END, "Done installing document [{}]",
                    nextDocument.getDocumentReferenceWithLocale());
            }
        } catch (Exception e) {
            if (configuration.isVerbose()) {
                this.logger.error(LOG_INSTALLDOCUMENT_FAILURE_END, "Failed to install document [{}]",
                    nextDocument.getDocumentReferenceWithLocale(), e);
            }
        }
    }

    public void unimportPages(Collection<XarEntry> pages, PackageConfiguration configuration)
        throws WikiManagerException
    {
        if (configuration.getWiki() == null) {
            Collection<String> wikis = this.wikiDescriptors.getAllIds();

            for (String subwiki : wikis) {
                unimportPagesFromWiki(pages, subwiki, configuration);
            }
        } else {
            unimportPagesFromWiki(pages, configuration.getWiki(), configuration);
        }
    }

    private void unimportPagesFromWiki(Collection<XarEntry> entries, String wiki, PackageConfiguration configuration)
    {
        WikiReference wikiReference = new WikiReference(wiki);

        for (XarEntry xarEntry : entries) {
            // Only delete what should be deleted.
            if (configuration.getEntriesToImport() == null
                || configuration.getEntriesToImport().containsKey(xarEntry.getEntryName())) {
                DocumentReference documentReference =
                    new DocumentReference(this.resolver.resolve(xarEntry, wikiReference), xarEntry.getLocale());

                if (!configuration.isSkipMandatorytDocuments() || !isMandatoryDocument(documentReference)) {
                    deleteDocument(documentReference, configuration);
                }
            }
        }
    }

    public void deleteDocument(DocumentReference documentReference, PackageConfiguration configuration)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            // Make sure to have an expected context as much as possible
            if (configuration.getUserReference() != null) {
                xcontext.setUserReference(configuration.getUserReference());
            }

            XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);

            if (!document.isNew()) {
                xcontext.getWiki().deleteDocument(document, xcontext);

                if (configuration.isVerbose()) {
                    this.logger.info(LOG_DELETEDDOCUMENT, "Deleted document [{}]",
                        document.getDocumentReferenceWithLocale());
                }
            }
        } catch (XWikiException e) {
            this.logger.error(LOG_DELETEDDOCUMENT_FAILURE, "Failed to delete document [{}]", documentReference, e);
        }
    }

    private boolean isMandatoryDocument(DocumentReference documentReference)
    {
        return this.initializerManager.getMandatoryDocumentInitializer(documentReference) != null;
    }

    public XWikiDocument getXWikiDocument(WikiReference wikiReference, LocalDocumentReference documentReference,
        XarFile xarFile) throws XarException, IOException
    {
        XarEntry realEntry = xarFile.getEntry(documentReference);
        if (realEntry != null) {
            InputStream stream = xarFile.getInputStream(realEntry);

            try {
                return getXWikiDocument(stream, wikiReference);
            } catch (Exception e) {
                throw new XarException("Failed to read entry [" + realEntry + "]", e);
            } finally {
                stream.close();
            }
        }

        return null;
    }

    private DocumentReference cleanDocumentReference(DocumentReference reference)
    {
        // Remove the version if any since it does not make sense in a XAR
        DocumentReference documentReference = reference;
        if (reference instanceof DocumentVersionReference) {
            documentReference = ((DocumentVersionReference) reference).removeVersion();
        }

        return documentReference;
    }

    public XWikiDocument getXWikiDocument(DocumentReference reference) throws IOException, XarException
    {
        if (reference != null) {
            Collection<XarInstalledExtension> extensions =
                getXarInstalledExtensionRepository().getXarInstalledExtensions(reference);
            if (!extensions.isEmpty()) {
                return getXWikiDocument(reference, extensions.iterator().next());
            }
        }

        return null;
    }

    public XWikiDocument getXWikiDocument(DocumentReference reference, ExtensionId extensionId)
        throws IOException, XarException
    {
        if (reference != null) {
            if (extensionId != null) {
                return getXWikiDocument(reference,
                    (XarInstalledExtension) this.installedXARs.getInstalledExtension(extensionId));
            }

            return getXWikiDocument(reference);
        }

        return null;
    }

    public XWikiDocument getXWikiDocument(DocumentReference reference, XarInstalledExtension extension)
        throws XarException, IOException
    {
        if (reference != null) {
            // Remove the version if any since it does not make sense in a XAR
            DocumentReference documentReference = cleanDocumentReference(reference);

            return getXWikiDocument(documentReference.getWikiReference(), documentReference.getLocalDocumentReference(),
                extension);
        }

        return null;
    }

    public XWikiDocument getXWikiDocument(WikiReference wikiReference, LocalDocumentReference documentReference,
        XarInstalledExtension extension) throws XarException, IOException
    {
        try (
            XarFile xarFile = new XarFile(new File(extension.getFile().getAbsolutePath()), extension.getXarPackage())) {
            return getXWikiDocument(wikiReference, documentReference, xarFile);
        }
    }

    public XWikiDocument getXWikiDocument(InputStream source, WikiReference wikiReference)
        throws FilterException, IOException, ComponentLookupException
    {
        // Output
        DocumentInstanceOutputProperties documentProperties = new DocumentInstanceOutputProperties();
        documentProperties.setDefaultReference(wikiReference);
        documentProperties.setVersionPreserved(false);

        // Input
        XARInputProperties xarProperties = new XARInputProperties();
        xarProperties.setWithHistory(false);

        return this.documentImporter.importDocument(new DefaultInputStreamInputSource(source), xarProperties,
            documentProperties);
    }

    /**
     * @since 9.3RC1
     */
    public void reset(DocumentReference reference, DocumentReference authorReference)
        throws IOException, XarException, XWikiException, XarExtensionExtension
    {
        Collection<XarInstalledExtension> installedExtensions =
            getXarInstalledExtensionRepository().getXarInstalledExtensions(reference);
        if (!installedExtensions.isEmpty()) {
            XarInstalledExtension extension = installedExtensions.iterator().next();

            // Remove the version if any since it does not make sense in a XAR
            DocumentReference documentReference = cleanDocumentReference(reference);

            XWikiDocument document = getXWikiDocument(documentReference, extension);

            if (document != null) {
                XWikiContext xcontext = this.xcontextProvider.get();

                // Get database document
                XWikiDocument databaseDocument = xcontext.getWiki().getDocument(documentReference, xcontext);

                // Override data of database document with extension document
                databaseDocument.apply(document, true);
                // Make sure new version will have the right author
                databaseDocument.setAuthorReference(authorReference);
                databaseDocument.setContentAuthorReference(authorReference);
                // Force generating new version
                databaseDocument.setMetaDataDirty(true);
                databaseDocument.setContentDirty(true);

                // Save
                xcontext.getWiki().saveDocument(databaseDocument, "Reset document from extension [" + extension + "]",
                    xcontext);
            } else {
                throw new XarExtensionExtension("Can't find any document with reference [" + documentReference
                    + "] in extension [" + extension.getId() + "]");
            }
        } else {
            throw new XarExtensionExtension(
                "Can't find any installed extension associated with the document reference [" + reference + "]");
        }
    }

    public List<DocumentReference> getDocumentReferences(Collection<XarEntry> pages, PackageConfiguration configuration)
        throws WikiManagerException
    {
        List<DocumentReference> documents = new ArrayList<>(pages.size());

        if (configuration.getWiki() == null) {
            Collection<String> wikis = this.wikiDescriptors.getAllIds();

            for (String subwiki : wikis) {
                getDocumentReferencesFromWiki(documents, pages, subwiki, configuration);
            }
        } else {
            getDocumentReferencesFromWiki(documents, pages, configuration.getWiki(), configuration);
        }

        return documents;
    }

    private void getDocumentReferencesFromWiki(List<DocumentReference> documents, Collection<XarEntry> pages,
        String wiki, PackageConfiguration configuration)
    {
        WikiReference wikiReference = new WikiReference(wiki);

        for (XarEntry xarEntry : pages) {
            // Only delete what should be deleted.
            if (configuration.getEntriesToImport() == null
                || configuration.getEntriesToImport().containsKey(xarEntry.getEntryName())) {
                DocumentReference documentReference = new DocumentReference(xarEntry, wikiReference);

                if (!configuration.isSkipMandatorytDocuments() || !isMandatoryDocument(documentReference)) {
                    documents.add(documentReference);
                }
            }
        }
    }
}
