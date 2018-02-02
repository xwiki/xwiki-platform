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

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.XarEntry;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

/**
 * Take care of properly merging and saving a document.
 * 
 * @version $Id$
 */
@Component(roles = DocumentMergeImporter.class)
@Singleton
public class DocumentMergeImporter
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private MandatoryDocumentInitializerManager initializerManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    /**
     * @param comment the comment to use when/if saving the document
     * @param previousDocument the previous version of the document
     * @param currentDocument the current version of the document
     * @param nextDocument the new version of the document
     * @param configuration various setup for the import
     * @return the result of the merge
     * @throws Exception when failed to save the document
     */
    public XarEntryMergeResult saveDocument(String comment, XWikiDocument previousDocument,
        XWikiDocument currentDocument, XWikiDocument nextDocument, PackageConfiguration configuration) throws Exception
    {
        XarEntryMergeResult mergeResult = null;

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
                    // Already existing document in database but without previous version
                    if (!currentDocument.equalsData(nextDocument)) {
                        XWikiDocument documentToSave =
                            askDocumentToSave(currentDocument, null, nextDocument, null, configuration, null);

                        if (documentToSave != currentDocument) {
                            saveDocument(documentToSave, comment, false, configuration);
                        }
                    }
                }
            }
        } else if (previousDocument != null) {
            // Document have been deleted in the database
            XWikiDocument documentToSave =
                askDocumentToSave(null, previousDocument, nextDocument, null, configuration, null);

            if (documentToSave != null) {
                saveDocument(documentToSave, comment, true, configuration);
            }
        } else {
            // Simple install (the document does not exist in previous version or in the database)
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

        MergeResult documentMergeResult;
        try {
            documentMergeResult = mergedDocument.merge(previousDocument, nextDocument, mergeConfiguration, xcontext);
        } catch (Exception e) {
            // Unexpected error, lets behave as if there was a conflict
            documentMergeResult = new MergeResult();
            documentMergeResult.getLog()
                .error("Unexpected exception thrown. Usually means there is a bug in the merge.", e);
            documentMergeResult.setModified(true);
        }

        documentMergeResult.getLog().log(this.logger);

        XWikiDocument documentToSave;
        if (documentMergeResult.isModified() || !documentMergeResult.getLog().getLogsFrom(LogLevel.ERROR).isEmpty()) {
            documentToSave = askDocumentToSave(currentDocument, previousDocument, nextDocument, mergedDocument,
                configuration, documentMergeResult);

            if (documentToSave != currentDocument) {
                saveDocument(documentToSave, comment, false, configuration);
            }
        }

        return new XarEntryMergeResult(
            new XarEntry(new LocalDocumentReference(mergedDocument.getDocumentReferenceWithLocale())),
            documentMergeResult);
    }

    private XWikiDocument getMandatoryDocument(DocumentReference documentReference)
    {
        MandatoryDocumentInitializer initializer =
            this.initializerManager.getMandatoryDocumentInitializer(documentReference);

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

    private GlobalAction getMergeConflictAnswer(ConflictQuestion.ConflictType type, PackageConfiguration configuration)
    {
        GlobalAction action = (GlobalAction) this.execution.getContext().getProperty(ConflictQuestion.toKey(type));

        if (action == null && configuration != null) {
            action = configuration.getConflictAction(type);
        }

        return action;
    }

    private void setMergeConflictAnswer(ConflictQuestion.ConflictType type, GlobalAction action)
    {
        this.execution.getContext().setProperty(ConflictQuestion.toKey(type), action);
    }

    private XWikiDocument askDocumentToSave(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocument mergedDocument, PackageConfiguration configuration,
        MergeResult documentMergeResult)
    {
        // Indicate future author to whoever is going to answer the question
        if (currentDocument != null) {
            nextDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        if (mergedDocument != null) {
            mergedDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        DocumentReference userReference = configuration != null ? configuration.getUserReference() : null;
        if (userReference != null) {
            nextDocument.setAuthorReference(userReference);
            nextDocument.setContentAuthorReference(userReference);
            for (XWikiAttachment attachment : nextDocument.getAttachmentList()) {
                attachment.setAuthorReference(nextDocument.getAuthorReference());
            }
            if (mergedDocument != null) {
                mergedDocument.setAuthorReference(userReference);
                mergedDocument.setContentAuthorReference(userReference);
                for (XWikiAttachment attachment : mergedDocument.getAttachmentList()) {
                    if (attachment.isContentDirty()) {
                        attachment.setAuthorReference(mergedDocument.getAuthorReference());
                    }
                }
            }
        }

        // Calculate the conflict type
        ConflictQuestion.ConflictType type;
        if (previousDocument == null) {
            type = ConflictQuestion.ConflictType.CURRENT_EXIST;
        } else if (currentDocument == null) {
            type = ConflictQuestion.ConflictType.CURRENT_DELETED;
        } else if (documentMergeResult != null) {
            if (!documentMergeResult.getLog().getLogs(LogLevel.ERROR).isEmpty()) {
                type = ConflictQuestion.ConflictType.MERGE_FAILURE;
            } else {
                type = ConflictQuestion.ConflictType.MERGE_SUCCESS;
            }
        } else {
            type = null;
        }

        // Create a question
        ConflictQuestion question =
            new ConflictQuestion(currentDocument, previousDocument, nextDocument, mergedDocument, type);

        // Find the answer
        GlobalAction contextAction = getMergeConflictAnswer(question.getType(), configuration);
        if (contextAction != null && contextAction != GlobalAction.ASK) {
            question.setGlobalAction(contextAction);
        } else if (configuration != null && configuration.getJobStatus() != null && configuration.isInteractive()) {
            try {
                // Ask what to do
                configuration.getJobStatus().ask(question);
                if (question.isAlways()) {
                    setMergeConflictAnswer(question.getType(), question.getGlobalAction());
                }
            } catch (InterruptedException e) {
                // TODO: log something ?
            }
        }

        // Find the XWikiDocument to save
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

    private void saveDocument(XWikiDocument document, String comment, boolean setCreator,
        PackageConfiguration configuration) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument currentDocument =
            xcontext.getWiki().getDocument(document.getDocumentReferenceWithLocale(), xcontext);

        if (!currentDocument.isNew()) {
            if (document != currentDocument) {
                if (document.isNew()) {
                    currentDocument.loadAttachmentsContentSafe(xcontext);
                    currentDocument.apply(document);
                } else {
                    currentDocument = document;
                }
            }
        } else {
            currentDocument = document;
        }

        // Set document authors
        DocumentReference configuredUser = configuration.getUserReference();
        if (configuredUser != null) {
            if (setCreator) {
                currentDocument.setCreatorReference(configuredUser);
            }
            currentDocument.setAuthorReference(configuredUser);
            currentDocument.setContentAuthorReference(configuredUser);

            // Set attachments authors
            for (XWikiAttachment attachment : currentDocument.getAttachmentList()) {
                if (attachment.isContentDirty()) {
                    attachment.setAuthorReference(currentDocument.getAuthorReference());
                }
            }
        } else {
            if (document != currentDocument) {
                if (setCreator) {
                    currentDocument.setCreatorReference(document.getCreatorReference());
                }
                currentDocument.setAuthorReference(document.getAuthorReference());
                currentDocument.setContentAuthorReference(document.getContentAuthorReference());

                // Set attachments authors
                for (XWikiAttachment attachment : document.getAttachmentList()) {
                    if (attachment.isContentDirty()) {
                        currentDocument.getAttachment(attachment.getFilename())
                            .setAuthorReference(attachment.getAuthorReference());
                    }
                }
            }

            // Make sure to keep the content author we want
            currentDocument.setContentDirty(false);
            currentDocument.setContentUpdateDate(new Date());
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
