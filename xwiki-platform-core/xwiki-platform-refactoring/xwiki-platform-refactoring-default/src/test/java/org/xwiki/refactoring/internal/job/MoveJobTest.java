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
import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.MoveRequest;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 */
public class MoveJobTest extends AbstractOldCoreEntityJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(MoveJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    @Test
    public void moveInsideItsOwnHierarchy() throws Exception
    {
        SpaceReference spaceReference =
            new SpaceReference("Entity", new SpaceReference("Model", new WikiReference("code")));

        run(createRequest(spaceReference.getParent(), spaceReference));
        verify(this.mocker.getMockedLogger()).error("Cannot make [{}] a descendant of itself.",
            spaceReference.getParent());

        verifyNoMove();
    }

    @Test
    public void moveUnsupportedEntity() throws Exception
    {
        run(createRequest(new WikiReference("from"), new WikiReference("to")));
        verify(this.mocker.getMockedLogger()).error("Unsupported source entity type [{}].", EntityType.WIKI);

        verifyNoMove();
    }

    @Test
    public void moveToUnsupportedDestination() throws Exception
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
    public void moveMissingDocument() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("foo", "A", "Page");
        run(createRequest(sourceReference, new SpaceReference("B", new WikiReference("bar"))));
        verify(this.mocker.getMockedLogger()).warn("Skipping [{}] because it doesn't exist.", sourceReference);

        verifyNoMove();
    }

    @Test
    public void moveDocumentWithoutDeleteRight() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.xcontext.getWiki().exists(documentReference, xcontext)).thenReturn(true);

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
    public void moveDocumentToRestrictedDestination() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.xcontext.getWiki().exists(oldReference, xcontext)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, oldReference)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, userReference, newReference)).thenReturn(false);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(true);
        request.setUserReference(userReference);
        run(request);

        verify(this.mocker.getMockedLogger()).error(
            "You don't have sufficient permissions over the destination document [{}].", newReference);

        verifyNoMove();
    }

    @Test
    public void moveDocumentToSpace() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "One", "Page");
        when(this.xcontext.getWiki().exists(oldReference, this.xcontext)).thenReturn(true);

        XWikiDocument oldDocument = mock(XWikiDocument.class, "old");
        when(this.xcontext.getWiki().getDocument(oldReference, this.xcontext)).thenReturn(oldDocument);

        DocumentReference backLinkReference = new DocumentReference("wiki", "Three", "BackLink");
        when(oldDocument.getBackLinkedReferences(this.xcontext)).thenReturn(Arrays.asList(backLinkReference));

        XWikiDocument backLinkDocument = mock(XWikiDocument.class, "backLink");
        when(this.xcontext.getWiki().getDocument(backLinkReference, this.xcontext)).thenReturn(backLinkDocument);
        when(backLinkDocument.getTranslationLocales(this.xcontext)).thenReturn(Arrays.asList(Locale.FRENCH));
        when(backLinkDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        XWikiDocument backLinkDocumentFR = mock(XWikiDocument.class, "backLinkFR");
        when(backLinkDocument.getTranslatedDocument(Locale.FRENCH, this.xcontext)).thenReturn(backLinkDocumentFR);
        when(backLinkDocumentFR.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        DocumentReference newReference = new DocumentReference("wiki", "Two", "Page");
        when(this.xcontext.getWiki().exists(newReference, this.xcontext)).thenReturn(true);

        XWikiDocument newDocument = mock(XWikiDocument.class, "new");
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.xcontext.getUserReference()).thenReturn(bobReference);

        when(this.xcontext.getWiki().copyDocument(oldReference, newReference, null, false, true, false, this.xcontext))
            .thenReturn(true);

        MoveRequest request = createRequest(oldReference, newReference.getParent());
        request.setCheckRights(false);
        request.setInteractive(false);
        request.setUserReference(aliceReference);
        run(request);

        verify(this.xcontext, times(3)).setUserReference(aliceReference);
        verify(this.xcontext.getWiki()).deleteAllDocuments(newDocument, this.xcontext);
        verify(this.mocker.getMockedLogger())
            .info("Document [{}] has been copied to [{}].", oldReference, newReference);
        verify(this.xcontext.getWiki()).deleteAllDocuments(oldDocument, this.xcontext);
        verify(this.xcontext, times(3)).setUserReference(bobReference);
    }

    private MoveRequest createRequest(EntityReference source, EntityReference destination)
    {
        MoveRequest request = new MoveRequest();
        request.setEntityReferences(Arrays.asList(source));
        request.setDestination(destination);
        return request;
    }

    private void verifyNoMove() throws Exception
    {
        verify(this.xcontext.getWiki(), never()).deleteAllDocuments(any(XWikiDocument.class), eq(this.xcontext));
        verify(this.xcontext.getWiki(), never()).copyDocument(any(DocumentReference.class),
            any(DocumentReference.class), anyString(), anyBoolean(), anyBoolean(), anyBoolean(), eq(this.xcontext));
    }
}
