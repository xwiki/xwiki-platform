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
package com.xpn.xwiki.user.impl.xwiki;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OldcoreTest
@ReferenceComponentList
public class XWikiGroupServiceImplTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    XWikiGroupServiceImpl groupService;

    private XWikiDocument user;

    private XWikiDocument userWithSpaces;

    private XWikiDocument group;

    private BaseObject groupObject;

    @BeforeEach
    public void before() throws Exception
    {
        this.groupService = new XWikiGroupServiceImpl();

        doReturn(0).when(this.oldcore.getSpyXWiki()).getMaxRecursiveSpaceChecks(any(XWikiContext.class));

        this.user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
        this.user.newXObject(new DocumentReference("wiki", "XWiki", "XWikiUser"), this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(this.user, this.oldcore.getXWikiContext());

        this.userWithSpaces = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user with spaces"));
        this.userWithSpaces.newXObject(new DocumentReference("wiki", "XWiki", "XWikiUser"),
            this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(this.userWithSpaces, this.oldcore.getXWikiContext());

        this.group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
        this.groupObject =
            this.group
                .newXObject(new DocumentReference("wiki", "XWiki", "XWikiGroups"), this.oldcore.getXWikiContext());
        this.groupObject.setStringValue("member", this.user.getFullName());
        this.oldcore.getSpyXWiki().saveDocument(this.group, this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().setWikiId("wiki");
    }

    @Test
    public void testListMemberForGroup() throws XWikiException
    {
        assertEquals(
            new HashSet<String>(Arrays.asList(this.user.getFullName())),
            new HashSet<String>(this.groupService.listMemberForGroup(this.group.getFullName(),
                this.oldcore.getXWikiContext())));

        this.groupObject.setStringValue("member", this.userWithSpaces.getFullName());
        this.oldcore.getSpyXWiki().saveDocument(this.group, this.oldcore.getXWikiContext());

        assertEquals(new HashSet<String>(Arrays.asList(this.userWithSpaces.getFullName())), new HashSet<String>(
            this.groupService.listMemberForGroup(this.group.getFullName(), this.oldcore.getXWikiContext())));
    }

    @Test
    public void getAllMatchedMembersNamesForGroup() throws Exception
    {
        Query query = mock(Query.class);
        when(this.oldcore.getQueryManager().createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        List<Object> members = Arrays.asList("one", "two", "three");
        when(query.execute()).thenReturn(members);

        assertEquals(members, this.groupService.getAllMatchedMembersNamesForGroup(this.group.getFullName(), "foo", 3,
            10, false, this.oldcore.getXWikiContext()));

        verify(query).setWiki("wiki");
        verify(query).setOffset(10);
        verify(query).setLimit(3);
        verify(query).bindValue("groupdocname", this.group.getFullName());
        verify(query).bindValue("groupclassname", "XWiki.XWikiGroups");
        verify(query).bindValue("matchfield", "%foo%");
    }

    @Test
    public void countAllMatchedMembersNamesForGroup() throws Exception
    {
        Query query = mock(Query.class);
        when(this.oldcore.getQueryManager().createQuery(anyString(), eq(Query.HQL))).thenReturn(query);
        when(query.execute()).thenReturn(Collections.singletonList(5L));

        assertEquals(5, this.groupService.countAllMatchedMembersNamesForGroup(this.group.getFullName(), "foo",
            this.oldcore.getXWikiContext()));

        verify(query).setWiki("wiki");
        verify(query).bindValue("groupdocname", this.group.getFullName());
        verify(query).bindValue("groupclassname", "XWiki.XWikiGroups");
        verify(query).bindValue("matchfield", "%foo%");
    }
}
