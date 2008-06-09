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

package com.xpn.xwiki.user.api;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class XWikiUser
{
    private String user;

    private boolean main;

    public XWikiUser(String user)
    {
        this(user, false);
    }

    public XWikiUser(String user, boolean main)
    {
        setUser(user);
        setMain(main);
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    /**
     * Check if the user belongs to a group or not.
     * 
     * @param groupName The group to check.
     * @param context The current {@link XWikiContext context}.
     * @return <tt>true</tt> if the user does belong to the specified group, false otherwise or if
     *         an exception occurs.
     * @throws XWikiException If an error occurs when checking the groups.
     * @since Platform-1.3
     */
    public boolean isUserInGroup(String groupName, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isEmpty(getUser())) {
            XWikiGroupService groupService = context.getWiki().getGroupService(context);
            Collection groups = groupService.getAllGroupsNamesForMember(getUser(), 0, 0, context);
            return groups.contains(groupName);
        }
        return false;
    }

    public boolean isMain()
    {
        return main;
    }

    public void setMain(boolean main)
    {
        this.main = main;
    }
    
    public String toString()
    {
        return getUser();
    }
}
