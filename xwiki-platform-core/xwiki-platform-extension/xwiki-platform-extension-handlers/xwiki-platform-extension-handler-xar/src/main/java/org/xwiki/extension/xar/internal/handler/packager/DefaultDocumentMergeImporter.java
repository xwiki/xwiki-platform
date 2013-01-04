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

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.xar.internal.handler.ConflictQuestion;
import org.xwiki.extension.xar.internal.handler.ConflictQuestion.GlobalAction;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

/**
 * Default implementation of {@link DocumentMergeImporter}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultDocumentMergeImporter implements DocumentMergeImporter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private Logger logger;

    @Override
    public XarEntryMergeResult saveDocument(String comment, XWikiDocument previousDocument,
        XWikiDocument currentDocument, XWikiDocument nextDocument, PackageConfiguration configuration) throws Exception
    {
        XarEntryMergeResult mergeResult = null;

        if (configuration.isLogEnabled()) {
            this.logger.info("Importing document [{}] in language [{}]...", nextDocument.getDocumentReference(),
                nextDocument.getRealLocale());
        }

        // Merge and save
        if (currentDocument != null && !currentDocument.isNew()) {
            if (previousDocument != null) {
                // 3 ways merge
                mergeResult = merge(comment, currentDocument, previousDocument, nextDocument, configuration);
            } else {
                // Check if a mandatory document initializer exists for the current document
                XWikiDocument mandatoryDocument = getMandatoryDocument(nextDocument.getDocumentReference());

                if (mandatoryDocument != null) {
                    // 3 ways merge
                    mergeResult = merge(comment, currentDocument, mandatoryDocument, nextDocument, configuration);
                } else {
                    // already existing document in database but without previous version
                    if (!currentDocument.equalsData(nextDocument)) {
                        XWikiDocument documentToSave;
                        if (configuration.isInteractive()) {
                            // Indicate future author to whoever is going to answer the question
                            nextDocument.setCreatorReference(currentDocument.getCreatorReference());
                            DocumentReference userReference = configuration.getUserReference();
                            nextDocument.setAuthorReference(userReference);
                            nextDocument.setContentAuthorReference(userReference);

                            documentToSave =
                                askDocumentToSave(currentDocument, previousDocument, nextDocument, null, configuration);
                        } else {
                            documentToSave = nextDocument;
                        }

                        if (documentToSave != currentDocument) {
                            saveDocument(documentToSave, comment, false, configuration);
                        }
                    }
                }
            }
        } else {
            saveDocument(nextDocument, comment, true, configuration);
        }

        return mergeResult;
    }

    private XarEntryMergeResult merge(String comment, XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, PackageConfiguration configuration) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // 3 ways merge
        XWikiDocument mergedDocument = currentDocument.clone();

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setProvidedVersionsModifiables(true);

        MergeResult documentMergeResult =
            mergedDocument.merge(previousDocument, nextDocument, mergeConfiguration, xcontext);

        if (configuration.isInteractive() && !documentMergeResult.getLog().getLogs(LogLevel.ERROR).isEmpty()) {
            // Indicate future author to whoever is going to answer the question
            nextDocument.setCreatorReference(currentDocument.getCreatorReference());
            mergedDocument.setCreatorReference(currentDocument.getCreatorReference());
            DocumentReference userReference = configuration.getUserReference();
            if (userReference != null) {
                nextDocument.setAuthorReference(userReference);
                nextDocument.setContentAuthorReference(userReference);
                mergedDocument.setAuthorReference(userReference);
                mergedDocument.setContentAuthorReference(userReference);
            }

            XWikiDocument documentToSave =
                askDocumentToSave(currentDocument, previousDocument, nextDocument, mergedDocument, configuration);

            if (documentToSave != currentDocument) {
                saveDocument(documentToSave, comment, false, configuration);
            }
        } else if (documentMergeResult.isModified()) {
            saveDocument(mergedDocument, comment, false, configuration);
        }

        return new XarEntryMergeResult(new XarEntry(mergedDocument.getDocumentReference(), mergedDocument.getLocale()),
            documentMergeResult);
    }

    private XWikiDocument getMandatoryDocument(DocumentReference documentReference)
    {
        MandatoryDocumentInitializer initializer;
        try {
            initializer =
                this.componentManager.get().getInstance(MandatoryDocumentInitializer.class,
                    this.serializer.serialize(documentReference));
        } catch (ComponentLookupException e) {
            try {
                initializer =
                    this.componentManager.get().getInstance(MandatoryDocumentInitializer.class,
                        this.localSerializer.serialize(documentReference));
            } catch (ComponentLookupException e1) {
                initializer = null;
            }
        }

        XWikiDocument mandatoryDocument;
        if (initializer != null) {
            // Generate clean mandatory document
            mandatoryDocument = new XWikiDocument(documentReference);

            if (!initializer.updateDocument(mandatoryDocument)) {
                mandatoryDocument = null;
            }
        } else {
            mandatoryDocument = null;
        }

        return mandatoryDocument;
    }

    private XWikiDocument askDocumentToSave(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocument mergedDocument, PackageConfiguration configuration)
    {
        // Ask what to do
        ConflictQuestion question =
            new ConflictQuestion(currentDocument, previousDocument, nextDocument, mergedDocument);

        if (mergedDocument == null) {
            question.setGlobalAction(GlobalAction.NEXT);
        }

        if (configuration != null && configuration.getJobStatus() != null) {
            try {
                configuration.getJobStatus().ask(question);
            } catch (InterruptedException e) {
                // TODO: log something ?
            }
        }

        XWikiDocument documentToSave;

        switch (question.getGlobalAction()) {
            case CURRENT:
                documentToSave = currentDocument;
                break;
            case NEXT:
                documentToSave = nextDocument;
                break;
            case PREVIOUS:
                documentToSave = previousDocument;
                break;
            case CUSTOM:
                documentToSave = question.getCustomDocument() != null ? question.getCustomDocument() : mergedDocument;
                break;
            default:
                documentToSave = mergedDocument;
                break;
        }

        return documentToSave;
    }

    private XWikiDocument getDatabaseDocument(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        XWikiDocument existingDocument = context.getWiki().getDocument(document.getDocumentReference(), context);

        if (!document.getLocale().equals(Locale.ROOT)) {
            Locale defaultLocale = existingDocument.getDefaultLocale();
            XWikiDocument translatedDocument = existingDocument.getTranslatedDocument(document.getLocale(), context);

            if (translatedDocument == existingDocument) {
                translatedDocument = new XWikiDocument(document.getDocumentReference());
                translatedDocument.setDefaultLocale(defaultLocale);
                translatedDocument.setTranslation(1);
                translatedDocument.setLocale(document.getLocale());
            }

            existingDocument = translatedDocument;
        }

        return existingDocument;
    }

    private void saveDocument(XWikiDocument document, String comment, boolean setCreator,
        PackageConfiguration configuration) throws Exception
    {
        XWikiContext context = this.xcontextProvider.get();

        XWikiDocument currentDocument = getDatabaseDocument(document, context);

        DocumentReference userReference = configuration.getUserReference();

        if (!currentDocument.isNew()) {
            if (document != currentDocument) {
                if (document.isNew()) {
                    currentDocument.apply(document);
                    if (setCreator) {
                        currentDocument.setCreatorReference(document.getCreatorReference());
                    }
                    currentDocument.setAuthorReference(document.getAuthorReference());
                    currentDocument.setContentAuthorReference(document.getContentAuthorReference());
                } else {
                    currentDocument = document;
                }
            }
        } else {
            currentDocument = document;
        }

        if (userReference != null) {
            if (setCreator) {
                currentDocument.setCreatorReference(userReference);
            }
            currentDocument.setAuthorReference(userReference);
            currentDocument.setContentAuthorReference(userReference);
        }

        saveDocumentSetContextUser(currentDocument, comment);
    }

    private void saveDocumentSetContextUser(XWikiDocument document, String comment) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference userReference = xcontext.getUserReference();

        try {
            // Make sure to have context user corresponding to document author for badly designed listeners expecting
            // the document to actually be saved by context user
            xcontext.setUserReference(document.getAuthorReference());

            xcontext.getWiki().saveDocument(document, comment, false, xcontext);
        } finally {
            xcontext.setUserReference(userReference);
        }
    }
}
