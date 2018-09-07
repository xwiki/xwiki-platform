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

package com.xpn.xwiki.plugin.rightsmanager;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * API for managing rights, users and groups.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerPluginApi extends PluginApi<RightsManagerPlugin>
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * The logging toolkit.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerPluginApi.class);

    /**
     * API for managing rights and inheritance.
     */
    private RightsManagerRightsApi rightsApi;

    /**
     * API for managing users.
     */
    private RightsManagerUsersApi usersApi;

    /**
     * API for managing groups.
     */
    private RightsManagerGroupsApi groupsApi;

    /**
     * Create an instance of the Rights Manager plugin user api.
     *
     * @param plugin the entry point of the Rights Manager plugin.
     * @param context the XWiki context.
     */
    public RightsManagerPluginApi(RightsManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        this.rightsApi = new RightsManagerRightsApi(context);
        this.usersApi = new RightsManagerUsersApi(context);
        this.groupsApi = new RightsManagerGroupsApi(context);
    }

    /**
     * @return the API for managing rights and inheritance.
     */
    public RightsManagerRightsApi getRightsApi()
    {
        return this.rightsApi;
    }

    /**
     * @return the API for managing users.
     */
    public RightsManagerUsersApi getUsersApi()
    {
        return this.usersApi;
    }

    /**
     * @return the API for managing groups.
     */
    public RightsManagerGroupsApi getGroupsApi()
    {
        return this.groupsApi;
    }

    /**
     * Convert Map/List pattern matching parameter from Velocity to [][] used in {@link RightsManager}.
     *
     * @param map a map of list from Velocity.
     * @return a table of table for {@link RightsManager} methods.
     */
    static Object[][] createMatchingTable(Map<?, ?> map)
    {
        if (map == null || map.size() == 0) {
            return null;
        }

        Object[][] table = new Object[map.size()][3];

        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            table[i][0] = entry.getKey();

            if (entry.getValue() instanceof List) {
                List<?> typeValue = (List<?>) entry.getValue();
                table[i][1] = typeValue.get(0);
                if (typeValue.size() > 1) {
                    table[i][2] = typeValue.get(1);
                }
            } else {
                table[i][2] = entry.getValue();
            }

            ++i;
        }

        return table;
    }

    /**
     * Convert List/List order fields from Velocity to [][] used in {@link RightsManager}.
     *
     * @param list a list of list from Velocity.
     * @return a table of table for {@link RightsManager} methods.
     */
    static Object[][] createOrderTable(List<?> list)
    {
        if (list == null || list.size() == 0) {
            return null;
        }

        Object[][] table = new Object[list.size()][3];

        int i = 0;
        for (Object entry : list) {
            if (entry instanceof List) {
                List<?> fieldParams = (List<?>) entry;
                table[i][0] = fieldParams.get(0);
                if (fieldParams.size() > 1) {
                    table[i][1] = fieldParams.get(1);
                }
                if (fieldParams.size() > 2) {
                    table[i][2] = fieldParams.get(2);
                }
            } else {
                table[i][0] = entry.toString();
            }

            ++i;
        }

        return table;
    }

    /**
     * Log error and register {@link #CONTEXT_LASTERRORCODE} and {@link #CONTEXT_LASTEXCEPTION}.
     *
     * @param comment the comment to use with {@link #LOGGER}.
     * @param e the exception.
     */
    private void logError(String comment, XWikiException e)
    {
        LOGGER.error(comment, e);

        this.context.put(CONTEXT_LASTERRORCODE, e.getCode());
        this.context.put(CONTEXT_LASTEXCEPTION, e);
    }

    // Groups management

    /**
     * Get all groups containing provided user.
     *
     * @param member the name of the member (user or group).
     * @return the {@link Collection} of {@link String} containing group name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection<String> getAllGroupsNamesForMember(String member) throws XWikiException
    {
        Collection<String> memberList;

        try {
            memberList = RightsManager.getInstance().getAllGroupsNamesForMember(member, 0, 0, this.context);
        } catch (RightsManagerException e) {
            logError(MessageFormat.format("Try to get all groups containing user [{0}]", new Object[] { member }), e);

            memberList = Collections.emptyList();
        }

        return memberList;
    }

    /**
     * Get all members (users or groups) provided group contains.
     *
     * @param group the name of the group.
     * @return the {@link Collection} of {@link String} containing member (user or group) name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection<String> getAllMembersNamesForGroup(String group) throws XWikiException
    {
        return getAllMembersNamesForGroup(group, 0, 0);
    }

    /**
     * Get all members (users or groups) provided group contains.
     *
     * @param group the name of the group.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return the {@link Collection} of {@link String} containing member (user or group) name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection<String> getAllMembersNamesForGroup(String group, int nb, int start) throws XWikiException
    {
        return getAllMatchedMembersNamesForGroup(group, null, nb, start, null);
    }

    /**
     * Get members of provided group.
     *
     * @param group the group.
     * @param matchField a string to search in result to filter.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param orderAsc if true, the result is ordered ascendent, if false it descendant. If null no order is applied.
     * @return the {@link Collection} of {@link String} containing member name.
     * @throws XWikiException error when browsing groups.
     * @since 1.6M1
     */
    public Collection<String> getAllMatchedMembersNamesForGroup(String group, String matchField, int nb, int start,
        Boolean orderAsc) throws XWikiException
    {
        Collection<String> memberList;

        try {
            memberList =
                RightsManager.getInstance().getAllMatchedMembersNamesForGroup(group, matchField, nb, start, orderAsc,
                    this.context);
        } catch (RightsManagerException e) {
            logError(MessageFormat.format("Try to get all matched member of group [{0}]", new Object[] { group }), e);

            memberList = Collections.emptyList();
        }

        return memberList;
    }

    /**
     * Filters the members of the specified group using the given text and counts the results.
     *
     * @param group the group whose members are going to be counted
     * @param filter the text used to filter the group members
     * @return the number of group members that match the given text filter
     * @see #getAllMatchedMembersNamesForGroup(String, String, int, int, Boolean)
     * @since 10.8RC1
     */
    public int countAllMatchedMembersNamesForGroup(String group, String filter)
    {
        try {
            return RightsManager.getInstance().countAllMatchedMembersNamesForGroup(group, filter, this.context);
        } catch (XWikiException e) {
            logError("Failed to count the group members that match a given text filter.", e);
            return 0;
        }
    }

    /**
     * Return the number of groups containing provided member.
     *
     * @param member the name of the member (user or group).
     * @return the number of groups.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllGroupsNamesForMember(String member) throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllGroupsNamesForMember(member, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count groups containing provided user", e);
        }

        return count;
    }

    /**
     * Return the number of members provided group contains.
     *
     * @param group the name of the group.
     * @return the number of members.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMembersNamesForGroup(String group) throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllMembersNamesForGroup(group, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all members provided group contains", e);
        }

        return count;
    }
}
