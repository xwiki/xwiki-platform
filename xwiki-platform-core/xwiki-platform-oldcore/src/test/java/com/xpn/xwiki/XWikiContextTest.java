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
package com.xpn.xwiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link com.xpn.xwiki.XWikiContext}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class XWikiContextTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiContext xcontext;

    @BeforeEach
    public void beforeEach()
    {
        this.xcontext = this.oldcore.getXWikiContext();
    }

    @Test
    public void testgetUser()
    {
        this.xcontext.setMainXWiki("wiki");
        this.xcontext.setWikiId("wiki");
        this.xcontext.setUserReference(new DocumentReference("wiki", "space", "user"));

        assertEquals("space.user", this.xcontext.getUser());
        assertEquals("space.user", this.xcontext.getLocalUser());
        assertEquals("space.user", this.xcontext.getXWikiUser().getUser());
        assertTrue(this.xcontext.getXWikiUser().isMain(), "space.user");

        this.xcontext.setWikiId("wiki1");

        assertEquals("wiki:space.user", this.xcontext.getUser());
        assertEquals("space.user", this.xcontext.getLocalUser());
        assertEquals("wiki:space.user", this.xcontext.getXWikiUser().getUser());
        assertTrue(this.xcontext.getXWikiUser().isMain(), "space.user");
    }

    @Test
    public void testSetUser()
    {
        this.xcontext.setMainXWiki("wiki");
        this.xcontext.setWikiId("wiki");
        this.xcontext.setUser("XWiki.user");

        assertEquals(new DocumentReference("wiki", "XWiki", "user"), this.xcontext.getUserReference());
        assertEquals("XWiki.user", this.xcontext.getUser());

        this.xcontext.setUser("user");

        assertEquals(new DocumentReference("wiki", "XWiki", "user"), this.xcontext.getUserReference());
        assertEquals("XWiki.user", this.xcontext.getUser());
    }

    @Test
    public void testAnonymousUser()
    {
        this.xcontext.setMainXWiki("wiki");
        this.xcontext.setWikiId("wiki");
        this.xcontext.setUserReference(null);

        assertNull(this.xcontext.getUserReference());
        assertEquals("XWiki.XWikiGuest", this.xcontext.getUser());
        assertEquals("XWiki.XWikiGuest", this.xcontext.getLocalUser());
        assertNull(this.xcontext.getXWikiUser());

        this.xcontext.setWikiId("wiki2");

        assertNull(this.xcontext.getUserReference());
        assertEquals("XWiki.XWikiGuest", this.xcontext.getUser());
        assertEquals("XWiki.XWikiGuest", this.xcontext.getLocalUser());
        assertNull(this.xcontext.getXWikiUser());

        this.xcontext.setUser(XWikiRightService.GUEST_USER_FULLNAME);

        assertEquals(new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME), this.xcontext.getXWikiUser());
        assertNull(this.xcontext.getUserReference());

        this.xcontext.setUser(XWikiRightService.GUEST_USER);

        assertEquals(new XWikiUser(XWikiRightService.GUEST_USER), this.xcontext.getXWikiUser());
        assertNull(this.xcontext.getUserReference());
    }
}
