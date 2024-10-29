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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.event.status.QuestionAskedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.refactoring.event.EntitiesRenamedEvent;
import org.xwiki.refactoring.event.EntitiesRenamingEvent;
import org.xwiki.refactoring.job.EntityJobStatus;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.OverwriteQuestion;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 */
@ComponentTest
public class MoveJobTest extends AbstractMoveJobTest
{
    @InjectMockComponents
    private MoveJob moveJob;

    @MockComponent
    private ObservationManager observationManager;

    @Override
    protected Job getJob()
    {
        return moveJob;
    }

    @Test
    void moveInsideItsOwnHierarchy() throws Throwable
    {
        SpaceReference spaceReference =
            new SpaceReference("Entity", new SpaceReference("Model", new WikiReference("code")));

        run(createRequest(spaceReference.getParent(), spaceReference));
        assertEquals("Cannot make [Space code:Model] a descendant of itself.", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveToTheSameLocation() throws Throwable
    {
        SpaceReference spaceReference =
            new SpaceReference("Entity", new SpaceReference("Model", new WikiReference("code")));

        run(createRequest(spaceReference, spaceReference.getParent()));
        assertEquals("Cannot move [Space code:Model.Entity] into [Space code:Model], it's already there.",
            getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveUnsupportedEntity() throws Throwable
    {
        run(createRequest(new WikiReference("from"), new WikiReference("to")));
        assertEquals("Unsupported entity type [WIKI].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        assertEquals("Unsupported source entity type [WIKI].", getLogCapture().getMessage(1));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveToUnsupportedDestination() throws Throwable
    {
        run(createRequest(new DocumentReference("wiki", "Space", "Page"), new WikiReference("test")));
        assertEquals("Unsupported destination entity type [WIKI] for a document.", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        run(createRequest(new DocumentReference("wiki", "Space", "Page"), new DocumentReference("test", "A", "B")));
        assertEquals("Unsupported destination entity type [DOCUMENT] for a document.", getLogCapture().getMessage(1));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        run(createRequest(new SpaceReference("Space", new WikiReference("wiki")),
            new DocumentReference("test", "A", "B")));
        assertEquals("Unsupported destination entity type [DOCUMENT] for a space.", getLogCapture().getMessage(2));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveMissingDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("foo", "A", "Page");
        run(createRequest(sourceReference, new SpaceReference("B", new WikiReference("bar"))));
        assertEquals("Skipping [foo:A.Page] because it doesn't exist.", getLogCapture().getMessage(0));
        assertEquals(Level.WARN, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveDocumentWithoutDeleteRight() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(false);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, documentReference)).thenReturn(true);

        MoveRequest request = createRequest(documentReference, new SpaceReference("Foo", new WikiReference("bar")));
        request.setCheckRights(true);
        request.setUserReference(userReference);
        request.setCheckAuthorRights(true);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You are not allowed to delete [wiki:Space.Page].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveDocumentWithoutDeleteRightAuthor() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, documentReference)).thenReturn(false);

        MoveRequest request = createRequest(documentReference, new SpaceReference("Foo", new WikiReference("bar")));
        request.setCheckRights(true);
        request.setUserReference(userReference);
        request.setCheckAuthorRights(true);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You are not allowed to delete [wiki:Space.Page].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveDocumentToRestrictedDestination() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(false);
        when(this.authorization.hasAccess(Right.VIEW, userReference, oldReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, oldReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You don't have sufficient permissions over the destination document [wiki:Two.Page].",
            getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveDocumentToRestrictedDestinationAuthor() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, oldReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, newReference)).thenReturn(false);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, oldReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You don't have sufficient permissions over the destination document [wiki:Two.Page].",
            getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());

        verifyNoMove();
    }

    @Test
    void moveDocumentFromRestrictedSource() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, oldReference)).thenReturn(false);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, oldReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You don't have sufficient permissions over the source document [wiki:One.Page].",
            getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
        verifyNoMove();
    }

