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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.attachment.refactoring.event.AttachmentMovedEvent;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.RefactoringException;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MovedAttachmentListener}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class MovedAttachmentListenerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final AttachmentReference SOURCE_ATTACHMENT = new AttachmentReference("oldname", DOCUMENT_REFERENCE);

    private static final AttachmentReference TARGET_ATTACHMENT = new AttachmentReference("newname", DOCUMENT_REFERENCE);

    @InjectMockComponents
    private MovedAttachmentListener listener;

    @MockComponent
    private ReferenceUpdater linkRefactoring;

    @MockComponent
    private JobProgressManager progressManager;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private AuthorizationManager authorization;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void onEventNoUpdateReference()
    {
        MoveAttachmentRequest data = new MoveAttachmentRequest();
        data.setProperty(MoveAttachmentRequest.UPDATE_REFERENCES, false);
        this.listener.onEvent(new AttachmentMovedEvent(SOURCE_ATTACHMENT, TARGET_ATTACHMENT), null, data);
        verifyNoInteractions(this.linkRefactoring);
        verifyNoInteractions(this.progressManager);
        verifyNoInteractions(this.modelBridge);
        verifyNoInteractions(this.authorization);
    }

    @Test
    void onEvent() throws RefactoringException
    {
        MoveAttachmentRequest data = new MoveAttachmentRequest();
        AttachmentMovedEvent event = new AttachmentMovedEvent(SOURCE_ATTACHMENT, TARGET_ATTACHMENT);
        DocumentReference d1 = new DocumentReference("wiki", "space", "page1");
        DocumentReference d2 = new DocumentReference("wiki", "space", "page2");

        when(this.authorization.hasAccess(eq(Right.EDIT), any(), any())).thenReturn(true);
        when(this.modelBridge.getBackLinkedDocuments(SOURCE_ATTACHMENT)).thenReturn(Set.of(d1, d2));

        this.listener.onEvent(event, null, data);

        verify(this.progressManager).pushLevelProgress(3, this.listener);
        verify(this.progressManager, times(3)).startStep(this.listener);
        verify(this.progressManager, times(3)).endStep(this.listener);
        verify(this.progressManager).popLevelProgress(this.listener);
        verify(this.linkRefactoring).update(d1, SOURCE_ATTACHMENT, TARGET_ATTACHMENT);
        verify(this.linkRefactoring).update(d2, SOURCE_ATTACHMENT, TARGET_ATTACHMENT);
        verify(this.linkRefactoring).update(DOCUMENT_REFERENCE, SOURCE_ATTACHMENT, TARGET_ATTACHMENT);
        assertEquals(1, this.logCapture.size());
        assertEquals("Updating the back-links for attachment [Attachment wiki:space.page@oldname].",
            this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }
}
