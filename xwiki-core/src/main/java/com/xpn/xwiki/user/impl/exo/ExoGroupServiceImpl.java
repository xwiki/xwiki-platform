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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;

import org.apache.commons.lang.NotImplementedException;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.organization.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ExoGroupServiceImpl extends XWikiGroupServiceImpl implements XWikiGroupService
{
    private static OrganizationService organizationService;

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
            organizationService =
                (OrganizationService) manager
                    .getComponentInstanceOfType(OrganizationService.class);
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

    public Collection listGroupsForUser(String username, XWikiContext context)
        throws XWikiException
    {
        GroupHandler groupHandler = getGroupHandler();
        Collection groups = null;
        try {
            if (username.startsWith("XWiki.")) {
                username = username.substring(6);
                groups = groupHandler.findGroupsOfUser(username);
            }

            ArrayList list = new ArrayList();
            if (groups == null)
                return list;
            Iterator it = groups.iterator();
            while (it.hasNext()) {
                Group group = (Group) it.next();
                list.add(group.getGroupName());
            }
            return list;
        } catch (Exception e) {
            Object[] args = {username};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_LISTING_USERS,
                "Exception while listing groups for user {0}",
                e,
                args);

        }
    }

    public void addUserToGroup(String user, String database, String group, XWikiContext context)
        throws XWikiException
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
                "Exception while adding user {0} to group {1}",
                e,
                args);
        }
    }

    public List listMemberForGroup(String group, XWikiContext context) throws XWikiException
    {
        UserHandler userHandler = getUserHandler();

        List usersList = new ArrayList();
        List exoList = null;

        try {
            if (group == null) {
                PageList plist = null;
                plist = userHandler.getUserPageList(100);
                exoList = plist.getAll();
            } else
                exoList = userHandler.findUsersByGroup(group).getAll();

            if (exoList != null) {
                for (int i = 0; i < exoList.size(); i++) {
                    User user = (User) exoList.get(i);
                    usersList.add(user.getUserName());
                }
            }
            return usersList;
        } catch (Exception e) {
            Object[] args = {group};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_USERS,
                "Exception while listing users for group {0}",
                e,
                args);
        }
    }

    public List listAllGroups(XWikiContext context) throws XWikiException
    {
        GroupHandler handlerGroup = getGroupHandler();
        List allGroups = new ArrayList();
        List exoGroups = null;
        try {
            exoGroups = (List) handlerGroup.getAllGroups();

            if (exoGroups != null) {
                for (int i = 0; i < exoGroups.size(); i++) {
                    Group group = (Group) exoGroups.get(i);
                    allGroups.add(group.getId());
                }
            }
            return allGroups;
        } catch (Exception e) {
            Object[] args = {};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_EXO_EXCEPTION_USERS,
                "Exception while listing groups",
                e,
                args);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#getAllMatchedUsers(java.lang.Object[][],
     *      boolean, int, int, java.lang.Object[][], com.xpn.xwiki.XWikiContext) TODO: fully
     *      implements this method.
     */
    public List getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        if ((matchFields != null && matchFields.length > 0) || withdetails
            || (order != null && order.length > 0)) {
            throw new NotImplementedException();
        }

        List usersList = listMemberForGroup(null, context);

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
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#getAllMatchedGroups(java.lang.Object[][],
     *      boolean, int, int, java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public List getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        // TODO : fully implement this methods for eXo platform

        if ((matchFields != null && matchFields.length > 0) || withdetails
            || (order != null && order.length > 0)) {
            throw new NotImplementedException();
        }

        List groupList = listAllGroups(context);

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
    public int countAllMatchedUsers(Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        return getAllMatchedGroups(matchFields, false, 0, 0, null, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#countAllMatchedGroups(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMatchedGroups(Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        return getAllMatchedUsers(matchFields, false, 0, 0, null, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl#removeUserOrGroupFromAllGroups(java.lang.String,
     *      java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext) TODO: fully implements
     *      this method.
     */
    public void removeUserOrGroupFromAllGroups(String memberWiki, String memberSpace,
        String memberName, XWikiContext context) throws XWikiException
    {
        throw new NotImplementedException();
    }
}
