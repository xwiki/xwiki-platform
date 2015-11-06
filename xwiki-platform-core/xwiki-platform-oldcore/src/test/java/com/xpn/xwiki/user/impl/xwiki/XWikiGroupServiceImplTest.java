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
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

@ReferenceComponentList
public class XWikiGroupServiceImplTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    XWikiGroupServiceImpl groupService;

    private XWikiDocument user;

    private XWikiDocument userWithSpaces;

    private XWikiDocument group;

    private BaseObject groupObject;

    @Before
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
}
