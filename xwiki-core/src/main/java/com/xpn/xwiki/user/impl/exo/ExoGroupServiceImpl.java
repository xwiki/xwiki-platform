/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

package com.xpn.xwiki.user.impl.exo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;

public class ExoGroupServiceImpl extends XWikiGroupServiceImpl
{
    private static OrganizationService organizationService;

    @Override
    public void init(XWiki xwiki, XWikiContext context) throws XWikiException
    {
        super.init(xwiki, context);
    }

    protected GroupHandler getGroupHandler()
    {
        if (organizationService == null) {
            organizationService = getOrganizationService();
        }
        return organizationService.getGroupHandler();
    }

    public static OrganizationService getOrganizationService()
    {
        if (organizationService == null) {
            PortalContainer manager = PortalContainer.getInstance();
            organizationService = (OrganizationService) manager.getComponentInstanceOfType(OrganizationService.class);
        }
        return organizationService;
    }

    protected UserHandler getUserHandler()
    {
        if (organizationService == null) {
            organizationService = getOrganizationService();
        }
        return organizationService.getUserHandler();
    }

    protected MembershipHandler getMembershipHandler()
    {
        if (organizationService == null) {
            organizationService = getOrganizationService();
        }
        return organizationService.getMembershipHandler();
    }

    protected MembershipTypeHandler getMembershipTypeHandler()
    {
        if (organizationService == null) {
            organizationService = getOrganizationService();
        }
        return organizationService.getMembershipTypeHandler();
    }

    @Override
    public Collection<String> listGroupsForUser(String username, XWikiContext context) throws XWikiException
    {
        GroupHandler groupHandler = getGroupHandler();
        Collection<Group> groups = null;
        try {
            if (username.startsWith("XWiki.")) {
                username = username.substring(6);
                groups = groupHandler.findGroupsOfUser(username);
            }

            ArrayList<String> list = new ArrayList<String>();
            if (groups == null) {
                return list;
            }
            for (Group group : groups) {
                list.add(group.getGroupName());
            }
            return list;
        } catch (Exception e) {
            Object[] args = {username};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_LISTING_USERS,
                "Exception while listing groups for user {0}", e, args);

        }
    }

    @Override
    public void addUserToGroup(String user, String database, String group, XWikiContext context) throws XWikiException
    {
        // TODO: test this code
        MembershipHandler membershipHandler = getMembershipHandler();
        MembershipTypeHandler memberShipTypeHandler = getMembershipTypeHandler();
        boolean broadcast = false;
        Collection list = null;
        // check user and group exist membership
        try {
            list = membershipHandler.findMembershipsByUserAndGroup(user, group);
            // size = 0 -----> user and group does not exist membership
            if (list.size() == 0) {
                broadcast = true;
            }

            // link membership
            MembershipType mst = memberShipTypeHandler.findMembershipType(group);
            if (mst == null) {
                mst = memberShipTypeHandler.createMembershipTypeInstance();
            }
            User username = getUserHandler().findUserByName(user);
            Group groupname = getGroupHandler().findGroupById(group);
            membershipHandler.linkMembership(username, groupname, mst, broadcast);
            super.addUserToGroup(user, database, group, context);
        } catch (Exception e) {
            Object[] args = {user, group};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_ADDING_USERS,
                "Exception while adding user {0} to group {1}", e, args);
        }
    }

    @Override
    public List<String> listMemberForGroup(String group, XWikiContext context) throws XWikiException
    {
        UserHandler userHandler = getUserHandler();

        List<String> usersList = new ArrayList<String>();
        List<User> exoList = null;

        try {
            if (group == null) {
                PageList plist = null;
                plist = userHandler.getUserPageList(100);
                exoList = plist.getAll();
            } else {
                exoList = userHandler.findUsersByGroup(group).getAll();
            }

            if (exoList != null) {
                for (User user : exoList) {
                    usersList.add(user.getUserName());
                }
            }
            return usersList;
        } catch (Exception e) {
            Object[] args = {group};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_USERS, "Exception while listing users for group {0}",
                e, args);
        }
    }

    @Override
    public List<String> listAllGroups(XWikiContext context) throws XWikiException
    {
        GroupHandler handlerGroup = getGroupHandler();
        List<String> allGroups = new ArrayList<String>();
        List<Group> exoGroups = null;
        try {
            exoGroups = (List<Group>) handlerGroup.getAllGroups();

            if (exoGroups != null) {
                for (Group group : exoGroups) {
                    allGroups.add(group.getId());
                }
            }
            return allGroups;
        } catch (Exception e) {
            Object[] args = {};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_USERS, "Exception while listing groups", e, args);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#getAllMatchedUsers(java.lang.Object[][], boolean, int,
     *      int, java.lang.Object[][], com.xpn.xwiki.XWikiContext) TODO: fully implements this method.
     */
    @Override
    public List<String> getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb, int start,
        Object[][] order, XWikiContext context) throws XWikiException
    {
        if ((matchFields != null && matchFields.length > 0) || withdetails || (order != null && order.length > 0)) {
            throw new NotImplementedException();
        }

        List<String> usersList = listMemberForGroup(null, context);

        if (nb > 0 || start > 0) {
            int fromIndex = start < 0 ? 0 : start;
            int toIndex = fromIndex + (nb <= 0 ? usersList.size() - 1 : nb);

            usersList = usersList.subList(fromIndex, toIndex);
        }

        return usersList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#getAllMatchedGroups(java.lang.Object[][], boolean, int,
     *      int, java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    @Override
    public List<String> getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb, int start,
        Object[][] order, XWikiContext context) throws XWikiException
    {
        // TODO : fully implement this methods for eXo platform

        if ((matchFields != null && matchFields.length > 0) || withdetails || (order != null && order.length > 0)) {
            throw new NotImplementedException();
        }

        List<String> groupList = listAllGroups(context);

        if (nb > 0 || start > 0) {
            int fromIndex = start < 0 ? 0 : start;
            int toIndex = fromIndex + (nb <= 0 ? groupList.size() - 1 : nb);

            groupList = groupList.subList(fromIndex, toIndex);
        }

        return groupList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#countAllMatchedUsers(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public int countAllMatchedUsers(Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        return getAllMatchedGroups(matchFields, false, 0, 0, null, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#countAllMatchedGroups(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public int countAllMatchedGroups(Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsers(matchFields, false, 0, 0, null, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#removeUserOrGroupFromAllGroups(java.lang.String,
     *      java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext) TODO: fully implements this method.
     */
    @Override
    public void removeUserOrGroupFromAllGroups(String memberWiki, String memberSpace, String memberName,
        XWikiContext context) throws XWikiException
    {
        throw new NotImplementedException();
    }
}