    @Test
    void moveDocumentFromRestrictedSourceAuthor() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, oldReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, newReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, authorReference, oldReference)).thenReturn(false);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        run(request);

        assertEquals("You don't have sufficient permissions over the source document [wiki:One.Page].",
            getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
        verifyNoMove();
    }

    @Test
    void moveDocumentToSpace() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(newReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        when(this.modelBridge.rename(oldReference, newReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);
        request.setUserReference(userReference);
        Job job = run(request);

        verify(this.observationManager).notify(any(EntitiesRenamingEvent.class), same(job), same(request));
        verify(this.observationManager).notify(new DocumentRenamingEvent(oldReference, newReference), job, request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).rename(oldReference, newReference);

        verify(this.observationManager).notify(new DocumentRenamedEvent(oldReference, newReference), job, request);
        verify(this.observationManager).notify(any(EntitiesRenamedEvent.class), same(job), same(request));
    }

    @Test
    void cancelEntitiesRenamingEvent() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(newReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);

        doAnswer((Answer<Void>) invocation -> {
            ((EntitiesRenamingEvent) invocation.getArgument(0)).cancel();
            return null;
        }).when(this.observationManager).notify(any(EntitiesRenamingEvent.class), any(MoveJob.class), same(request));

        Job job = run(request);

        verify(this.observationManager).notify(any(EntitiesRenamingEvent.class), same(job), same(request));
        verify(this.observationManager, never()).notify(any(DocumentRenamingEvent.class), any(), any());

        verify(this.modelBridge, never()).delete(any());
        verify(this.modelBridge, never()).copy(any(), any());

        verify(this.observationManager, never()).notify(any(DocumentRenamedEvent.class), any(), any());
        verify(this.observationManager, never()).notify(any(EntitiesRenamedEvent.class), any(), any());
    }

    @Test
    void cancelDocumentRenamingEvent() throws Throwable
    {
        SpaceReference sourceReference = new SpaceReference("wiki", "Source");
        DocumentReference oldAliceReference = new DocumentReference("Alice", sourceReference);
        when(this.modelBridge.exists(oldAliceReference)).thenReturn(true);
        DocumentReference oldBobReference = new DocumentReference("Bob", sourceReference);
        when(this.modelBridge.exists(oldBobReference)).thenReturn(true);
        when(this.modelBridge.getDocumentReferences(sourceReference))
            .thenReturn(List.of(oldAliceReference, oldBobReference));

        SpaceReference destinationReference = new SpaceReference("wiki", "Destination");
        DocumentReference newAliceReference =
            new DocumentReference("Alice", new SpaceReference("Source", destinationReference));
        DocumentReference newBobReference =
            new DocumentReference("Bob", new SpaceReference("Source", destinationReference));
        when(this.modelBridge.rename(oldBobReference, newBobReference)).thenReturn(true);

        MoveRequest request = createRequest(sourceReference, destinationReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);

        // Cancel the rename of the first document.
        doAnswer((Answer<Void>) invocation -> {
            ((DocumentRenamingEvent) invocation.getArgument(0)).cancel();
            return null;
        }).when(this.observationManager).notify(eq(new DocumentRenamingEvent(oldAliceReference, newAliceReference)),
            any(MoveJob.class), same(request));

        Job job = run(request);

        verify(this.observationManager).notify(any(EntitiesRenamingEvent.class), same(job), same(request));

        // The rename of the first document is canceled.
        verify(this.observationManager).notify(new DocumentRenamingEvent(oldAliceReference, newAliceReference), job,
            request);
        verify(this.modelBridge, never()).rename(oldAliceReference, newAliceReference);
        verify(this.observationManager, never()).notify(new DocumentRenamedEvent(oldAliceReference, newAliceReference),
            job, request);

        // The second document is still renamed.
        verify(this.observationManager).notify(new DocumentRenamingEvent(oldBobReference, newBobReference), job,
            request);
        verify(this.modelBridge).rename(oldBobReference, newBobReference);
        verify(this.observationManager).notify(new DocumentRenamedEvent(oldBobReference, newBobReference), job,
            request);

        verify(this.observationManager).notify(any(EntitiesRenamedEvent.class), same(job), same(request));
    }

    @Test
    void moveDocumentToSpaceHome() throws Throwable
    {
        DocumentReference source = new DocumentReference("wiki", "A", "B");
        when(this.modelBridge.exists(source)).thenReturn(true);
        DocumentReference destination = new DocumentReference("wiki", "C", "WebHome");

        when(this.modelBridge.rename(source, new DocumentReference("wiki", "C", "B"))).thenReturn(true);

        MoveRequest request = createRequest(source, destination);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).rename(source, new DocumentReference("wiki", "C", "B"));
    }

    @Test
    void moveSpaceHomeDeep() throws Throwable
    {
        DocumentReference spaceHome = new DocumentReference("chess", List.of("A", "B", "C"), "WebHome");
        DocumentReference docFromSpace = new DocumentReference("X", spaceHome.getLastSpaceReference());
        when(this.modelBridge.getDocumentReferences(spaceHome.getLastSpaceReference()))
            .thenReturn(List.of(docFromSpace));
        when(this.modelBridge.exists(docFromSpace)).thenReturn(true);

        WikiReference newWiki = new WikiReference("tennis");

        MoveRequest request = createRequest(spaceHome, newWiki);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setDeep(true);
        run(request);

        verify(this.modelBridge).rename(docFromSpace, new DocumentReference("tennis", "C", "X"));

        verify(this.observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class),
            eq(Map.of(docFromSpace, new EntitySelection(docFromSpace))));
    }

    @Test
    void moveSpaceToSpaceHome() throws Throwable
    {
        SpaceReference sourceSpace = new SpaceReference("wiki", "A", "B");
        DocumentReference sourceDoc = new DocumentReference("X", sourceSpace);
        when(this.modelBridge.getDocumentReferences(sourceSpace)).thenReturn(List.of(sourceDoc));
        when(this.modelBridge.exists(sourceDoc)).thenReturn(true);

        DocumentReference destination = new DocumentReference("wiki", "C", "WebHome");

        MoveRequest request = createRequest(sourceSpace, destination);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).rename(sourceDoc, new DocumentReference("wiki", List.of("C", "B"), "X"));
    }

    @Test
    void getGroupPath()
    {
        DocumentReference alice = new DocumentReference("chess", List.of("A", "B"), "C");
        DocumentReference bob = new DocumentReference("chess", List.of("A", "B"), "D");
        DocumentReference carol = new DocumentReference("chess", List.of("A", "E"), "F");

        MoveRequest request = new MoveRequest();
        request.setEntityReferences(List.of(alice, bob));
        request.setDestination(carol);
        this.moveJob.initialize(request);

        assertEquals(new JobGroupPath(List.of(RefactoringJobs.GROUP, "chess", "A")), this.moveJob.getGroupPath());
    }

    @Test
    void cancelMove() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", List.of("Path", "To"), "Source");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);
        DocumentReference destinationReference = new DocumentReference("wiki", "Destination", "WebHome");
        MoveRequest request = createRequest(sourceReference, destinationReference);
        request.setCheckRights(false);

        doAnswer((Answer<Void>) invocation -> {
            DocumentsDeletingEvent event = invocation.getArgument(0);
            event.cancel();
            return null;
        }).when(this.observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class), any(Map.class));

        run(request);

        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }

    @Test
    void checkEntitySelection() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", List.of("Path", "To"), "Source");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);
        DocumentReference destinationReference = new DocumentReference("wiki", "Destination", "WebHome");
        MoveRequest request = createRequest(sourceReference, destinationReference);
        request.setCheckRights(false);

        doAnswer((Answer<Void>) invocation -> {
            Map<EntityReference, EntitySelection> concernedEntities = invocation.getArgument(2);
            concernedEntities.get(sourceReference).setSelected(false);
            return null;
        }).when(this.observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class), any(Map.class));

        run(request);

        verify(this.modelBridge, never()).copy(eq(sourceReference), any(DocumentReference.class));
    }

    @Test
    void visitDocuments() throws Exception
    {
        SpaceReference sourceReference = new SpaceReference("wiki", "Path", "To", "Space");
        SpaceReference destinationReference = new SpaceReference("wiki", "Other", "Space");
        SpaceReference nestedSpaceReference = new SpaceReference("WebPreferences", sourceReference);
        DocumentReference one = new DocumentReference("WebPreference", nestedSpaceReference);
        DocumentReference two = new DocumentReference("WebHome", nestedSpaceReference);
        DocumentReference three = new DocumentReference("ZZZ", sourceReference);
        DocumentReference four = new DocumentReference("WebPreferences", sourceReference);
        DocumentReference five = new DocumentReference("AAA", sourceReference);
        DocumentReference six = new DocumentReference("WebHome", sourceReference);
        when(this.modelBridge.getDocumentReferences(sourceReference))
            .thenReturn(List.of(one, two, three, four, five, six));
        this.moveJob.initialize(createRequest(sourceReference, destinationReference));

        List<DocumentReference> visitedPages = new ArrayList<>();
        this.moveJob.visitDocuments(sourceReference, visitedPages::add);

        assertEquals(List.of(five, six, two, one, three, four), visitedPages);
    }

    @Test
    void confirmOverwrite() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference spaceHomReference = new DocumentReference("wiki", "Two", "WebHome");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        MoveRequest request = createRequest(oldReference, spaceHomReference);
        request.setInteractive(true);
        request.setCheckRights(false);

        when(this.modelBridge.exists(oldReference)).thenReturn(true);
        when(this.modelBridge.exists(newReference)).thenReturn(true);
        when(this.modelBridge.canOverwriteSilently(newReference)).thenReturn(false);

        doAnswer(invocationOnMock -> {
            QuestionAskedEvent questionAskedEvent = invocationOnMock.getArgument(0);
            assertEquals("org.xwiki.refactoring.job.OverwriteQuestion", questionAskedEvent.getQuestionType());
            questionAskedEvent.answered();
            return null;
        }).when(this.observationManager).notify(any(QuestionAskedEvent.class), any());

        try (MockedConstruction<OverwriteQuestion> ignored = mockConstruction(OverwriteQuestion.class,
            (mock, context) -> {
                when(mock.isOverwrite()).thenReturn(false);
            })) {
            this.moveJob.initialize(request);
            EntityJobStatus<MoveRequest> status = this.moveJob.getStatus();

            run(request);
        }
        verifyNoMove();
        assertEquals("Skipping [wiki:One.Page] because [wiki:Two.Page] already exists and the user doesn't want to "
            + "overwrite it.", getLogCapture().getMessage(0));
        assertEquals(Level.WARN, getLogCapture().getLogEvent(0).getLevel());
    }
}
