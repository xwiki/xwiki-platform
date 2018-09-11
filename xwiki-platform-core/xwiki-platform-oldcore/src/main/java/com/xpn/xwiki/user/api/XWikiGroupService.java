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
package com.xpn.xwiki.user.api;

import java.util.Collection;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Interface giving access to users and groups management.
 *
 * @version $Id$
 */
public interface XWikiGroupService
{
    void init(XWiki xwiki, XWikiContext context) throws XWikiException;

    /**
     * @deprecated since 10.8RC1, use GroupManager component instead
     */
    @Deprecated
    void initCache(XWikiContext context) throws XWikiException;

    /**
     * @deprecated since 10.8RC1, use GroupManager component instead
     */
    @Deprecated
    void initCache(int iCapacity, XWikiContext context) throws XWikiException;

    /**
     * @deprecated since 10.8RC1, use GroupManager component instead
     */
    @Deprecated
    void flushCache();

    /**
     * @deprecated Use {@link #getAllGroupsNamesForMember(String, int, int, XWikiContext)}.
     */
    @Deprecated
    Collection<String> listGroupsForUser(String username, XWikiContext context) throws XWikiException;

    /**
     * @deprecated should never be used ! There is a listener taking care of that automatically.
     */
    @Deprecated
    void addUserToGroup(String user, String database, String group, XWikiContext context) throws XWikiException;

    /**
     * Remove user or group name from all groups.
     *
     * @param userOrGroupWiki the name of the wiki of the member.
     * @param userOrGroupSpace the name of the space of the member.
     * @param userOrGroupName the name of the member.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    void removeUserOrGroupFromAllGroups(String userOrGroupWiki, String userOrGroupSpace, String userOrGroupName,
        XWikiContext context) throws XWikiException;

    /**
     * @deprecated Use {@link #getAllMembersNamesForGroup(String, int, int, XWikiContext)}.
     */
    @Deprecated
    List<String> listMemberForGroup(String s, XWikiContext context) throws XWikiException;

    /**
     * @deprecated Use {@link #getAllMatchedGroups(Object[][], boolean, int, int, Object[][], XWikiContext)}.
     */
    @Deprecated
    List<String> listAllGroups(XWikiContext context) throws XWikiException;

    /**
     * Search for all users with provided constraints and in a provided order.
     *
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if a {@link List} containing {@link String} names is returned or {@link List}
     *            containing {@link com.xpn.xwiki.doc.XWikiDocument}.
     * @param nb the maximum number of results to return. Infinite if 0.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>asc : a Boolean, if true the order is ascendent</li>
     *            </ul>
     * @param context the {@link XWikiContext}.
     * @return the list of users.
     * @throws XWikiException error when getting users.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<?> getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb, int start, Object[][] order,
        XWikiContext context) throws XWikiException;

    /**
     * Search for all groups with provided constraints and in a provided order.
     *
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     *            .
     * @param withdetails indicate if a {@link List} containing {@link String} names is returned or {@link List}
     *            containing {@link com.xpn.xwiki.doc.XWikiDocument}.
     * @param nb the maximum number of result to return. Infinite if 0.
     * @param start the index of the first found group to return.
     * @param order the field to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>asc : a Boolean, if true the order is ascendent</li>
     *            </ul>
     * @param context the {@link XWikiContext}.
     * @return the list of groups.
     * @throws XWikiException error when getting groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<?> getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb, int start, Object[][] order,
        XWikiContext context) throws XWikiException;

    /**
     * Return number of users with provided constraints.
     *
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     *            .
     * @param context the {@link XWikiContext}.
     * @return the of found users.
     * @throws XWikiException error when getting number of users.
     * @since 1.1.2
     * @since 1.2M2
     */
    int countAllMatchedUsers(Object[][] matchFields, XWikiContext context) throws XWikiException;

