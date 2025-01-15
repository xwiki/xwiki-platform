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
package org.xwiki.refactoring.internal.job;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RenameJob}.
 * 
 * @version $Id$
 */
@ComponentTest
class RenameJobTest extends AbstractMoveJobTest
{
    @InjectMockComponents
    private RenameJob renameJob;

    @Override
    protected Job getJob()
    {
        return this.renameJob;
    }

    @Test
    void renameMultipleEntities() throws Throwable
    {
        DocumentReference blackReference = new DocumentReference("wiki", "Color", "Black");
        DocumentReference whiteReference = new DocumentReference("wiki", "Color", "White");
        DocumentReference orangeReference = new DocumentReference("wiki", "Color", "Orange");

        when(this.modelBridge.exists(blackReference)).thenReturn(true);
        when(this.modelBridge.exists(whiteReference)).thenReturn(true);

        MoveRequest request = new MoveRequest();
        request.setEntityReferences(List.of(blackReference, whiteReference));
        request.setDestination(orangeReference);
        request.setCheckAuthorRights(false);
        request.setCheckRights(false);
        run(request);

        verifyNoMove();
        assertEquals(1, getLogCapture().size());
        assertEquals("Cannot rename multiple entities.", getLogCapture().getMessage(0));
    }

    @Test
    void changeEntityType() throws Throwable
    {
        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        SpaceReference bobReference = new SpaceReference("wiki", "Bob");

        run(createRequest(aliceReference, bobReference));

        verifyNoMove();
        assertEquals("You cannot change the entity type (from [DOCUMENT] to [SPACE]).", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void convertNotTerminalDocumentToTerminalDocumentPreservingChildren() throws Throwable
    {
        DocumentReference nonTerminalReference = new DocumentReference("wiki", "One", "WebHome");
        DocumentReference terminalReference = new DocumentReference("wiki", "Zero", "One");

        MoveRequest request = createRequest(nonTerminalReference, terminalReference);
        request.setDeep(true);
        run(request);

        verifyNoMove();
        assertEquals("You cannot transform a non-terminal document [wiki:One.WebHome] into a terminal document "
            + "[wiki:Zero.One] and preserve its child documents at the same time.", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void renameSpaceHomeDeep() throws Throwable
    {
        DocumentReference aliceReference = new DocumentReference("wiki", "Alice", "WebHome");
        DocumentReference bobReference = new DocumentReference("wiki", "Bob", "WebHome");

        MoveRequest request = createRequest(aliceReference, bobReference);
        request.setDeep(true);
        run(request);

        // We verify that job fetches the space children.
        verify(this.modelBridge, atLeastOnce()).getDocumentReferences(aliceReference.getLastSpaceReference());
    }

    @Test
    void renameSpace() throws Throwable
    {
        SpaceReference aliceReference = new SpaceReference("wiki", "Alice");
        SpaceReference bobReference = new SpaceReference("wiki", "Bob");

        DocumentReference aliceWebHomeReference = new DocumentReference("wiki", "Alice", "WebHome");
        DocumentReference bobWebHomeReference = new DocumentReference("wiki", "Bob", "WebHome");
        when(this.modelBridge.getDocumentReferences(aliceReference)).thenReturn(List.of(aliceWebHomeReference));
        when(this.modelBridge.exists(aliceWebHomeReference)).thenReturn(true);
        MoveRequest request = createRequest(aliceReference, bobReference);
        request.setCheckRights(false);
        run(request);

        // We verify that job fetches the space children.
        verify(this.modelBridge, times(2)).getDocumentReferences(aliceReference);
        verify(this.modelBridge).rename(aliceWebHomeReference, bobWebHomeReference);
    }

    @Test
    void renameDocument() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "Old");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference newReference = new DocumentReference("wiki", "Space", "New");
        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        when(this.modelBridge.rename(oldReference, newReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);
        request.setUserReference(userReference);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).rename(oldReference, newReference);
    }
}
