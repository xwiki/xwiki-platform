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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.extension.xar.XWikiDocumentMerger;
import org.xwiki.extension.xar.XWikiDocumentMergerConfiguration;
import org.xwiki.extension.xar.XarExtensionException;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.xar.XarEntryType.UpgradeType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;

/**
 * Default implementation of {@link XWikiDocumentMerger};
 *
 * @version $Id$
 * @since 10.3
 */
@Component
@Singleton
public class DefaultXWikiDocumentMerger implements XWikiDocumentMerger
{
    @Inject
    private MandatoryDocumentInitializerManager initializerManager;

    @Inject
    private Execution execution;

    @Inject
    private JobContext jobContext;

    @Inject
    private MergeManager mergeManager;

    @Inject
    private MergeConflictDecisionsManager conflictDecisionsManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Override
    public XWikiDocument merge(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration) throws XarExtensionException
    {
        //////////
        // Upgrade
        //////////

        if (previousDocument != null) {
            return upgrade(currentDocument, previousDocument, nextDocument, configuration);
        }

        //////////
        // Install
        //////////

        return install(currentDocument, nextDocument, configuration);
    }

    private XWikiDocument upgrade(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        UpgradeType type = configuration.getType();
        if (type != null) {
            switch (type) {
                case OVERWRITE:
                    return OVERWRITE(currentDocument, previousDocument, nextDocument, configuration);
                case SKIP:
                    return SKIP(currentDocument, previousDocument, nextDocument, configuration);
                case SKIP_ALLWAYS:
                    return SKIP_ALLWAYS(currentDocument, previousDocument, nextDocument, configuration);

                default:
                    break;
            }
        }

        return THREEWAYS(currentDocument, previousDocument, nextDocument, configuration);
    }

    private XWikiDocument install(XWikiDocument currentDocument, XWikiDocument nextDocument,
        XWikiDocumentMergerConfiguration configuration)
    {
        XWikiDocument result = currentDocument;

        if (currentDocument != null) {
            // Check if a mandatory document initializer exists for the current document
            MandatoryDocumentInitializer initializer =
                this.initializerManager.getMandatoryDocumentInitializer(nextDocument.getDocumentReference());

            XWikiDocument mandatoryDocument = getMandatoryDocument(nextDocument.getDocumentReference(), initializer);

            if (mandatoryDocument != null) {
                // Update the new version with the initializer since we want to keep modification made by the
                // initializer in the new version
                initializer.updateDocument(nextDocument);

                // 3 ways merge
                result = merge3(currentDocument, mandatoryDocument, nextDocument, configuration);
            } else {
                // Already existing document in database but without previous version
                if (!currentDocument.equalsData(nextDocument)) {
                    result = askDocumentToSave(currentDocument, null, nextDocument, configuration, null);
                }
            }
        } else {
            // Simple install (the document does not exist in previous version or in the database)
            result = nextDocument;
        }

        return result;
    }

    private XWikiDocument THREEWAYS(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        XWikiDocument result = null;

        if (currentDocument != null) {
            // 3 ways merge
            result = merge3(currentDocument, previousDocument, nextDocument, configuration);
        } else {
            // Document have been deleted in the database
            result = askDocumentToSave(null, previousDocument, nextDocument, configuration, null);
        }

        return result;
    }

    private XWikiDocument OVERWRITE(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        // Always install new version no matter what
        return nextDocument;
    }

    private XWikiDocument SKIP(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        XWikiDocument result = currentDocument;

        if (currentDocument != null) {
            if (previousDocument != null && currentDocument.equalsData(previousDocument)) {
                // No customization have been made
                result = nextDocument;
            }
        } else if (previousDocument == null) {
            result = nextDocument;
        }

        return result;
    }

    private XWikiDocument SKIP_ALLWAYS(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        XWikiDocument result = currentDocument;

        // Only install the document (no upgrade)
        if (currentDocument == null && previousDocument == null) {
            result = nextDocument;
        }

        return result;
    }

    private XWikiDocument merge3(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration)
    {
        // Check if there is any customization
        if (currentDocument.equalsData(previousDocument)) {
            // Check if there is any difference between previous and new
            return currentDocument.equalsData(nextDocument) ? currentDocument : nextDocument;
        }

        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setUserReference(contextProvider.get().getUserReference());
        mergeConfiguration.setConcernedDocument(currentDocument.getDocumentReferenceWithLocale());
        mergeConfiguration.setProvidedVersionsModifiables(false);

        MergeDocumentResult documentMergeResult;
        try {
            documentMergeResult =
                mergeManager.mergeDocument(previousDocument, nextDocument, currentDocument, mergeConfiguration);
        } catch (Exception e) {
            // Unexpected error, lets behave as if there was a conflict
            documentMergeResult = new MergeDocumentResult(currentDocument, previousDocument, nextDocument);
            documentMergeResult.getLog()
                .error("Unexpected exception thrown. Usually means there is a bug in the merge.", e);
            documentMergeResult.setMergeResult(currentDocument.clone());
        }

        documentMergeResult.getLog().log(this.logger);

        return askDocumentToSave(currentDocument, previousDocument, nextDocument, configuration, documentMergeResult);
    }

