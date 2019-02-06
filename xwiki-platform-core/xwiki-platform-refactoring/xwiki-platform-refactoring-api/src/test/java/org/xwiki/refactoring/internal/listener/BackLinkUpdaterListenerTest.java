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

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.job.RenameJob;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BackLinkUpdaterListener}.
 * 
 * @version $Id$
 */
@ComponentTest
public class BackLinkUpdaterListenerTest
{
    @InjectMockComponents
    private BackLinkUpdaterListener listener;

    @MockComponent
    private LinkRefactoring linkRefactoring;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    private DocumentReference aliceReference = new DocumentReference("foo", "Users", "Alice");

    private DocumentReference bobReference = new DocumentReference("foo", "Users", "Bob");

    private DocumentReference carolReference = new DocumentReference("foo", "Users", "Carol");

    private DocumentReference denisReference = new DocumentReference("bar", "Users", "Denis");

    private DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(aliceReference, bobReference);

    @Mock
    private RenameJob renameJob;

    private MoveRequest renameRequest = new MoveRequest();

    @BeforeEach
    public void configure() throws Exception
    {
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));
        when(this.modelBridge.getBackLinkedReferences(aliceReference, "foo")).thenReturn(Arrays.asList(carolReference));
        when(this.modelBridge.getBackLinkedReferences(aliceReference, "bar")).thenReturn(Arrays.asList(denisReference));
    }

    @Test
    public void onDocumentRenamedWithUpdateLinksOnFarm()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);
    }

    @Test
    public void onDocumentRenamedWithUpdateLinksOnFarmAndNoEditRight()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(true);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.renameJob.hasAccess(Right.EDIT, denisReference)).thenReturn(false);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(), any());
    }

    @Test
    public void onDocumentRenamedWithUpdateLinksOnWiki()
    {
        renameRequest.setUpdateLinks(true);
        renameRequest.setUpdateLinksOnFarm(false);

        when(this.renameJob.hasAccess(Right.EDIT, carolReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring, never()).renameLinks(eq(denisReference), any(), any());
    }

    @Test
    public void onDocumentRenamedWithoutUpdateLinks()
    {
        renameRequest.setUpdateLinks(false);
        renameRequest.setUpdateLinksOnFarm(true);

        this.listener.onEvent(documentRenamedEvent, renameJob, renameRequest);

        verify(this.linkRefactoring, never()).renameLinks(any(), any(), any());
    }

    @Test
    public void onDocumentRenamedWithoutRenameJob()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.linkRefactoring).renameLinks(carolReference, aliceReference, bobReference);
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);
    }

    @Test
    public void onDocumentRenamedWithoutRenameJobAndNoEditRight()
    {
        when(this.authorization.hasAccess(Right.EDIT, carolReference)).thenReturn(false);
        when(this.authorization.hasAccess(Right.EDIT, denisReference)).thenReturn(true);

        this.listener.onEvent(documentRenamedEvent, null, null);

        verify(this.linkRefactoring, never()).renameLinks(eq(carolReference), any(), any());
        verify(this.linkRefactoring).renameLinks(denisReference, aliceReference, bobReference);
    }

    @Test
    public void onOtherEvents()
    {
        this.listener.onEvent(null, null, null);

        verify(this.linkRefactoring, never()).renameLinks(any(), any(), any());
    }
}
