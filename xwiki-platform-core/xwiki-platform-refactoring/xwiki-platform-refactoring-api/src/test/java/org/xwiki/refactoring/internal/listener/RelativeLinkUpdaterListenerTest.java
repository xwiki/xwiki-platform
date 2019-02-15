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
package org.xwiki.refactoring.internal.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentCopiedEvent;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RelativeLinkUpdaterListener}.
 * 
 * @version $Id$
 */
@ComponentTest
public class RelativeLinkUpdaterListenerTest
{
    @InjectMockComponents
    private RelativeLinkUpdaterListener listener;

    @MockComponent
    private LinkRefactoring linkRefactoring;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private DocumentReference oldReference = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference newReference = new DocumentReference("wiki", "Users", "Bob");

    private DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(oldReference, newReference);

    private DocumentCopiedEvent documentCopiedEvent = new DocumentCopiedEvent(oldReference, newReference);

    private MoveRequest renameRequest = new MoveRequest();

    private CopyRequest copyRequest = new CopyRequest();

    @Test
    public void onDocumentRenamedWithUpdateLinks()
    {
        renameRequest.setUpdateLinks(true);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.linkRefactoring).updateRelativeLinks(oldReference, newReference);

        assertEquals("Updating the relative links from [wiki:Users.Bob].", logCapture.getMessage(0));
    }

    @Test
    public void onDocumentRenamedWithoutUpdateLinks()
    {
        renameRequest.setUpdateLinks(false);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.linkRefactoring, never()).updateRelativeLinks(any(), any());
    }

    @Test
    public void onDocumentRenamedWithoutRenameRequest()
    {
        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.linkRefactoring).updateRelativeLinks(oldReference, newReference);

        assertEquals("Updating the relative links from [wiki:Users.Bob].", logCapture.getMessage(0));
    }

    @Test
    public void onDocumentCopiedWithUpdateLinks()
    {
        copyRequest.setUpdateLinks(true);

        this.listener.onEvent(documentCopiedEvent, null, copyRequest);

        verify(this.linkRefactoring).updateRelativeLinks(oldReference, newReference);

        assertEquals("Updating the relative links from [wiki:Users.Bob].", logCapture.getMessage(0));
    }

    @Test
    public void onDocumentCopiedWithoutUpdateLinks()
    {
        copyRequest.setUpdateLinks(false);

        this.listener.onEvent(documentCopiedEvent, null, copyRequest);

        verify(this.linkRefactoring, never()).updateRelativeLinks(any(), any());
    }

    @Test
    public void onDocumentCopiedWithoutCopyRequest()
    {
        this.listener.onEvent(documentCopiedEvent, null, null);

        verify(this.linkRefactoring).updateRelativeLinks(oldReference, newReference);

        assertEquals("Updating the relative links from [wiki:Users.Bob].", logCapture.getMessage(0));
    }

    @Test
    public void onOtherEvents()
    {
        this.listener.onEvent(null, null, null);

        verify(this.linkRefactoring, never()).updateRelativeLinks(any(), any());
    }
}
