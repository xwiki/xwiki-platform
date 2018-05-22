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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.xar.XWikiDocumentMerger;
import org.xwiki.extension.xar.XWikiDocumentMergerConfiguration;
import org.xwiki.extension.xar.XarExtensionException;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xar.XarEntryType.UpgradeType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private MandatoryDocumentInitializerManager initializerManager;

    @Inject
    private Execution execution;

    @Inject
    private JobContext jobContext;

    @Inject
    private Logger logger;

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
            XWikiDocument mandatoryDocument = getMandatoryDocument(nextDocument.getDocumentReference());

            if (mandatoryDocument != null) {
                // 3 ways merge
                result = merge3(currentDocument, mandatoryDocument, nextDocument, configuration);
            } else {
                // Already existing document in database but without previous version
                if (!currentDocument.equalsData(nextDocument)) {
                    result = askDocumentToSave(currentDocument, null, nextDocument, null, configuration, null);
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
            result = askDocumentToSave(null, previousDocument, nextDocument, null, configuration, null);
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

        if (documentMergeResult.isModified() || !documentMergeResult.getLog().getLogsFrom(LogLevel.ERROR).isEmpty()) {
            return askDocumentToSave(currentDocument, previousDocument, nextDocument, mergedDocument, configuration,
                documentMergeResult);
        }

        return null;
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

    private XWikiDocument askDocumentToSave(XWikiDocument currentDocument, XWikiDocument previousDocument,
        XWikiDocument nextDocument, XWikiDocument mergedDocument, XWikiDocumentMergerConfiguration configuration,
        MergeResult documentMergeResult)
    {
        // Indicate future author to whoever is going to answer the question
        if (currentDocument != null) {
            nextDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        if (mergedDocument != null) {
            mergedDocument.setCreatorReference(currentDocument.getCreatorReference());
        }
        DocumentReference userReference = configuration != null ? configuration.getAuthorReference() : null;
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
