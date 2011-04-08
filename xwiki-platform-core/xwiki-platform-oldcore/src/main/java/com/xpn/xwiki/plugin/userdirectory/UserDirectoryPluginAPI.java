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
package com.xpn.xwiki.plugin.userdirectory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

import java.util.List;

public class UserDirectoryPluginAPI extends Api {
    UserDirectoryPlugin userDir;

    public UserDirectoryPluginAPI(UserDirectoryPlugin userDirectory, XWikiContext context) {
        super(context);
        this.userDir = userDirectory;
    }

    public Group addGroup(XWikiContext context) throws XWikiException {
        return userDir.addGroup(context);
    }

    public void updateGroup(XWikiContext context) throws XWikiException {
        userDir.updateGroup(context);
    }

    public boolean groupExist(String name, XWikiContext context) throws XWikiException {
        return userDir.groupExist(name, context);
    }

    public Group getGroup(String space, String name, XWikiContext context) throws XWikiException {
        return userDir.getGroup(space, name, context);
    }

    public Group getGroup(String name, XWikiContext context) throws XWikiException {
        return userDir.getGroup(name, context);
    }

    public List getAllGroupsPageName(XWikiContext context) throws XWikiException {
        return userDir.getAllGroupsPageName(context);
    }

    public List getAllGroups(XWikiContext context) throws XWikiException {
        return userDir.getAllGroups(context);
    }

    public List getAllGroups(String orderBy, XWikiContext context) throws XWikiException {
        return userDir.getAllGroups(orderBy, context);
    }

    public List getMembers(String grpPage, XWikiContext context) throws XWikiException {
        return userDir.getMembers(grpPage, context);
    }

    public List getUnactivatedMembers(String grpPage, XWikiContext context) throws XWikiException {
        return userDir.getUnactivatedMembers(grpPage, context);
    }

    public boolean addParentGroup(String childGroupName, String parentGroupName, XWikiContext context) throws XWikiException {
        return userDir.addParentGroup(childGroupName, parentGroupName, context);
    }

    public boolean removeParentGroup(String childGroupName, String parentGroupName, XWikiContext context) throws XWikiException {
        return userDir.removeParentGroup(childGroupName, parentGroupName, context);
    }

    public List getParentGroups(String grpName, XWikiContext context) throws XWikiException {
        return userDir.getParentGroups(grpName, context);
    }

    public String inviteToGroup(String name, String firstName, String email, String group, XWikiContext context) throws XWikiException {
        return userDir.inviteToGroup(name, firstName, email, group,  context);
    }

    public void addUserToGroup(String userPage, String group, XWikiContext context) throws XWikiException {
        userDir.addUserToGroup(userPage, group,  context);
    }

    public String getUserName(String userPage, XWikiContext context) throws XWikiException {
        return userDir.getUserName(userPage, context);
    }

    public String getUserEmail(String userPage, XWikiContext context) throws XWikiException {
        return userDir.getUserEmail(userPage, context);
    }

    public List getUsersDocumentName(String grpPage, XWikiContext context) throws XWikiException {
        return userDir.getUsersDocumentName(grpPage, context);
    }

    public List getUsersDocument(String grpPage, XWikiContext context) throws XWikiException {
        return userDir.getUsersDocument(grpPage, context);
    }


    public boolean removeMemberships(String userPage, String grpPage, XWikiContext context) throws XWikiException {
        return userDir.removeMemberships(userPage, grpPage, context);
    }

    public void sendDeactivationEMail(String userPage, String grpPage, XWikiContext context) throws XWikiException {
        userDir.sendDeactivationEMail(userPage, grpPage, context);
    }

    public List getUserMemberships(String userPage, XWikiContext context) throws XWikiException {
        return userDir.getUserMemberships(userPage, context);
    }

    public boolean deactivateAccount(String userPage, XWikiContext context) throws XWikiException {
        return userDir.deactivateAccount(userPage, context);
    }

    public void resendInvitation(String userPage, XWikiContext context) throws XWikiException {
        userDir.resendInvitation(userPage, context);
    }

}
