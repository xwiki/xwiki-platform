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
 *
 */
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiUser;

import java.util.Collection;

public class User extends Api
{
    private XWikiUser user;

    public User(XWikiUser user, XWikiContext context)
    {
        super(context);
        this.user = user;
    }

    public XWikiUser getUser()
    {
        if (hasProgrammingRights()) {
            return user;
        }
        return null;
    }

    public boolean isUserInGroup(String groupName) throws XWikiException
    {
        XWikiGroupService groupService = getXWikiContext().getWiki().getGroupService(getXWikiContext());
        Collection groups = groupService.listGroupsForUser(user.getUser(), getXWikiContext());
        return groups.contains(groupName);
    }

    /**
     * API to retrieve the e-mail address of this user. This e-mail address is taken from the user
     * profile. If the user hasn't changed his profile, then this is the e-mail address he filled in
     * the registration form.
     * 
     * @return The e-mail address from the user profile, or <tt>null</tt> if there is an error
     *         retrieving the email.
     * @since 1.1.3
     * @since 1.2.2
     * @since 1.3M2
     */
    public String getEmail()
    {
        XWikiDocument userDoc;
        try {
            userDoc = getXWikiContext().getWiki().getDocument(user.getUser(), getXWikiContext());
            BaseObject obj = userDoc.getObject("XWiki.XWikiUsers");
            return obj.getStringValue("email");
        } catch (Exception e) {
            // APIs should never throw errors, as velocity cannot catch them, and scripts should be
            // as robust as possible. Instead, the code using this should know that null means there
            // was an error, if it really needs to report these exceptions.
            return null;
        }
    }
}
