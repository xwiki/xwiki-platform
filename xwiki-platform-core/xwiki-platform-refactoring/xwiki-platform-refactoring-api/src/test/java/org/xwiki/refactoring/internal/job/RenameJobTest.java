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

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RenameJob}.
 * 
 * @version $Id$
 */
public class RenameJobTest extends AbstractMoveJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(RenameJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    @Test
    public void renameMultipleEntities() throws Throwable
    {
        DocumentReference blackReference = new DocumentReference("wiki", "Color", "Black");
        DocumentReference whiteReference = new DocumentReference("wiki", "Color", "White");
        DocumentReference orangeReference = new DocumentReference("wiki", "Color", "Orange");

        MoveRequest request = new MoveRequest();
        request.setEntityReferences(Arrays.<EntityReference>asList(blackReference, whiteReference));
        request.setDestination(orangeReference);
        run(request);

        verifyNoMove();
    }

    @Test
    public void changeEntityType() throws Throwable
    {
        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        SpaceReference bobReference = new SpaceReference("wiki", "Bob");

        run(createRequest(aliceReference, bobReference));

        verifyNoMove();
        verify(this.mocker.getMockedLogger()).error("You cannot change the entity type (from [{}] to [{}]).",
            aliceReference.getType(), bobReference.getType());
    }

    @Test
    public void convertNotTerminalDocumentToTerminalDocumentPreservingChildren() throws Throwable
    {
        DocumentReference nonTerminalReference = new DocumentReference("wiki", "One", "WebHome");
        DocumentReference terminalReference = new DocumentReference("wiki", "Zero", "One");

        MoveRequest request = createRequest(nonTerminalReference, terminalReference);
        request.setDeep(true);
        run(request);

        verifyNoMove();
        verify(this.mocker.getMockedLogger()).error(
            "You cannot transform a non-terminal document [{}] into a terminal document [{}]"
                + " and preserve its child documents at the same time.", nonTerminalReference, terminalReference);
    }

    @Test
    public void renameSpaceHomeDeep() throws Throwable
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
    public void renameSpace() throws Throwable
    {
        SpaceReference aliceReference = new SpaceReference("wiki", "Alice");
        SpaceReference bobReference = new SpaceReference("wiki", "Bob");

        run(createRequest(aliceReference, bobReference));

        // We verify that job fetches the space children.
        verify(this.modelBridge, times(2)).getDocumentReferences(aliceReference);
    }

    @Test
    public void renameDocument() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "Old");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference newReference = new DocumentReference("wiki", "Space", "New");
        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        when(this.modelBridge.copy(oldReference, newReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference);
        request.setCheckRights(false);
        request.setInteractive(false);
        request.setUserReference(userReference);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).delete(oldReference);
        verify(this.modelBridge).createRedirect(oldReference, newReference);
    }
}
