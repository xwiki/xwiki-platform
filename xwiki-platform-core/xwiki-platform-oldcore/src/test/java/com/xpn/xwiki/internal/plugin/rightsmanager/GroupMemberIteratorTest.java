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
package com.xpn.xwiki.internal.plugin.rightsmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GroupMemberIterator}.
 *
 * @version $Id$
 * @since 6.4.2, 7.0M2
 */
public class GroupMemberIteratorTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private Execution execution;

    private DocumentReferenceResolver<String> resolver;

    @Test
    public void getMembersWhenNoExecutionContext() throws Exception
    {
        Execution execution = this.componentManager.registerMockComponent(Execution.class);
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "userpage");
        try {
            new GroupMemberIterator(userReference, null, execution).next();
        } catch (RuntimeException expected) {
            assertEquals("Aborting member extraction from [userwiki:XWiki.userpage] since no XWiki Context was found",
                expected.getMessage());
        }
    }

    @Test
    public void getMembersWhenNoXWikiContext() throws Exception
    {
        Execution execution = this.componentManager.registerMockComponent(Execution.class);
        when(execution.getContext()).thenReturn(new ExecutionContext());
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "userpage");
        try {
            new GroupMemberIterator(userReference, null, execution).next();
        } catch (RuntimeException expected) {
            assertEquals("Aborting member extraction from [userwiki:XWiki.userpage] since no XWiki Context was found",
                expected.getMessage());
        }
    }

    @Test
    public void getMembersWhenReferenceIsNull() throws Exception
    {
        setUpBaseMocks();

        Iterator<DocumentReference> iterator = new GroupMemberIterator(null, null, this.execution);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSuperadmin() throws Exception
    {
        setUpBaseMocks();
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "superadmin");

        Iterator<DocumentReference> iterator = new GroupMemberIterator(userReference, null, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(userReference, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenGuest() throws Exception
    {
        setUpBaseMocks();
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "XWikiGuest");

        Iterator<DocumentReference> iterator = new GroupMemberIterator(userReference, null, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(userReference, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSingleUser() throws Exception
    {
        Iterator<DocumentReference> iterator = setUpSingleUser();

        assertTrue(iterator.hasNext());
        assertEquals(new DocumentReference("userwiki", "XWiki", "userpage"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSingleUserWithoutCallingHasNextFirst() throws Exception
    {
        Iterator<DocumentReference> iterator = setUpSingleUser();

        assertEquals(new DocumentReference("userwiki", "XWiki", "userpage"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenCallingNextWithNoMoreReference() throws Exception
    {
        Iterator<DocumentReference> iterator = setUpSingleUser();

        assertEquals(new DocumentReference("userwiki", "XWiki", "userpage"), iterator.next());
        try {
            iterator.next();
        } catch (NoSuchElementException expected) {
            assertEquals("No more users to extract from the passed [userwiki:XWiki.userpage] reference",
                expected.getMessage());
        }
    }

    public Iterator<DocumentReference> setUpSingleUser() throws Exception
    {
        setUpBaseMocks();
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "userpage");
        setUpUserPageMocks(userReference);

        return new GroupMemberIterator(userReference, null, this.execution);
    }

    @Test
    public void getMembersWhenSingleReferenceButDocumentDoesntExist() throws Exception
    {
        setUpBaseMocks();
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "invalid");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(true);
        when(this.xwiki.getDocument(userReference, this.xwikiContext)).thenReturn(document);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(userReference, null, this.execution);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSingleUserButBothUserAndGroupReference() throws Exception
    {
        setUpBaseMocks();
        DocumentReference reference = new DocumentReference("wiki", "XWiki", "page");

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        when(document.getDocumentReference()).thenReturn(reference);
        when(this.xwiki.getDocument(reference, this.xwikiContext)).thenReturn(document);

        // It's a user reference
        BaseObject bo1 = mock(BaseObject.class);
        when(document.getXObject(new DocumentReference("wiki", "XWiki", "XWikiUsers"))).thenReturn(bo1);

        // It's also a group reference (with one user in it)
        List<BaseObject> memberObjects = new ArrayList<>();
        BaseObject bo2 = mock(BaseObject.class);
        when(bo2.getStringValue("member")).thenReturn("XWiki.user");
        memberObjects.add(bo2);
        when(document.getXObjects(new DocumentReference("wiki", "XWiki", "XWikiGroups"))).thenReturn(
            memberObjects);
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        when(this.resolver.resolve("XWiki.user", reference)).thenReturn(userReference);
        setUpUserPageMocks(userReference);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(reference, this.resolver, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(new DocumentReference("wiki", "XWiki", "page"), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(new DocumentReference("wiki", "XWiki", "user"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSingleUserButDocumentFailsToLoad() throws Exception
    {
        setUpBaseMocks();
        DocumentReference userReference = new DocumentReference("userwiki", "XWiki", "userpage");
        when(xwiki.getDocument(userReference, this.xwikiContext)).thenThrow(new XWikiException());

        Iterator<DocumentReference> iterator = new GroupMemberIterator(userReference, null, this.execution);

        try {
            iterator.next();
        } catch (RuntimeException expected) {
            assertEquals("Failed to get document for User or Group [userwiki:XWiki.userpage] when extracting all "
                + "users for reference [userwiki:XWiki.userpage]", expected.getMessage());
        }
    }

    @Test
    public void getMembersWhenGroupWithTwoUsers() throws Exception
    {
        setUpBaseMocks();
        DocumentReference groupReference = new DocumentReference("groupwiki", "XWiki", "grouppage");
        DocumentReference userReference1 = new DocumentReference("userwiki", "XWiki", "user1");
        DocumentReference userReference2 = new DocumentReference("userwiki", "XWiki", "user2");
        setUpGroupPageMocks(groupReference, userReference1, userReference2);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(groupReference, resolver, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(userReference1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(userReference2, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenGroupWithOneUser() throws Exception
    {
        setUpBaseMocks();
        DocumentReference groupReference = new DocumentReference("groupwiki", "XWiki", "grouppage");
        DocumentReference userReference1 = new DocumentReference("userwiki", "XWiki", "user1");
        setUpGroupPageMocks(groupReference, userReference1);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(groupReference, resolver, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(userReference1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenSingleReferenceNotAUser() throws Exception
    {
        setUpBaseMocks();
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        DocumentReference reference = new DocumentReference("somewiki", "XWiki", "somepage");
        when(document.getDocumentReference()).thenReturn(reference);
        when(document.getXObject(new DocumentReference("somewiki", "XWiki", "XWikiUsers"))).thenReturn(null);
        when(this.xwiki.getDocument(reference, this.xwikiContext)).thenReturn(document);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(reference, null, this.execution);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void getMembersWhenNestedGroup() throws Exception
    {
        setUpBaseMocks();
        DocumentReference groupReference1 = new DocumentReference("groupwiki", "XWiki", "grouppage1");
        DocumentReference userReference1 = new DocumentReference("userwiki", "XWiki", "user1");
        DocumentReference userReference2 = new DocumentReference("userwiki", "XWiki", "user2");
        DocumentReference groupReference2 = new DocumentReference("groupwiki", "XWiki", "grouppage2");
        DocumentReference userReference3 = new DocumentReference("userwiki", "XWiki", "user3");
        DocumentReference userReference4 = new DocumentReference("userwiki", "XWiki", "user4");
        setUpGroupPageMocks(groupReference1, userReference1, userReference2, groupReference2);
        setUpGroupPageMocks(groupReference2, userReference3, userReference4);

        Iterator<DocumentReference> iterator = new GroupMemberIterator(groupReference1, this.resolver, this.execution);

        assertTrue(iterator.hasNext());
        assertEquals(userReference1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(userReference2, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(userReference3, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(userReference4, iterator.next());
        assertFalse(iterator.hasNext());
    }

    private void setUpGroupPageMocks(DocumentReference... references)
        throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        when(document.getDocumentReference()).thenReturn(references[0]);
        when(xwiki.getDocument(references[0], this.xwikiContext)).thenReturn(document);

        List<BaseObject> memberObjects = new ArrayList<>();
        for (int i = 1; i < references.length; i++) {
            BaseObject bo = mock(BaseObject.class);
            when(bo.getStringValue("member")).thenReturn("XWiki." + references[i].getName());
            memberObjects.add(bo);
        }
        when(document.getXObjects(new DocumentReference("groupwiki", "XWiki", "XWikiGroups"))).thenReturn(
            memberObjects);

        for (int i = 1; i < references.length; i++) {
            setUpUserPageMocks(references[i]);
            when(this.resolver.resolve("XWiki." + references[i].getName(), references[0])).thenReturn(references[i]);
        }
    }

    private void setUpUserPageMocks(DocumentReference userReference)
        throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.isNew()).thenReturn(false);
        when(document.getDocumentReference()).thenReturn(userReference);
        BaseObject baseObject = mock(BaseObject.class);
        when(document.getXObject(new DocumentReference(
            userReference.getWikiReference().getName(), "XWiki", "XWikiUsers"))).thenReturn(baseObject);
        when(this.xwiki.getDocument(userReference, this.xwikiContext)).thenReturn(document);
    }

    private void setUpBaseMocks() throws Exception
    {
        this.resolver = this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        this.execution = this.componentManager.registerMockComponent(Execution.class);
        this.xwikiContext = mock(XWikiContext.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this.xwikiContext);
        when(this.execution.getContext()).thenReturn(executionContext);
        this.xwiki = mock(XWiki.class);
        when(this.xwikiContext.getWiki()).thenReturn(this.xwiki);
    }
}
