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
package com.xpn.xwiki.api;

import org.jmock.Mock;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Unit tests for {@link com.xpn.xwiki.api.User}.
 * 
 * @version $Id$
 */
public class UserTest extends AbstractBridgedXWikiComponentTestCase
{
    /**
     * Checks that XWIKI-2040 remains fixed.
     */
    public void testIsUserInGroupDoesNotThrowNPE()
    {
        User u = new User(null, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiUser xu = new XWikiUser((String)null);
        u = new User(xu, null);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

        XWikiContext c = new XWikiContext();
        u = new User(xu, c);
        assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));
    }

    public void testGetEmail() throws Exception
    {
        Mock mockXWiki = mock(XWiki.class);
        getContext().setWiki((XWiki) mockXWiki.proxy());
        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Admin"));
        BaseClass userClass = new BaseClass();
        userClass.addTextField("email", "email address", 20);
        mockXWiki.stubs().method("getXClass").will(returnValue(userClass));
        BaseObject userObj = doc.newXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"), getContext());
        userObj.setStringValue("email", "admin@mail.com");
        mockXWiki.stubs().method("getDocument").will(returnValue(doc));

        User u = new User(null, null);
        assertNull(u.getEmail());

        XWikiUser xu = new XWikiUser("XWiki.Admin");
        u = new User(xu, getContext());
        assertEquals("admin@mail.com", u.getEmail());
    }
}