    /**
     * Return number of groups with provided constraints.
     *
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     *            .
     * @param context the {@link XWikiContext}.
     * @return the of found groups.
     * @throws XWikiException error when getting number of groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    int countAllMatchedGroups(Object[][] matchFields, XWikiContext context) throws XWikiException;

    /**
     * Get all groups containing provided member in the provided member wiki.
     * <p>
     * Since 10.8RC1 it's recommended to use org.xwiki.user.group.GroupManager unless you want to bypass the cache.
     *
     * @param member the name of the member (user or group).
     * @param nb the maximum number of result to return.
     * @param start the index of the first found member to return.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link String} containing group name.
     * @throws XWikiException error when browsing groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    Collection<String> getAllGroupsNamesForMember(String member, int nb, int start, XWikiContext context)
        throws XWikiException;

    /**
     * Get all groups containing provided member in the current wiki.
     * <p>
     * Since 10.8RC1 it's recommended to use org.xwiki.user.group.GroupManager unless you want to bypass the cache.
     *
     * @param memberReference the member. Can be either user or group.
     * @param limit the maximum number of result to return.
     * @param offset the index of the first found member to return.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link DocumentReference} containing representing groups.
     * @throws XWikiException error when browsing groups.
     * @since 2.4M2
     */
    Collection<DocumentReference> getAllGroupsReferencesForMember(DocumentReference memberReference, int limit,
        int offset, XWikiContext context) throws XWikiException;

    /**
     * Get all members provided group contains.
     * <p>
     * Since 10.8RC1 it's recommended to use org.xwiki.user.group.GroupManager unless you want to bypass the cache.
     *
     * @param group the name of the group.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link String} containing member name.
     * @throws XWikiException error when browsing groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    Collection<String> getAllMembersNamesForGroup(String group, int nb, int start, XWikiContext context)
        throws XWikiException;

    /**
     * Get members of provided group.
     *
     * @param group the group.
     * @param matchField a string to search in result to filter.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param orderAsc if true, the result is ordered ascendent, if false it descendant. If null no order is applied.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link String} containing member name.
     * @throws XWikiException error when browsing groups.
     * @since 1.6M1
     */
    Collection<String> getAllMatchedMembersNamesForGroup(String group, String matchField, int nb, int start,
        Boolean orderAsc, XWikiContext context) throws XWikiException;

    /**
     * Filters the members of the specified group using the given text and counts the results.
     *
     * @param group the group whose members are going to be counted
     * @param filter the text used to filter the group members
     * @param xcontext the XWiki context
     * @return the number of group members that match the given text filter
     * @throws XWikiException if counting the group members fails
     * @see #getAllMatchedMembersNamesForGroup(String, String, int, int, Boolean, XWikiContext)
     * @since 10.8RC1
     */
    default int countAllMatchedMembersNamesForGroup(String group, String filter, XWikiContext xcontext)
        throws XWikiException
    {
        return getAllMatchedMembersNamesForGroup(group, filter, 0, 0, null, xcontext).size();
    }

    /**
     * Return the number of groups containing provided member.
     * <p>
     * Since 10.8RC1 it's recommended to use org.xwiki.user.group.GroupManager unless you want to bypass the cache.
     *
     * @param member the name of the member (user or group).
     * @param context the XWiki context.
     * @return the number of groups.
     * @throws XWikiException error when getting number of users.
     * @since 1.1.2
     * @since 1.2M2
     */
    int countAllGroupsNamesForMember(String member, XWikiContext context) throws XWikiException;

    /**
     * Return the number of members provided group contains.
     * <p>
     * Since 10.8RC1 it's recommended to use org.xwiki.user.group.GroupManager unless you want to bypass the cache.
     *
     * @param group the name of the group.
     * @param context the XWiki context.
     * @return the number of members.
     * @throws XWikiException error when getting number of groups.
     * @since 1.1.2
     * @since 1.2M2
     */
    int countAllMembersNamesForGroup(String group, XWikiContext context) throws XWikiException;
}
