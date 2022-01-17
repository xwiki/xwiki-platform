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
package org.xwiki.attachment.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.MoveAttachmentRequest;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.internal.job.MoveAttachmentJob;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Provides the operations related to attachments manipulation. In particular regarding the operations to move
 * attachments.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named("attachment")
@Unstable
public class AttachmentScriptService implements ScriptService
{
    @Inject
    private AttachmentsManager attachmentsManager;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    /**
     * Creates an attachment move request.
     *
     * @param sourceLocation the location of the document containing the source attachment
     * @param sourceName the name of the source attachment
     * @param targetLocation the target location of the document containing the attachment
     * @param targetName the target name of the attachment
     * @param autoRedirect if {@code true} a redirection will be set from the source location to the target
     *     location
     * @param isAsync {@code true} if the job can be executed asynchronously, {@code false} otherwise
     * @return the initialized move attachment request
     */
    public MoveAttachmentRequest createMoveRequest(DocumentReference sourceLocation, String sourceName,
        DocumentReference targetLocation, String targetName, boolean autoRedirect, boolean isAsync)
    {
        MoveAttachmentRequest request = new MoveAttachmentRequest();
        request.setEntityReferences(singletonList(new AttachmentReference(sourceName, sourceLocation)));
        request.setProperty(MoveAttachmentRequest.DESTINATION, new AttachmentReference(targetName, targetLocation));
        request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, autoRedirect);
        request.setInteractive(isAsync);
        return request;
    }

    /**
     * Creates an attachment move job from a request.
     *
     * @param request a move attachment request to start a job from
     * @return the initialized move attachment request
     */
    public Job createMoveJob(MoveAttachmentRequest request) throws JobException
    {
        return this.jobExecutor.execute(MoveAttachmentJob.HINT, request);
    }

    /**
     * @param documentReference the document reference containing the attachment
     * @param attachmentName the name of the attachment
     * @return {@code true} if the attachment exists or has existed (but was moved with a redirection) at the requested
     *     location.
     */
    public boolean locationAvailable(DocumentReference documentReference, String attachmentName)
    {
        try {
            return this.attachmentsManager.available(new AttachmentReference(attachmentName, documentReference));
        } catch (XWikiException e) {
            this.logger.warn("Failed to check if [{}] exists [{}]. Cause: [{}].", attachmentName, documentReference,
                getRootCauseMessage(e));
            return false;
        }
    }
}
