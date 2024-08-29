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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.job.JobContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.job.DeleteJob;
import org.xwiki.refactoring.job.DeleteRequest;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutomaticRedirectCreatorListener}.
 *
 * @version $Id$
 */
@ComponentTest
class AutomaticRedirectCreatorListenerTest
{
    @InjectMockComponents
    private AutomaticRedirectCreatorListener listener;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private JobContext jobContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Mock
    private DeleteJob deleteJob;

    private DocumentReference oldReference = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference newReference = new DocumentReference("wiki", "Users", "Bob");

    private DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(oldReference, newReference);

    private DocumentDeletedEvent documentDeletedEvent = new DocumentDeletedEvent(oldReference);

    private MoveRequest renameRequest = new MoveRequest();

    private DeleteRequest deleteRequest = new DeleteRequest();

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.jobContext.getCurrentJob()).thenReturn(deleteJob);
        when(this.deleteJob.getRequest()).thenReturn(deleteRequest);
        deleteRequest.setNewBacklinkTargets(Collections.singletonMap(oldReference, newReference));
    }

    @Test
    void onDocumentRenamedWithAutomaticRedirect()
    {
        renameRequest.setAutoRedirect(true);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.modelBridge).createRedirect(oldReference, newReference);

        assertEquals("Creating automatic redirect from [wiki:Users.Alice] to [wiki:Users.Bob].",
            logCapture.getMessage(0));
    }

    @Test
    void onDocumentRenamedWithoutAutomaticRedirect()
    {
        renameRequest.setAutoRedirect(false);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.modelBridge, never()).createRedirect(any(), any());
    }

    @Test
    void onDocumentRenamedWithoutRenameRequest()
    {
        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.modelBridge).createRedirect(oldReference, newReference);

        assertEquals("Creating automatic redirect from [wiki:Users.Alice] to [wiki:Users.Bob].",
            logCapture.getMessage(0));
    }

    @Test
    void onDocumentDeletedWithAutomaticRedirect()
    {
        deleteRequest.setAutoRedirect(true);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.modelBridge).createRedirect(oldReference, newReference);

        assertEquals("Creating automatic redirect from [wiki:Users.Alice] to [wiki:Users.Bob].",
            logCapture.getMessage(0));
    }

    @Test
    void onDocumentDeletedWithAutomaticRedirectOnDocWithoutNewTarget()
    {
        DocumentReference docReference = new DocumentReference("wiki", "Users", "Carol");
        deleteRequest.setAutoRedirect(true);

        this.listener.onEvent(new DocumentDeletedEvent(docReference), null, null);

        verify(this.modelBridge, never()).createRedirect(eq(docReference), any(DocumentReference.class));
    }

    @Test
    void onDocumentDeletedWithoutAutomaticRedirect()
    {
        deleteRequest.setAutoRedirect(false);

        this.listener.onEvent(documentDeletedEvent, null, deleteRequest);

        verify(this.modelBridge, never()).createRedirect(any(), any());
    }

    @Test
    void onDocumentDeletedWithoutDeleteJob()
    {
        when(this.jobContext.getCurrentJob()).thenReturn(null);

        this.listener.onEvent(documentDeletedEvent, null, null);

        verify(this.modelBridge, never()).createRedirect(any(), any());
    }
}
