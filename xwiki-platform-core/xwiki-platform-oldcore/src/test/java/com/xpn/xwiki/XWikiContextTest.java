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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Unit tests for {@link com.xpn.xwiki.XWikiContext}.
 * 
 * @version $Id$
 */
public class XWikiContextTest extends AbstractBridgedComponentTestCase
{
    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        getContext().setWiki(new XWiki()
        {
            @Override
            public boolean isVirtualMode()
            {
                return true;
            }
        });
    }

    @Test
    public void testgetUser()
    {
        getContext().setMainXWiki("wiki");
        getContext().setDatabase("wiki");
        getContext().setUserReference(new DocumentReference("wiki", "space", "user"));

        Assert.assertEquals("space.user", getContext().getUser());
        Assert.assertEquals("space.user", getContext().getLocalUser());
        Assert.assertEquals("space.user", getContext().getXWikiUser().getUser());
        Assert.assertTrue("space.user", getContext().getXWikiUser().isMain());

        getContext().setDatabase("wiki1");

        Assert.assertEquals("wiki:space.user", getContext().getUser());
        Assert.assertEquals("space.user", getContext().getLocalUser());
        Assert.assertEquals("wiki:space.user", getContext().getXWikiUser().getUser());
        Assert.assertTrue("space.user", getContext().getXWikiUser().isMain());
    }

    @Test
    public void testSetUser()
    {
        getContext().setMainXWiki("wiki");
        getContext().setDatabase("wiki");
        getContext().setUser("XWiki.user");

        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "user"), getContext().getUserReference());
        Assert.assertEquals("XWiki.user", getContext().getUser());

        getContext().setUser("user");

        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "user"), getContext().getUserReference());
        Assert.assertEquals("XWiki.user", getContext().getUser());
    }

    @Test
    public void testAnonymousUser()
    {
        getContext().setMainXWiki("wiki");
        getContext().setDatabase("wiki");
        getContext().setUserReference(null);

        Assert.assertNull(getContext().getUserReference());
        Assert.assertEquals("XWiki.XWikiGuest", getContext().getUser());
        Assert.assertEquals("XWiki.XWikiGuest", getContext().getLocalUser());
        Assert.assertNull(getContext().getXWikiUser());

        getContext().setDatabase("wiki2");

        Assert.assertNull(getContext().getUserReference());
        Assert.assertEquals("XWiki.XWikiGuest", getContext().getUser());
        Assert.assertEquals("XWiki.XWikiGuest", getContext().getLocalUser());
        Assert.assertNull(getContext().getXWikiUser());
        
        getContext().setUser(XWikiRightService.GUEST_USER_FULLNAME);
        
        Assert.assertEquals(new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME), getContext().getXWikiUser());
        Assert.assertNull(getContext().getUserReference());
        
        getContext().setUser(XWikiRightService.GUEST_USER);
        
        Assert.assertEquals(new XWikiUser(XWikiRightService.GUEST_USER), getContext().getXWikiUser());
        Assert.assertNull(getContext().getUserReference());
    }
}
