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
package org.xwiki.refactoring.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 */
public class DefaultModelBridgeTest
{
    @Rule
    public MockitoComponentMockingRule<ModelBridge> mocker = new MockitoComponentMockingRule<ModelBridge>(
        DefaultModelBridge.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    @Before
    public void configure() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    public void create() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);

        this.mocker.getComponentUnderTest().create(documentReference);

        verify(this.xcontext.getWiki()).saveDocument(document, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been created.", documentReference);
    }

    @Test
    public void copy() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        DocumentReference copyReference = new DocumentReference("wiki", "Space", "Copy");

        when(this.xcontext.getWiki().copyDocument(sourceReference, copyReference, "fr", false, true, true,
            this.xcontext)).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().copy(sourceReference, copyReference));

        verify(this.mocker.getMockedLogger()).info("Document [{}] has been copied to [{}].", sourceReference,
            copyReference);
    }

    @Test
    public void deleteTranslation() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(1);

        this.mocker.getComponentUnderTest().delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteDocument(sourceDocument, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been deleted.", sourceReference);
    }

    @Test
    public void deleteAllTranslations() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");

        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(0);

        this.mocker.getComponentUnderTest().delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteAllDocuments(sourceDocument, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been deleted with all its translations.",
            sourceReference);
    }

    @Test
    public void createRedirect() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newReference = new DocumentReference("wiki", "Space", "New");

        DocumentReference redirectClassReference = new DocumentReference("wiki", "XWiki", "RedirectClass");
        when(this.xcontext.getWiki().exists(redirectClassReference, this.xcontext)).thenReturn(true);

        XWikiDocument oldDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldReference, this.xcontext)).thenReturn(oldDocument);
        when(oldDocument.getXObject(eq(redirectClassReference), anyInt())).thenReturn(mock(BaseObject.class));

        this.mocker.getComponentUnderTest().createRedirect(oldReference, newReference);

        verify(oldDocument).setHidden(true);
        verify(this.xcontext.getWiki()).saveDocument(oldDocument, "Create automatic redirect.", this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Created automatic redirect from [{}] to [{}].", oldReference,
            newReference);
    }

    @Test
    public void getDocumentReferences() throws Exception
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Space");

        Query query = mock(Query.class);
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(query);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(spaceReference)).thenReturn("Space");

        when(query.execute()).thenReturn(Arrays.<Object>asList("Page"));

        DocumentReferenceResolver<String> explicitDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
        DocumentReference documentReference = new DocumentReference("Page", spaceReference);
        when(explicitDocumentReferenceResolver.resolve("Page", spaceReference)).thenReturn(documentReference);

        assertEquals(Arrays.asList(documentReference),
            this.mocker.getComponentUnderTest().getDocumentReferences(spaceReference));

        verify(query).setWiki(spaceReference.getWikiReference().getName());
        verify(query).bindValue("space", "Space");
        verify(query).bindValue("spacePrefix", "Space.%");
    }

    @Test
    public void updateParentFields() throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        DocumentReference child1Reference = new DocumentReference("wiki", "Space", "Child1");
        DocumentReference child2Reference = new DocumentReference("wiki", "Space", "Child2");
        when(oldParentDocument.getChildrenReferences(this.xcontext)).thenReturn(
            Arrays.asList(child1Reference, child2Reference));

        JobProgressManager mockProgressManager = mocker.getInstance(JobProgressManager.class);

        XWikiDocument child1Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child1Reference, this.xcontext)).thenReturn(child1Document);
        XWikiDocument child2Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child2Reference, this.xcontext)).thenReturn(child2Document);

        this.mocker.getComponentUnderTest().updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager).pushLevelProgress(2, this.mocker.getComponentUnderTest());

        verify(child1Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);

        verify(child2Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);
    }

    @Test
    public void updateParentFieldsNoChildren() throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        when(oldParentDocument.getChildrenReferences(this.xcontext)).thenReturn(
            Collections.<DocumentReference>emptyList());

        JobProgressManager mockProgressManager = mocker.getInstance(JobProgressManager.class);

        this.mocker.getComponentUnderTest().updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager, never()).pushLevelProgress(anyInt(), any());
        verify(this.xcontext.getWiki(), never()).saveDocument(any(XWikiDocument.class), eq("Updated parent field."),
            eq(true), eq(this.xcontext));
    }

    @Test
    public void updateTitle() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        this.mocker.getComponentUnderTest().update(documentReference, Collections.singletonMap("title", "foo"));

        verify(document).setTitle("foo");
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been updated.", documentReference);
    }
}
