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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.internal.job.AbstractEntityJob.Visitor;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 */
public class MoveJobTest extends AbstractMoveJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(MoveJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    @Test
    public void moveInsideItsOwnHierarchy() throws Throwable
    {
        SpaceReference spaceReference =
            new SpaceReference("Entity", new SpaceReference("Model", new WikiReference("code")));

        run(createRequest(spaceReference.getParent(), spaceReference));
        verify(this.mocker.getMockedLogger()).error("Cannot make [{}] a descendant of itself.",
            spaceReference.getParent());

        verifyNoMove();
    }

    @Test
    public void moveToTheSameLocation() throws Throwable
    {
        SpaceReference spaceReference =
            new SpaceReference("Entity", new SpaceReference("Model", new WikiReference("code")));

        run(createRequest(spaceReference, spaceReference.getParent()));
        verify(this.mocker.getMockedLogger()).error("Cannot move [{}] into [{}], it's already there.", spaceReference,
            spaceReference.getParent());

        verifyNoMove();
    }

    @Test
    public void moveUnsupportedEntity() throws Throwable
    {
        run(createRequest(new WikiReference("from"), new WikiReference("to")));
        verify(this.mocker.getMockedLogger()).error("Unsupported source entity type [{}].", EntityType.WIKI);

        verifyNoMove();
    }

    @Test
    public void moveToUnsupportedDestination() throws Throwable
    {
        run(createRequest(new DocumentReference("wiki", "Space", "Page"), new WikiReference("test")));
        verify(this.mocker.getMockedLogger()).error("Unsupported destination entity type [{}] for a document.",
            EntityType.WIKI);

        run(createRequest(new DocumentReference("wiki", "Space", "Page"), new DocumentReference("test", "A", "B")));
        verify(this.mocker.getMockedLogger()).error("Unsupported destination entity type [{}] for a document.",
            EntityType.DOCUMENT);

        run(createRequest(new SpaceReference("Space", new WikiReference("wiki")), new DocumentReference("test", "A",
            "B")));
        verify(this.mocker.getMockedLogger()).error("Unsupported destination entity type [{}] for a space.",
            EntityType.DOCUMENT);

        verifyNoMove();
    }

    @Test
    public void moveMissingDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("foo", "A", "Page");
        run(createRequest(sourceReference, new SpaceReference("B", new WikiReference("bar"))));
        verify(this.mocker.getMockedLogger()).warn("Skipping [{}] because it doesn't exist.", sourceReference);

        verifyNoMove();
    }

    @Test
    public void moveDocumentWithoutDeleteRight() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(false);

        MoveRequest request = createRequest(documentReference, new SpaceReference("Foo", new WikiReference("bar")));
        request.setCheckRights(true);
        request.setUserReference(userReference);
        run(request);

        verify(this.mocker.getMockedLogger()).error("You are not allowed to delete [{}].", documentReference);

        verifyNoMove();
    }

    @Test
    public void moveDocumentToRestrictedDestination() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(false);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(userReference);
        run(request);

        verify(this.mocker.getMockedLogger()).error(
            "You don't have sufficient permissions over the destination document [{}].", newReference);

        verifyNoMove();
    }

    @Test
    public void moveDocumentToSpace() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference backLinkReference = new DocumentReference("wiki", "Three", "BackLink");
        when(this.modelBridge.getBackLinkedReferences(oldReference, "wiki"))
            .thenReturn(Arrays.asList(backLinkReference));

        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.modelBridge.exists(newReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        when(this.modelBridge.delete(newReference)).thenReturn(true);
        when(this.modelBridge.copy(oldReference, newReference)).thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(false);
        request.setInteractive(false);
        request.setUserReference(userReference);
        run(request);

        LinkRefactoring linkRefactoring = getMocker().getInstance(LinkRefactoring.class);
        verify(linkRefactoring).renameLinks(backLinkReference, oldReference, newReference);
        verify(linkRefactoring).updateRelativeLinks(oldReference, newReference);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).delete(oldReference);
        verify(this.modelBridge).createRedirect(oldReference, newReference);
        verify(this.modelBridge).updateParentField(oldReference, newReference);
    }

    @Test
    public void updateLinksOnFarm() throws Throwable
    {
        DocumentReference oldReference = new DocumentReference("foo", "One", "Page");
        when(this.modelBridge.exists(oldReference)).thenReturn(true);

        DocumentReference newReference = new DocumentReference("foo", "Two", "Page");
        when(this.modelBridge.exists(newReference)).thenReturn(false);

        when(this.modelBridge.copy(oldReference, newReference)).thenReturn(true);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));

        DocumentReference aliceReference = new DocumentReference("foo", "Alice", "BackLink");
        when(this.modelBridge.getBackLinkedReferences(oldReference, "foo")).thenReturn(Arrays.asList(aliceReference));

        DocumentReference bobReference = new DocumentReference("bar", "Bob", "BackLink");
        when(this.modelBridge.getBackLinkedReferences(oldReference, "bar")).thenReturn(Arrays.asList(bobReference));

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(false);
        request.setInteractive(false);
        request.setUpdateLinksOnFarm(true);

        GroupedJob job = (GroupedJob) run(request);
        assertEquals(RefactoringJobs.GROUP, job.getGroupPath().toString());

        LinkRefactoring linkRefactoring = getMocker().getInstance(LinkRefactoring.class);
        verify(linkRefactoring).renameLinks(aliceReference, oldReference, newReference);
        verify(linkRefactoring).renameLinks(bobReference, oldReference, newReference);
    }

    @Test
    public void moveDocumentToSpaceHome() throws Throwable
    {
        DocumentReference source = new DocumentReference("wiki", "A", "B");
        when(this.modelBridge.exists(source)).thenReturn(true);
        DocumentReference destination = new DocumentReference("wiki", "C", "WebHome");

        when(this.modelBridge.copy(source, new DocumentReference("wiki", "C", "B"))).thenReturn(true);

        MoveRequest request = createRequest(source, destination);
        request.setCheckRights(false);
        request.setAutoRedirect(false);
        request.setUpdateLinks(false);
        run(request);

        verify(this.modelBridge).delete(source);
        verify(this.modelBridge, never()).createRedirect(any(DocumentReference.class), any(DocumentReference.class));

        LinkRefactoring linkRefactoring = getMocker().getInstance(LinkRefactoring.class);
        verify(linkRefactoring, never()).renameLinks(any(DocumentReference.class), any(DocumentReference.class),
            any(DocumentReference.class));
        verify(linkRefactoring, never()).updateRelativeLinks(any(DocumentReference.class),
            any(DocumentReference.class));
    }

    @Test
    public void moveSpaceHomeDeep() throws Throwable
    {
        DocumentReference spaceHome = new DocumentReference("chess", Arrays.asList("A", "B", "C"), "WebHome");
        DocumentReference docFromSpace = new DocumentReference("X", spaceHome.getLastSpaceReference());
        when(this.modelBridge.getDocumentReferences(spaceHome.getLastSpaceReference()))
            .thenReturn(Arrays.asList(docFromSpace));
        when(this.modelBridge.exists(docFromSpace)).thenReturn(true);

        WikiReference newWiki = new WikiReference("tennis");

        MoveRequest request = createRequest(spaceHome, newWiki);
        request.setCheckRights(false);
        request.setDeep(true);
        run(request);

        verify(this.modelBridge).copy(docFromSpace, new DocumentReference("tennis", "C", "X"));

        ObservationManager observationManager = this.mocker.getInstance(ObservationManager.class);
        verify(observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class),
            eq(Collections.singletonMap(docFromSpace, new EntitySelection(docFromSpace))));
    }

    @Test
    public void moveSpaceToSpaceHome() throws Throwable
    {
        SpaceReference sourceSpace = new SpaceReference("wiki", "A", "B");
        DocumentReference sourceDoc = new DocumentReference("X", sourceSpace);
        when(this.modelBridge.getDocumentReferences(sourceSpace)).thenReturn(Arrays.asList(sourceDoc));
        when(this.modelBridge.exists(sourceDoc)).thenReturn(true);

        DocumentReference destination = new DocumentReference("wiki", "C", "WebHome");

        MoveRequest request = createRequest(sourceSpace, destination);
        request.setCheckRights(false);
        run(request);

        verify(this.modelBridge).copy(sourceDoc, new DocumentReference("wiki", Arrays.asList("C", "B"), "X"));
    }

    @Test
    public void copyDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        DocumentReference copyReference = new DocumentReference("wiki", "Copy", "Page");
        when(this.modelBridge.copy(sourceReference, copyReference)).thenReturn(true);

        MoveRequest request = createRequest(sourceReference, copyReference.getParent());
        request.setCheckRights(false);
        request.setInteractive(false);
        request.setDeleteSource(false);
        Map<String, String> parameters = Collections.singletonMap("foo", "bar");
        request.setEntityParameters(sourceReference, parameters);
        run(request);

        verify(this.modelBridge).update(copyReference, parameters);

        LinkRefactoring linkRefactoring = getMocker().getInstance(LinkRefactoring.class);
        verify(linkRefactoring, never()).renameLinks(any(DocumentReference.class), any(DocumentReference.class),
            any(DocumentReference.class));
        verify(linkRefactoring).updateRelativeLinks(sourceReference, copyReference);

        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
        verify(this.modelBridge, never()).createRedirect(any(DocumentReference.class), any(DocumentReference.class));
    }

    @Test
    public void getGroupPath() throws Exception
    {
        DocumentReference alice = new DocumentReference("chess", Arrays.asList("A", "B"), "C");
        DocumentReference bob = new DocumentReference("chess", Arrays.asList("A", "B"), "D");
        DocumentReference carol = new DocumentReference("chess", Arrays.asList("A", "E"), "F");

        MoveRequest request = new MoveRequest();
        request.setEntityReferences(Arrays.asList(alice, bob));
        request.setDestination(carol);

        GroupedJob job = (GroupedJob) getMocker().getComponentUnderTest();
        job.initialize(request);

        assertEquals(new JobGroupPath(Arrays.asList("refactoring", "chess", "A")), job.getGroupPath());

        request.setUpdateLinksOnFarm(true);
        job.initialize(request);

        assertEquals(new JobGroupPath(Arrays.asList("refactoring")), job.getGroupPath());
    }

    @Test
    public void cancelMove() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Source");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);
        DocumentReference destinationReference = new DocumentReference("wiki", "Destination", "WebHome");
        MoveRequest request = createRequest(sourceReference, destinationReference);
        request.setCheckRights(false);

        ObservationManager observationManager = this.mocker.getInstance(ObservationManager.class);
        doAnswer((Answer<Void>) invocation -> {
            DocumentsDeletingEvent event = invocation.getArgument(0);
            event.cancel();
            return null;
        }).when(observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class), any(Map.class));

        run(request);

        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }

    @Test
    public void checkEntitySelection() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Source");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);
        DocumentReference destinationReference = new DocumentReference("wiki", "Destination", "WebHome");
        MoveRequest request = createRequest(sourceReference, destinationReference);
        request.setCheckRights(false);

        ObservationManager observationManager = this.mocker.getInstance(ObservationManager.class);
        doAnswer((Answer<Void>) invocation -> {
            Map<EntityReference, EntitySelection> concernedEntities =
                (Map<EntityReference, EntitySelection>) invocation.getArgument(2);
            concernedEntities.get(sourceReference).setSelected(false);
            return null;
        }).when(observationManager).notify(any(DocumentsDeletingEvent.class), any(MoveJob.class), any(Map.class));

        run(request);

        verify(this.modelBridge, never()).copy(eq(sourceReference), any(DocumentReference.class));
    }

    @Test
    public void visitDocuments() throws Exception
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
            .thenReturn(Arrays.asList(one, two, three, four, five, six));

        MoveJob job = ((MoveJob) getMocker().getComponentUnderTest());
        job.initialize(createRequest(sourceReference, destinationReference));

        List<DocumentReference> visitedPages = new ArrayList<>();
        job.visitDocuments(sourceReference, new Visitor<DocumentReference>()
        {
            @Override
            public void visit(DocumentReference node)
            {
                visitedPages.add(node);
            }
        });

        assertEquals(Arrays.asList(five, six, two, one, three, four), visitedPages);
    }
}
