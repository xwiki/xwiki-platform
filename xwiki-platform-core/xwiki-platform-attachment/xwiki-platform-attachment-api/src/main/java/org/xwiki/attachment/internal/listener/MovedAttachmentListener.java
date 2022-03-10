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
package org.xwiki.attachment.internal.listener;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.attachment.refactoring.event.AttachmentMovedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Listen for moved attachment events and refactor links to the references to the renamed attachment.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@Component
@Singleton
@Named(MovedAttachmentListener.HINT)
public class MovedAttachmentListener implements EventListener
{
    /**
     * Hint for this class.
     */
    public static final String HINT = "org.xwiki.attachment.internal.listener.MovedAttachmentListener";

    @Inject
    private LinkRefactoring linkRefactoring;

    @Inject
    private JobProgressManager progressManager;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private AuthorizationManager authorization;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList(new AttachmentMovedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        AttachmentMovedEvent attachmentMovedEvent = (AttachmentMovedEvent) event;
        MoveAttachmentRequest moveAttachmentRequest = (MoveAttachmentRequest) data;
        if (moveAttachmentRequest.isUpdateReferences()) {
            Predicate<EntityReference> canEdit = reference -> ((!moveAttachmentRequest.isCheckRights()
                || this.authorization.hasAccess(Right.EDIT, moveAttachmentRequest.getUserReference(), reference))
                && (!moveAttachmentRequest.isCheckAuthorRights()
                || this.authorization.hasAccess(Right.EDIT, moveAttachmentRequest.getAuthorReference(), reference)));
            // TODO: Make sure to apply the refactoring at farm level for attachments as part of XWIKI-8346.
            updateBackLinks(attachmentMovedEvent, canEdit);
        }
    }

    private void updateBackLinks(AttachmentMovedEvent event, Predicate<EntityReference> canEdit)
    {
        String wikiId = event.getSourceReference().getDocumentReference().getWikiReference().getName();
        this.logger.info("Updating the back-links for attachment [{}] in wiki [{}].", event.getSourceReference(),
            wikiId);
        List<DocumentReference> documentsList =
            this.modelBridge.getBackLinkedReferences(event.getSourceReference(), wikiId);
        // Since the backlinks are not stored for the document containing the attachment, we need to add it to the 
        // list of documents.
        documentsList.add(event.getSourceReference().getDocumentReference());

        this.progressManager.pushLevelProgress(documentsList.size(), this);

        try {
            for (DocumentReference backlinkDocumentReference : documentsList) {
                this.progressManager.startStep(this);
                if (canEdit.test(backlinkDocumentReference)) {
                    this.linkRefactoring.renameLinks(backlinkDocumentReference, event.getSourceReference(),
                        event.getTargetReference());
                }
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
