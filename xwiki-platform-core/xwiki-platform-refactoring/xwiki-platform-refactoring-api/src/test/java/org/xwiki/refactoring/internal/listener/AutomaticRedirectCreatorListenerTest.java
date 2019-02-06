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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AutomaticRedirectCreatorListener}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AutomaticRedirectCreatorListenerTest
{
    @InjectMockComponents
    private AutomaticRedirectCreatorListener listener;

    @MockComponent
    private ModelBridge modelBridge;

    private DocumentReference oldReference = new DocumentReference("wiki", "Users", "Alice");

    private DocumentReference newReference = new DocumentReference("wiki", "Users", "Bob");

    private DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(oldReference, newReference);

    private MoveRequest renameRequest = new MoveRequest();

    @Test
    public void onDocumentRenamedWithAutomaticRedirect()
    {
        renameRequest.setAutoRedirect(true);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.modelBridge).createRedirect(oldReference, newReference);
    }

    @Test
    public void onDocumentRenamedWithoutAutomaticRedirect()
    {
        renameRequest.setAutoRedirect(false);

        this.listener.onEvent(documentRenamedEvent, null, renameRequest);

        verify(this.modelBridge, never()).createRedirect(any(), any());
    }

    @Test
    public void onDocumentRenamedWithoutRenameRequest()
    {
        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.modelBridge).createRedirect(oldReference, newReference);
    }

    @Test
    public void onOtherEvents()
    {
        this.listener.onEvent(null, null, null);

        verify(this.modelBridge, never()).createRedirect(any(), any());
    }
}