    private XWikiDocument getMandatoryDocument(DocumentReference documentReference,
        MandatoryDocumentInitializer initializer)
    {
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
        XWikiDocument nextDocument, XWikiDocumentMergerConfiguration configuration,
        MergeDocumentResult documentMergeResult)
    {
        // Indicate future author to whoever is going to answer the question
        XWikiDocument mergedDocument;

        if (documentMergeResult != null) {
            mergedDocument = (XWikiDocument) documentMergeResult.getMergeResult();
        } else {
            mergedDocument = null;
        }

        if (currentDocument != null) {
            nextDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        if (mergedDocument != null) {
            mergedDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        DocumentReference userDocumentReference = configuration != null ? configuration.getAuthorReference() : null;
        if (userDocumentReference != null) {
            UserReference userReference = this.documentReferenceUserReferenceResolver.resolve(userDocumentReference);
            if (nextDocument != null) {
                DocumentAuthors authors = nextDocument.getAuthors();
                authors.setContentAuthor(userReference);
                authors.setEffectiveMetadataAuthor(userReference);
            }

            for (XWikiAttachment attachment : nextDocument.getAttachmentList()) {
                attachment.setAuthorReference(userDocumentReference);
            }
            if (mergedDocument != null) {
                DocumentAuthors mergedDocumentAuthors = mergedDocument.getAuthors();
                mergedDocumentAuthors.setEffectiveMetadataAuthor(userReference);
                mergedDocumentAuthors.setContentAuthor(userReference);

                for (XWikiAttachment attachment : mergedDocument.getAttachmentList()) {
                    if (attachment.isContentDirty()) {
                        attachment.setAuthorReference(userDocumentReference);
                    }
                }
            }
        }

        // Calculate the conflict type
        ConflictQuestion.ConflictType type;
        List<Conflict<?>> documentContentConflicts = null;
        if (previousDocument == null) {
            type = ConflictQuestion.ConflictType.CURRENT_EXIST;
        } else if (currentDocument == null) {
            type = ConflictQuestion.ConflictType.CURRENT_DELETED;
        } else if (documentMergeResult != null) {
            if (documentMergeResult.getLog().hasLogLevel(LogLevel.ERROR)) {
                type = ConflictQuestion.ConflictType.MERGE_FAILURE;
                documentContentConflicts = documentMergeResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT);
            } else {
                type = ConflictQuestion.ConflictType.MERGE_SUCCESS;
            }
        } else {
            type = null;
        }

        // Create a question
        ConflictQuestion question = new ConflictQuestion(currentDocument, previousDocument, nextDocument,
            mergedDocument, type, documentContentConflicts);

        // Find the answer
        GlobalAction contextAction = getMergeConflictAnswer(question.getType(), configuration);
        if (contextAction != null && contextAction != GlobalAction.ASK) {
            question.setGlobalAction(contextAction);
        } else {
            Job job = this.jobContext.getCurrentJob();

            if (job != null && job.getStatus() != null && job.getStatus().getRequest() != null
                && job.getStatus().getRequest().isInteractive()) {
                try {
                    // Ask what to do
                    job.getStatus().ask(question);
                    if (question.isAlways()) {
                        setMergeConflictAnswer(question.getType(), question.getGlobalAction());
                    }
                } catch (InterruptedException e) {
                    // TODO: log something ?
                }
            }
        }

        List<ConflictDecision> decisions = question.getDecisions();
        if (question.getGlobalAction() == GlobalAction.MERGED && !decisions.isEmpty()) {
            // record the decisions
            this.conflictDecisionsManager.setConflictDecisionList(decisions,
                currentDocument.getDocumentReferenceWithLocale(), contextProvider.get().getUserReference());

            // try again the merge with the decisions
            MergeConfiguration mergeConfiguration = new MergeConfiguration();
            mergeConfiguration.setConcernedDocument(currentDocument.getDocumentReferenceWithLocale());
            mergeConfiguration.setUserReference(contextProvider.get().getUserReference());
            mergeConfiguration.setProvidedVersionsModifiables(false);
            documentMergeResult =
                mergeManager.mergeDocument(previousDocument, nextDocument, currentDocument, mergeConfiguration);
            mergedDocument = (XWikiDocument) documentMergeResult.getMergeResult();
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
                documentToSave =
                    documentMergeResult == null || documentMergeResult.isModified() ? mergedDocument : currentDocument;
                break;
        }

        return documentToSave;
    }

    private GlobalAction getMergeConflictAnswer(ConflictQuestion.ConflictType type,
        XWikiDocumentMergerConfiguration configuration)
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
}
