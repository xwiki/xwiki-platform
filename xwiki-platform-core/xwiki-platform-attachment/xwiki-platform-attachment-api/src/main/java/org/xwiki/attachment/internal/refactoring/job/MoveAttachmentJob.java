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
package org.xwiki.attachment.internal.refactoring.job;

import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.attachment.refactoring.event.AttachmentMovedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.internal.job.AbstractEntityJob;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD;

/**
 * This is the job in charge of moving attachments.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Named(MoveAttachmentJob.HINT)
public class MoveAttachmentJob
    extends AbstractEntityJob<MoveAttachmentRequest, EntityJobStatus<MoveAttachmentRequest>>
{
    /**
     * The hint for this job.
     */
    public static final String HINT = "refactoring/attachment/move";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private AttachmentsManager attachmentsManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Override
    public String getType()
    {
        return HINT;
    }

    @Override
    protected void process(EntityReference source)
    {
        this.progressManager.pushLevelProgress(2, this);
        AttachmentReference destination = this.request.getProperty(MoveAttachmentRequest.DESTINATION);
        boolean autoRedirect = this.request.getProperty(MoveAttachmentRequest.AUTO_REDIRECT);

        XWiki wiki = this.xcontextProvider.get().getWiki();

        // Update the author for the attribution of the attachment uploader.
        this.modelBridge.setContextUserReference(this.request.getUserReference());
        try {
            if (checkMoveRights(source, destination)) {
                this.progressManager.startStep(this);
                moveAttachment(source, destination, autoRedirect, wiki);
                this.progressManager.endStep(this);

                this.progressManager.startStep(this);
                this.observationManager.notify(new AttachmentMovedEvent((AttachmentReference) source, destination),
                    this,
                    this.request);
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private boolean checkMoveRights(EntityReference source, EntityReference destination)
    {
        // Check view and edit right on source to ensure that the user doesn't get view right through the move.
        // While edit right implies view right, the view right might be explicitly denied.
        boolean hasSourceRight = hasAccess(Right.VIEW, source) && hasAccess(Right.EDIT, source);
        boolean hasDestinationRight = hasAccess(Right.EDIT, destination);

        if (!hasSourceRight) {
            this.logger.error("You don't have sufficient permissions over the source attachment [{}].", source);
        } else if (!hasDestinationRight) {
            // The destination's document might be the same as the source, therefore, only log the error when there is
            // no error regarding the source.
            this.logger.error("You don't have sufficient permissions over the destination attachment [{}].",
                destination);
        }

        return hasSourceRight && hasDestinationRight;
    }

    private void moveAttachment(EntityReference source, AttachmentReference destination, boolean autoRedirect,
        XWiki wiki)
    {
        try {
            XWikiDocument sourceDocument = wiki.getDocument(source.getParent(), this.xcontextProvider.get());
            XWikiDocument targetDocument = wiki.getDocument(destination.getParent(), this.xcontextProvider.get());
            XWikiAttachment sourceAttachment = sourceDocument.getExactAttachment(source.getName());

            // Update the author of the source and target documents.
            UserReference authorUserReference =
                this.documentReferenceUserReferenceResolver.resolve(this.request.getUserReference());
            sourceDocument.getAuthors().setEffectiveMetadataAuthor(authorUserReference);
            sourceDocument.getAuthors().setOriginalMetadataAuthor(authorUserReference);
            targetDocument.getAuthors().setEffectiveMetadataAuthor(authorUserReference);
            targetDocument.getAuthors().setOriginalMetadataAuthor(authorUserReference);

            // Remove the original attachment and create a new one with the same name.
            sourceDocument.removeAttachment(sourceAttachment);

            addAttachment(targetDocument, sourceAttachment, destination.getName());

            this.attachmentsManager.removeExistingRedirection(destination.getName(), targetDocument);

            if (autoRedirect) {
                initializeAutoRedirection(source, destination, sourceDocument);
            }

            if (Objects.equals(source.getParent(), destination.getParent())) {
                wiki.saveDocument(sourceDocument,
                    this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
                        source.getName(), destination.getName()),
                    this.xcontextProvider.get());
            } else {
                transactionalMove(wiki, sourceDocument, targetDocument, sourceAttachment.getFilename(),
                    destination.getName());
            }
        } catch (XWikiException | IOException e) {
            this.logger.warn("Failed to move attachment [{}] to [{}]. Cause: [{}]", source, destination,
                getRootCauseMessage(e));
        }
    }

    /**
     * Move the attachment from one document to another. In case of failure when saving the target document, the source
     * document is rollback with its attachment.
     *
     * @param wiki the wiki instance used to perform the documents save
     * @param sourceDocument the source document, containing the attachment to move
     * @param targetDocument the target document, in which to move the attachment
     * @param sourceFileName the file name of the source attachment
     * @param targetFileName the file name of the target attachment
     * @throws XWikiException in case of error when save the document
     * @throws IOException in case of error when putting back the attachment in the source document during the
     *     rollback
     */
    private void transactionalMove(XWiki wiki, XWikiDocument sourceDocument, XWikiDocument targetDocument,
        String sourceFileName, String targetFileName) throws XWikiException, IOException
    {
        String sourceSerialized = this.referenceSerializer.serialize(sourceDocument.getDocumentReference());
        String destinationSerialized = this.referenceSerializer.serialize(targetDocument.getDocumentReference());
        String historyMessageSource =
            this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
                destinationSerialized);
        String historyMessageTarget =
            this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
                sourceSerialized);
        wiki.saveDocument(sourceDocument, historyMessageSource, this.xcontextProvider.get());
        try {
            wiki.saveDocument(targetDocument, historyMessageTarget, this.xcontextProvider.get());
        } catch (Exception e) {
            // In case of failure during the save of the second document, we rollback the first one to its initial state
            // (i.e., with the attachment added again).
            // Remove the added attachment from the target document.
            XWikiAttachment attachment = targetDocument.getExactAttachment(targetFileName);
            addAttachment(sourceDocument, attachment, sourceFileName);
            targetDocument.removeAttachment(attachment);
            String historyMessageRollbackTarget =
                this.contextualLocalizationManager.getTranslationPlain("attachment.job.rollbackDocument.target",
                    sourceFileName, sourceSerialized);
            wiki.saveDocument(sourceDocument, historyMessageRollbackTarget, true, this.xcontextProvider.get());
            // We re-throw the exception since at the end, the job failed.
            throw e;
        }
    }

    private void initializeAutoRedirection(EntityReference source, AttachmentReference destination,
        XWikiDocument sourceDocument)
        throws XWikiException
    {
        int idx = sourceDocument.createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE,
            this.xcontextProvider.get());
        BaseObject xObject =
            sourceDocument.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, idx);
        if (xObject != null) {
            xObject.setStringValue(SOURCE_NAME_FIELD, source.getName());
            xObject.setStringValue(TARGET_LOCATION_FIELD,
                this.entityReferenceSerializer.serialize(destination.getParent()));
            xObject.setStringValue(TARGET_NAME_FIELD, destination.getName());
        }
    }

    private void addAttachment(XWikiDocument targetDocument, XWikiAttachment oldAttachment, String newName)
        throws IOException, XWikiException
    {
        // Clone the attachment and its history to the new document, with the new name.
        XWikiAttachment newAttachment = oldAttachment.clone(newName, this.xcontextProvider.get());
        newAttachment.setDoc(targetDocument);
        targetDocument.setAttachment(newAttachment);
    }
}
