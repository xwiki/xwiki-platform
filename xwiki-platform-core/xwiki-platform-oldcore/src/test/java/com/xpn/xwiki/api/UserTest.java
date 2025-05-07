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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.user.api.XWikiUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link com.xpn.xwiki.api.User}.
 * 
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class UserTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    /**
     * Checks that XWIKI-2040 remains fixed.
     */
    @Test
    void isUserInGroupDoesNotThrowNPE()
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

    @Test
    void getEmail() throws Exception
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Admin"));
        BaseClass userClass = new BaseClass();
        userClass.addTextField("email", "email address", 20);
        doc.setXClass(userClass);
        BaseObject userObj = doc.newXObject(new DocumentReference("xwiki", "XWiki", "XWikiUsers"),
            this.oldcore.getXWikiContext());
        userObj.setStringValue("email", "admin@mail.com");
        this.oldcore.getSpyXWiki().saveDocument(doc, this.oldcore.getXWikiContext());

        User u = new User(null, null);
        assertNull(u.getEmail());

        XWikiUser xu = new XWikiUser("XWiki.Admin");
        u = new User(xu, this.oldcore.getXWikiContext());
        assertEquals("admin@mail.com", u.getEmail());
    }
}
