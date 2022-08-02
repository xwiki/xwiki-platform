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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.plugin.rightsmanager.ReferenceUserIterator;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.rightsmanager.utils.AllowDeny;
import com.xpn.xwiki.plugin.rightsmanager.utils.LevelTree;
import com.xpn.xwiki.plugin.rightsmanager.utils.RequestLimit;
import com.xpn.xwiki.plugin.rightsmanager.utils.UsersGroups;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.web.Utils;

/**
 * Hidden toolkit used by the plugin API that make all the plugin's actions.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public final class RightsManager
{
    /**
     * Name of the default space where users and groups are stored.
     */
    public static final String DEFAULT_USERORGROUP_SPACE = "XWiki";

    /**
     * Separator symbol between wiki name and page full name.
     */
    private static final String WIKIFULLNAME_SEP = ":";

    /**
     * Separator symbol between space name and page name.
     */
    private static final String SPACEPAGENAME_SEP = ".";

    /**
     * Full name of the wiki preferences document.
     */
    private static final String WIKI_PREFERENCES = "XWiki.XWikiPreferences";

    /**
     * Name of the space preferences document.
     */
    private static final String SPACE_PREFERENCES = "WebPreferences";

    /**
     * Name of the "levels" field for the {@link #RIGHTS_CLASS} and {@link #GLOBAL_RIGHTS_CLASS} classes.
     */
    private static final String RIGHTSFIELD_LEVELS = "levels";

    /**
     * Name of the "users" field for the {@link #RIGHTS_CLASS} and {@link #GLOBAL_RIGHTS_CLASS} classes.
     */
    private static final String RIGHTSFIELD_USERS = "users";

    /**
     * Name of the "groups" field for the {@link #RIGHTS_CLASS} and {@link #GLOBAL_RIGHTS_CLASS} classes.
     */
    private static final String RIGHTSFIELD_GROUPS = "groups";

    /**
     * Name of the "allow" field for the {@link #RIGHTS_CLASS} and {@link #GLOBAL_RIGHTS_CLASS} classes.
     */
    private static final String RIGHTSFIELD_ALLOW = "allow";

    /**
     * Separator symbols of the list fields for the {@link #RIGHTS_CLASS} and {@link #GLOBAL_RIGHTS_CLASS} classes.
     */
    private static final String RIGHTSLISTFIELD_SEP = ",|";

    /**
     * Separator symbol of the list fields used for users and groups fields.
     */
    private static final String USERGROUPLISTFIELD_SEP = ",";

    /**
     * Symbol use in HQL "like" command that means "all characters".
     */
    private static final String HQLLIKE_ALL_SYMBOL = "%";

    /**
     * Use to retrieve documents concerned by a specific user or group rights.
     * See {@link #removeUserOrGroupFromAllRights(String, String, String, boolean, XWikiContext)}
     * and {@link #replaceUserOrGroupFromAllRights(DocumentReference, DocumentReference, boolean, XWikiContext)}.
     */
    private static final String ALL_RIGHTS_QUERY = ", BaseObject as obj, %s as prop "
        + "where doc.fullName=obj.name and (obj.className=?1 or obj.className=?2) "
        + "and obj.id=prop.id.id and prop.name=?3 and prop.value like ?4";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Unique instance of RightsManager.
     */
    private static RightsManager instance;

    /**
     * Used to resolve document reference based on another reference.
     */
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");

    /**
     * Used to resolve reference based on context.
     */
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "current");

    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    private EntityReferenceSerializer<String> localEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");

    /**
     * Hidden constructor of RightsManager only access via getInstance().
     */
    private RightsManager()
    {
    }

    /**
     * @return a unique instance of RightsManager. Thread safe.
     */
    public static RightsManager getInstance()
    {
        synchronized (RightsManager.class) {
            if (instance == null) {
                instance = new RightsManager();
            }
        }

        return instance;
    }

    // Groups and users management

    /**
     * Get the number of users or groups in the main wiki and the current wiki.
     *
     * @param user indicate if methods search for users or groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param context the XWiki context.
     * @return the number of groups in the main wiki and the current wiki.
     * @throws XWikiException error when getting number of users or groups.
     */
    public int countAllUsersOrGroups(boolean user, Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        if (context.isMainWiki()) {
            return countAllLocalUsersOrGroups(user, matchFields, context);
        }

        return countAllGlobalUsersOrGroups(user, matchFields, context)
            + countAllLocalUsersOrGroups(user, matchFields, context);
    }

    /**
     * Get the number of users or groups in the provided wiki.
     *
     * @param user indicate if methods search for users or groups.
     * @param wikiName the name of the wiki where to search for users or groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param context the XWiki context.
     * @return the number of groups in the provided wiki.
     * @throws XWikiException error when getting number of users or groups.
     */
    public int countAllWikiUsersOrGroups(boolean user, String wikiName, Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        if (context.isMainWiki()) {
            return countAllLocalUsersOrGroups(user, matchFields, context);
        }

        String database = context.getWikiId();

        try {
            context.setWikiId(wikiName);
            return countAllLocalUsersOrGroups(user, matchFields, context);
        } finally {
            context.setWikiId(database);
        }
    }

    /**
     * Get the number of users or groups in the main wiki.
     *
     * @param user indicate if methods search for users or groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param context the XWiki context.
     * @return the number of groups in the main wiki.
     * @throws XWikiException error when getting number of users or groups.
     */
    public int countAllGlobalUsersOrGroups(boolean user, Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        if (context.isMainWiki()) {
            return countAllLocalUsersOrGroups(user, matchFields, context);
        }

        return countAllWikiUsersOrGroups(user, context.getMainXWiki(), matchFields, context);
    }

    /**
     * Get the number of users or groups in the current wiki.
     *
     * @param user indicate if methods search for users or groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param context the XWiki context.
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of users or groups.
     */
    public int countAllLocalUsersOrGroups(boolean user, Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        if (user) {
            return context.getWiki().getGroupService(context).countAllMatchedUsers(matchFields, context);
        } else {
            return context.getWiki().getGroupService(context).countAllMatchedGroups(matchFields, context);
        }
    }

    /**
     * Get all users or groups in the main wiki and the current.
     *
     * @param user indicate if it is a user or a group.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if the methods return {@link List} or {@link String} or {@link List} of
     *            {@link XWikiDocument}.
     * @param limit the maximum number of result to return and index of the first element.
     * @param order the fields to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            </ul>
     * @param context the XWiki context.
     * @return a {@link List} of {@link String} containing user or group name if <code>withdetails</code> is false,
     *         otherwise a {@link List} of {@link XWikiDocument} containing user or group.
     * @throws XWikiException error when searching from users or groups.
     */
    public List<?> getAllMatchedUsersOrGroups(boolean user, Object[][] matchFields, boolean withdetails,
        RequestLimit limit, Object[][] order, XWikiContext context) throws XWikiException
    {
        if (context.isMainWiki()) {
            return getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails, limit, order, context);
        }

        List<Object> userOrGroupList = new ArrayList<>();

        int nbGlobalUsersOrGroups = countAllGlobalUsersOrGroups(user, null, context);

        int newstart = limit.getStart();

        // Get global groups
        if (newstart < nbGlobalUsersOrGroups) {
            userOrGroupList.addAll(getAllMatchedGlobalUsersOrGroups(user, matchFields, withdetails,
                new RequestLimit(limit.getNb(), newstart), order, context));
            newstart = 0;
        } else {
            newstart = newstart - nbGlobalUsersOrGroups;
        }

        // Get local groups
        if (limit.getNb() > userOrGroupList.size()) {
            userOrGroupList.addAll(getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails,
                new RequestLimit(limit.getNb() - userOrGroupList.size(), newstart), order, context));
        } else if (limit.getNb() <= 0) {
            userOrGroupList.addAll(getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails,
                new RequestLimit(0, newstart), order, context));
        }

        return userOrGroupList;
    }

    /**
     * Get all users or groups in the main wiki.
     *
     * @param user indicate if it is a user or a group.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if the methods return {@link List} or {@link String} or {@link List} of
     *            {@link XWikiDocument}.
     * @param limit the maximum number of result to return and index of the first element.
     * @param order the fields to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            </ul>
     * @param context the XWiki context.
     * @return a {@link List} of {@link String} containing user or group name if <code>withdetails</code> is false,
     *         otherwise a {@link List} of {@link XWikiDocument} containing user or group.
     * @throws XWikiException error when searching from users or groups.
     */
    public List<?> getAllMatchedGlobalUsersOrGroups(boolean user, Object[][] matchFields, boolean withdetails,
        RequestLimit limit, Object[][] order, XWikiContext context) throws XWikiException
    {
        if (context.isMainWiki()) {
            return getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails, limit, order, context);
        }

        return getAllMatchedWikiUsersOrGroups(user, context.getMainXWiki(), matchFields, withdetails, limit, order,
            context);
    }

    /**
     * Get all users or groups in the provided wiki.
     *
     * @param user indicate if it is a user or a group.
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if the methods return {@link List} or {@link String} or {@link List} of
     *            {@link XWikiDocument}.
     * @param limit the maximum number of result to return and index of the first element.
     * @param order the fields to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            </ul>
     * @param context the XWiki context.
     * @return a {@link List} of {@link String} containing user or group name if <code>withdetails</code> is false,
     *         otherwise a {@link List} of {@link XWikiDocument} containing user or group.
     * @throws XWikiException error when searching from users or groups.
     */
    public List<?> getAllMatchedWikiUsersOrGroups(boolean user, String wikiName, Object[][] matchFields,
        boolean withdetails, RequestLimit limit, Object[][] order, XWikiContext context) throws XWikiException
    {
        if (context.isMainWiki()) {
            return getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails, limit, order, context);
        }

        String database = context.getWikiId();

        try {
            context.setWikiId(wikiName);

            List<?> localGroupList =
                getAllMatchedLocalUsersOrGroups(user, matchFields, withdetails, limit, order, context);

            if (localGroupList != null && !withdetails) {
                List<String> wikiGroupList = new ArrayList<>(localGroupList.size());
                for (Object groupName : localGroupList) {
                    wikiGroupList.add(wikiName + WIKIFULLNAME_SEP + groupName);
                }

                localGroupList = wikiGroupList;
            }

            return localGroupList;
        } finally {
            context.setWikiId(database);
        }
    }

    /**
     * Get all users or groups in the current wiki.
     *
     * @param user indicate if it is a user or a group.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if the methods return {@link List} or {@link String} or {@link List} of
     *            {@link XWikiDocument}.
     * @param limit the maximum number of result to return and index of the first element.
     * @param order the fields to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            </ul>
     * @param context the XWiki context.
     * @return a {@link List} of {@link String} containing user or group name if <code>withdetails</code> is false,
     *         otherwise a {@link List} of {@link XWikiDocument} containing user or group.
     * @throws XWikiException error when searching from users or groups.
     */
    public List<?> getAllMatchedLocalUsersOrGroups(boolean user, Object[][] matchFields, boolean withdetails,
        RequestLimit limit, Object[][] order, XWikiContext context) throws XWikiException
    {
        if (user) {
            return context.getWiki().getGroupService(context).getAllMatchedUsers(matchFields, withdetails,
                limit.getNb(), limit.getStart(), order, context);
        } else {
            return context.getWiki().getGroupService(context).getAllMatchedGroups(matchFields, withdetails,
                limit.getNb(), limit.getStart(), order, context);
        }
    }

    /**
     * Get all groups containing provided user.
     *
     * @param member the name of the member (user or group).
     * @param nb the maximum number of result to return.
     * @param start the index of the first found member to return.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link String} containing group name.
     * @throws XWikiException error when browsing groups.
     * @deprecated since 10.8RC1, use org.xwiki.user.group.GroupManager component instead
     */
    @Deprecated
    public Collection<String> getAllGroupsNamesForMember(String member, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return context.getWiki().getGroupService(context).getAllGroupsNamesForMember(member, nb, start, context);
    }

    /**
     * Get all users provided group contains.
     *
     * @param group the name of the group.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param context the XWiki context.
     * @return the {@link Collection} of {@link String} containing user name.
     * @throws XWikiException error when browsing groups.
     * @deprecated since 10.8RC1, use org.xwiki.user.group.GroupManager component instead
     */
    @Deprecated
    public Collection<String> getAllMembersNamesForGroup(String group, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return context.getWiki().getGroupService(context).getAllMembersNamesForGroup(group, nb, start, context);
    }

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
    public Collection<String> getAllMatchedMembersNamesForGroup(String group, String matchField, int nb, int start,
        Boolean orderAsc, XWikiContext context) throws XWikiException
    {
        return context.getWiki().getGroupService(context).getAllMatchedMembersNamesForGroup(group, matchField, nb,
            start, orderAsc, context);
    }

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
    public int countAllMatchedMembersNamesForGroup(String group, String filter, XWikiContext xcontext)
        throws XWikiException
    {
        return xcontext.getWiki().getGroupService(xcontext).countAllMatchedMembersNamesForGroup(group, filter,
            xcontext);
    }

    /**
     * Return the number of groups containing provided member.
     *
     * @param member the name of the member (user or group).
     * @param context the XWiki context.
     * @return the number of groups.
     * @throws XWikiException error when getting number of users.
     * @deprecated since 10.8RC1, use org.xwiki.user.group.GroupManager component instead
     */
    @Deprecated
    public int countAllGroupsNamesForMember(String member, XWikiContext context) throws XWikiException
    {
        return context.getWiki().getGroupService(context).countAllGroupsNamesForMember(member, context);
    }

    /**
     * Return the number of members provided group contains.
     *
     * @param group the name of the group.
     * @param context the XWiki context.
     * @return the number of members.
     * @throws XWikiException error when getting number of groups.
     * @deprecated since 10.8RC1, use org.xwiki.user.group.GroupManager component instead
     */
    @Deprecated
    public int countAllMembersNamesForGroup(String group, XWikiContext context) throws XWikiException
    {
        return context.getWiki().getGroupService(context).countAllMembersNamesForGroup(group, context);
    }

    // Rights management

    /**
     * Get the {@link LevelTree} {@link Map} for the provided rights levels.
     *
     * @param preferences the document containing rights preferences.
     * @param levelsToMatch the levels names to check ("view", "edit", etc.).
     * @param global indicate it is global rights (wiki or space) or document rights.
     * @param context the XWiki context.
     * @return the {@link Map} containing [levelname : {@link LevelTree}].
     * @throws XWikiException error when browsing rights preferences.
     */
    private Map<String, LevelTree> getLevelTreeMap(XWikiDocument preferences, List<String> levelsToMatch,
        boolean global, XWikiContext context) throws XWikiException
    {
        Map<String, LevelTree> rightsMap = new HashMap<>();

        fillLevelTreeMap(rightsMap, preferences, levelsToMatch, global, true, context);

        return rightsMap;
    }

    /**
     * Get the {@link LevelTree} {@link Map} for he provided rights levels.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param levelsToMatch the levels names to check ("view", "edit", etc.).
     * @param context the XWiki context.
     * @return the {@link Map} containing [levelname : {@link LevelTree}].
     * @throws XWikiException error when browsing rights preferences.
     */
    public Map<String, LevelTree> getLevelTreeMap(String spaceOrPage, List<String> levelsToMatch, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument preferences = getXWikiPreferencesDoc(spaceOrPage, context);

        return getLevelTreeMap(preferences, levelsToMatch, isGlobal(preferences, spaceOrPage), context);
    }

    /**
     * Fill the {@link LevelTree} {@link Map}.
     *
     * @param rightsMap the {@link LevelTree} {@link Map} to fill.
     * @param levelInherited the levels names for which to find inheritance.
     * @param bobj the object containing rights preferences.
     * @param levelsToMatch the levels names to check ("view", "edit", etc.).
     * @param direct if true fill the {@link LevelTree#direct} field, otherwise fill the {@link LevelTree#inherited}.
     * @param context the XWiki context.
     */
    private void fillLevelTreeMap(Map<String, LevelTree> rightsMap, List<String> levelInherited, BaseObject bobj,
        List<String> levelsToMatch, boolean direct, XWikiContext context)
    {
        List<String> users =
            ListClass.getListFromString(bobj.getStringValue(RIGHTSFIELD_USERS), RIGHTSLISTFIELD_SEP, false);
        List<String> groups =
            ListClass.getListFromString(bobj.getStringValue(RIGHTSFIELD_GROUPS), RIGHTSLISTFIELD_SEP, false);
        List<String> levels =
            ListClass.getListFromString(bobj.getStringValue(RIGHTSFIELD_LEVELS), RIGHTSLISTFIELD_SEP, false);
        boolean allow = (bobj.getIntValue(RIGHTSFIELD_ALLOW) == 1);

        for (String levelName : levels) {
            if (levelsToMatch == null || levelsToMatch.contains(levelName)) {
                LevelTree level;
                if (!rightsMap.containsKey(levelName)) {
                    level = new LevelTree();
                    rightsMap.put(levelName, level);
                } else {
                    level = rightsMap.get(levelName);
                }

                AllowDeny allowdeny;
                if (direct) {
                    if (levelInherited != null) {
                        levelInherited.add(levelName);
                    }

                    if (level.direct == null) {
                        level.direct = new AllowDeny();
                    }
                    allowdeny = level.direct;
                } else {
                    if (level.inherited == null) {
                        level.inherited = new AllowDeny();
                    }
                    allowdeny = level.inherited;
                }

                UsersGroups usersgroups = allow ? allowdeny.allow : allowdeny.deny;

                usersgroups.users.addAll(users);
                usersgroups.groups.addAll(groups);
            }
        }
    }

    /**
     * Fill the {@link LevelTree} {@link Map} inherited part.
     *
     * @param rightsMap the {@link LevelTree} {@link Map} to fill.
     * @param levelInheritedIn the levels names for which to find inheritance.
     * @param preferences the document containing rights preferences.
     * @param levelsToMatch the levels names to check ("view", "edit", etc.).
     * @param global indicate it is global rights (wiki or space) or document rights.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing rights preferences.
     */
    private void fillLevelTreeMapInherited(Map<String, LevelTree> rightsMap, List<String> levelInheritedIn,
        XWikiDocument preferences, List<String> levelsToMatch, boolean global, XWikiContext context)
        throws XWikiException
    {
        List<String> levelInherited = levelInheritedIn;

        // Get document containing inherited rights
        XWikiDocument parentPreferences = getParentPreference(preferences, global, context);

        if (parentPreferences != null) {
            // Fill levels where to find inheritance
            if (levelInherited == null) {
                levelInherited = new ArrayList<>();
            }

            for (String levelName : levelsToMatch) {
                if (!rightsMap.containsKey(levelName) || (rightsMap.get(levelName)).inherited == null) {
                    levelInherited.add(levelName);
                }
            }

            // Find inheritance if needed
            if (!levelInherited.isEmpty()) {
                fillLevelTreeMap(rightsMap, parentPreferences, levelInherited, true, false, context);
            }
        }
    }

    /**
     * Fill the {@link LevelTree} {@link Map}.
     *
     * @param rightsMap the {@link LevelTree} {@link Map} to fill.
     * @param preferences the document containing rights preferences.
     * @param levelsToMatch the levels names to check ("view", "edit", etc.).
     * @param global indicate it is global rights (wiki or space) or document rights.
     * @param direct if true fill the {@link LevelTree#direct} field, otherwise fill the {@link LevelTree#inherited}.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing rights preferences.
     */
    private void fillLevelTreeMap(Map<String, LevelTree> rightsMap, XWikiDocument preferences,
        List<String> levelsToMatch, boolean global, boolean direct, XWikiContext context) throws XWikiException
    {
        List<String> levelInherited = null;
        if (levelsToMatch == null) {
            levelInherited = new ArrayList<>();
        }

        if (!preferences.isNew()) {
            EntityReference rightClassReference =
                global ? XWikiRightServiceImpl.GLOBALRIGHTCLASS_REFERENCE : XWikiRightServiceImpl.RIGHTCLASS_REFERENCE;
            List<BaseObject> rightObjects = preferences.getXObjects(rightClassReference);
            if (rightObjects != null) {
                for (BaseObject bobj : rightObjects) {
                    fillLevelTreeMap(rightsMap, levelInherited, bobj, levelsToMatch, direct, context);
                }
            }
        }

        fillLevelTreeMapInherited(rightsMap, levelInherited, preferences, levelsToMatch, global, context);
    }

    /**
     * Get the document containing inherited rights of provided document.
     *
     * @param currentPreference the document for which to find parent preferences document.
     * @param currentGlobal indicate if current preferences document is global.
     * @param context the XWiki context.
     * @return the document containing inherited rights of provided document.
     * @throws XWikiException error when browsing rights preferences.
     */
    public XWikiDocument getParentPreference(XWikiDocument currentPreference, boolean currentGlobal,
        XWikiContext context) throws XWikiException
    {
        XWikiDocument parentPreferences = null;

        if (currentGlobal) {
            if (currentPreference.getFullName().equals(WIKI_PREFERENCES)) {
                if (!context.isMainWiki()) {
                    parentPreferences = context.getWiki()
                        .getDocument(context.getMainXWiki() + WIKIFULLNAME_SEP + WIKI_PREFERENCES, context);
                }
            } else if (currentPreference.getName().equals(SPACE_PREFERENCES)) {
                String parentspace = currentPreference.getStringValue(WIKI_PREFERENCES, "parent");
                if (parentspace.trim().length() > 0) {
                    parentPreferences =
                        context.getWiki().getDocument(parentspace + SPACEPAGENAME_SEP + SPACE_PREFERENCES, context);
                } else {
                    parentPreferences = context.getWiki().getDocument(WIKI_PREFERENCES, context);
                }

                if (parentPreferences == currentPreference) {
                    parentPreferences = null;
                }
            }
        } else {
            parentPreferences = context.getWiki()
                .getDocument(currentPreference.getSpace() + SPACEPAGENAME_SEP + SPACE_PREFERENCES, context);
        }

        return parentPreferences;
    }

    /**
     * Get the document containing inherited rights of provided document.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param context the XWiki context.
     * @return the document containing inherited rights of provided document.
     * @throws XWikiException error when browsing rights preferences.
     */
    public XWikiDocument getParentPreference(String spaceOrPage, XWikiContext context) throws XWikiException
    {
        XWikiDocument preferences = getXWikiPreferencesDoc(spaceOrPage, context);

        boolean global = isGlobal(preferences, spaceOrPage);

        return getParentPreference(preferences, global, context);
    }

    /**
     * Get level right tree.
     *
     * @param doc the document containing rights preferences.
     * @param levelName the level right name ("view", "delete"...).
     * @param global indicate it is global rights (wiki or space) or document rights.
     * @param context the XWiki context.
     * @return the {@link LevelTree}.
     * @throws XWikiException error when browsing rights preferences.
     */
    private LevelTree getLevel(XWikiDocument doc, String levelName, boolean global, XWikiContext context)
        throws XWikiException
    {
        if (doc.isNew()) {
            return null;
        }

        List<String> rights = new ArrayList<>();
        rights.add(levelName);

        Map<String, LevelTree> rightsMap = getLevelTreeMap(doc, rights, global, context);

        return rightsMap.get(levelName);
    }

    /**
     * Get document containing rights preferences for provided wiki, space or document.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param context the XWiki context.
     * @return the document containing rights preferences.
     * @throws XWikiException error when getting document from database.
     */
    private XWikiDocument getXWikiPreferencesDoc(String spaceOrPage, XWikiContext context) throws XWikiException
    {
        XWikiDocument xwikidoc = null;

        if (spaceOrPage != null) {
            xwikidoc = context.getWiki().getDocument(spaceOrPage, context);

            if (xwikidoc.isNew()) {
                xwikidoc = context.getWiki().getDocument(spaceOrPage + SPACEPAGENAME_SEP + SPACE_PREFERENCES, context);
            }
        } else {
            xwikidoc = context.getWiki().getDocument(WIKI_PREFERENCES, context);
        }

        return xwikidoc;
    }

    /**
     * Indicate if provided document contains global or document rights.
     *
     * @param preferences the document containing rights preferences.
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @return true if provided document contains global rights, false otherwise.
     */
    private boolean isGlobal(XWikiDocument preferences, String spaceOrPage)
    {
        return !preferences.getFullName().equals(spaceOrPage);
    }

    /**
     * Get level right tree.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param levelName the level right name ("view", "delete"...).
     * @param context the XWiki context.
     * @return the {@link LevelTree}.
     * @throws XWikiException error when browsing rights.
     */
    public LevelTree getTreeLevel(String spaceOrPage, String levelName, XWikiContext context) throws XWikiException
    {
        XWikiDocument preferences = getXWikiPreferencesDoc(spaceOrPage, context);

        return getLevel(preferences, levelName, isGlobal(preferences, spaceOrPage), context);
    }

    /**
     * Remove a user or group from rights preferences document for provided level.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param userOrGroup the name of the user or group.
     * @param user indicate if it is a user or group.
     * @param levelName the name of the right level.
     * @param allow indicate if user is removed from allow right or deny right.
     * @param comment the comment to use when saving preferences document.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing rights.
     */
    public void removeUserOrGroupFromLevel(String spaceOrPage, String userOrGroup, boolean user, String levelName,
        boolean allow, String comment, XWikiContext context) throws XWikiException
    {
        XWikiDocument preferences = getXWikiPreferencesDoc(spaceOrPage, context);

        boolean global = isGlobal(preferences, spaceOrPage);

        EntityReference rightClassReference =
            global ? XWikiRightServiceImpl.GLOBALRIGHTCLASS_REFERENCE : XWikiRightServiceImpl.RIGHTCLASS_REFERENCE;

        boolean needUpdate = false;

        List<BaseObject> rightObjects = preferences.getXObjects(rightClassReference);
        if (rightObjects != null) {
            for (BaseObject bobj : rightObjects) {
                List<String> levels =
                    ListClass.getListFromString(bobj.getStringValue(RIGHTSFIELD_LEVELS), RIGHTSLISTFIELD_SEP, false);

                if (levels.contains(levelName)) {
                    needUpdate |= removeUserOrGroupFromRight(bobj, null, null, userOrGroup, user, context);
                }
            }

            if (needUpdate) {
                context.getWiki().saveDocument(preferences, comment, context);
            }
        }
    }

    /**
     * Remove all references to provided user or group from provided right object.
     *
     * @param right the object containing the right preferences.
     * @param userOrGroupWiki the name of the wiki of the use or group.
     * @param userOrGroupSpace the name of the space of the use or group.
     * @param userOrGroupName the name of the use or group.
     * @param user indicate if it is a user or a group.
     * @param context the XWiki context.
     * @return true if user or group has been found and removed.
     */
    public boolean removeUserOrGroupFromRight(BaseObject right, String userOrGroupWiki, String userOrGroupSpace,
        String userOrGroupName, boolean user, XWikiContext context)
    {
        boolean needUpdate = false;

        String userOrGroupField = getUserOrGroupField(user);

        List<String> usersOrGroups =
            ListClass.getListFromString(right.getLargeStringValue(userOrGroupField), USERGROUPLISTFIELD_SEP, false);

        if (userOrGroupWiki != null) {
            needUpdate |= usersOrGroups.remove(userOrGroupWiki + WIKIFULLNAME_SEP + userOrGroupName);
        }

        if (context.getWikiId() == null || context.getWikiId().equalsIgnoreCase(userOrGroupWiki)) {
            needUpdate |= usersOrGroups.remove(userOrGroupName);

            if (userOrGroupSpace == null || userOrGroupSpace.equals(DEFAULT_USERORGROUP_SPACE)) {
                needUpdate |= usersOrGroups.remove(userOrGroupSpace + SPACEPAGENAME_SEP + userOrGroupName);
            }
        }

        if (needUpdate) {
            right.setLargeStringValue(userOrGroupField,
                ListClass.getStringFromList(usersOrGroups, USERGROUPLISTFIELD_SEP));
        }

        return needUpdate;
    }

    private String getUserOrGroupField(boolean user)
    {
        return user ? RIGHTSFIELD_USERS : RIGHTSFIELD_GROUPS;
    }

    /**
     * Remove all references to provided user or group from provided rights document.
     *
     * @param rightsDocument the document containing the rights preferences.
     * @param userOrGroupWiki the name of the wiki of the use or group.
     * @param userOrGroupSpace the name of the space of the use or group.
     * @param userOrGroupName the name of the use or group.
     * @param user indicate if it is a user or a group.
     * @param global indicate if user or group is removed from global or document rights.
     * @param context the XWiki context.
     * @return true if user or group has been found and removed.
     */
    public boolean removeUserOrGroupFromRights(XWikiDocument rightsDocument, String userOrGroupWiki,
        String userOrGroupSpace, String userOrGroupName, boolean user, boolean global, XWikiContext context)
    {
        boolean needUpdate = false;

        EntityReference rightClassReference =
            global ? XWikiRightServiceImpl.GLOBALRIGHTCLASS_REFERENCE : XWikiRightServiceImpl.RIGHTCLASS_REFERENCE;

        List<BaseObject> rightObjects = rightsDocument.getXObjects(rightClassReference);
        if (rightObjects != null) {
            for (BaseObject bobj : rightObjects) {
                if (bobj == null) {
                    continue;
                }
                needUpdate |=
                    removeUserOrGroupFromRight(bobj, userOrGroupWiki, userOrGroupSpace, userOrGroupName, user, context);

                if (needUpdate && bobj.getLargeStringValue(RIGHTSFIELD_USERS).trim().length() == 0
                    && bobj.getLargeStringValue(RIGHTSFIELD_GROUPS).trim().length() == 0) {
                    rightsDocument.removeXObject(bobj);
                }
            }
        }

        return needUpdate;
    }

    /**
     * Remove all references to provided user or group from provided rights document.
     *
     * @param rightsDocument the document containing the rights preferences.
     * @param userOrGroupWiki the name of the wiki of the use or group.
     * @param userOrGroupSpace the name of the space of the use or group.
     * @param userOrGroupName the name of the use or group.
     * @param user indicate if it is a user or a group.
     * @param context the XWiki context.
     * @return true if user or group has been found and removed.
     */
    public boolean removeUserOrGroupFromAllRights(XWikiDocument rightsDocument, String userOrGroupWiki,
        String userOrGroupSpace, String userOrGroupName, boolean user, XWikiContext context)
    {
        return removeUserOrGroupFromRights(rightsDocument, userOrGroupWiki, userOrGroupSpace, userOrGroupName, user,
            true, context)
            || removeUserOrGroupFromRights(rightsDocument, userOrGroupWiki, userOrGroupSpace, userOrGroupName, user,
                false, context);
    }

    /**
     * Remove all references to provided user or group from all rights documents.
     *
     * @param userOrGroupWiki the name of the wiki of the use or group.
     * @param userOrGroupSpace the name of the space of the use or group.
     * @param userOrGroupName the name of the use or group.
     * @param user indicate if it is a user or a group.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing rights.
     */
    public void removeUserOrGroupFromAllRights(String userOrGroupWiki, String userOrGroupSpace, String userOrGroupName,
        boolean user, XWikiContext context) throws XWikiException
    {
        List<String> parameterValues = new ArrayList<>();

        String fieldName;
        if (user) {
            fieldName = RIGHTSFIELD_USERS;
        } else {
            fieldName = RIGHTSFIELD_GROUPS;
        }

        BaseClass rightClass = context.getWiki().getRightsClass(context);
        BaseClass globalRightClass = context.getWiki().getGlobalRightsClass(context);

        String fieldTypeName = ((PropertyClass) rightClass.get(fieldName)).newProperty().getClass().getSimpleName();

        String where = String.format(ALL_RIGHTS_QUERY, fieldTypeName);

        parameterValues.add(rightClass.getName());
        parameterValues.add(globalRightClass.getName());
        parameterValues.add(fieldName);

        if (context.getWikiId() == null || context.getWikiId().equalsIgnoreCase(userOrGroupWiki)) {
            if (userOrGroupSpace == null || userOrGroupSpace.equals(DEFAULT_USERORGROUP_SPACE)) {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + userOrGroupName + HQLLIKE_ALL_SYMBOL);
            } else {
                parameterValues.add(
                    HQLLIKE_ALL_SYMBOL + userOrGroupSpace + SPACEPAGENAME_SEP + userOrGroupName + HQLLIKE_ALL_SYMBOL);
            }
        } else {
            parameterValues
                .add(HQLLIKE_ALL_SYMBOL + userOrGroupWiki + WIKIFULLNAME_SEP + userOrGroupName + HQLLIKE_ALL_SYMBOL);
        }

        List<XWikiDocument> documentList =
            context.getWiki().getStore().searchDocuments(where, parameterValues, context);

        for (XWikiDocument groupDocument : documentList) {
            if (removeUserOrGroupFromAllRights(groupDocument, userOrGroupWiki, userOrGroupSpace, userOrGroupName, user,
                context)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    /**
     * Replace a user or a group reference with another one on a right object.
     * @param right the right to change.
     * @param userOrGroupSourceReference the reference of the user or group that we need to replace
     * @param userOrGroupTargetReference the reference of the user or group that will be used as replacement
     * @param user if {@code true} the reference will be looked in the users properties, else in the groups one
     * @return {@code true} if the right has been changed
     * @since 11.9RC1
     */
    private boolean replaceUserOrGroupFromRight(BaseObject right, DocumentReference userOrGroupSourceReference,
        DocumentReference userOrGroupTargetReference, boolean user)
    {
        boolean needUpdate = false;

        String userOrGroupField = getUserOrGroupField(user);
        List<String> usersOrGroups =
            ListClass.getListFromString(right.getLargeStringValue(userOrGroupField), USERGROUPLISTFIELD_SEP, false);

        String userOrGroupSource = this.compactWikiEntityReferenceSerializer.serialize(userOrGroupSourceReference);
        String userOrGroupTarget = this.compactWikiEntityReferenceSerializer.serialize(userOrGroupTargetReference);

        if (usersOrGroups.remove(userOrGroupSource)) {
            usersOrGroups.add(userOrGroupTarget);
            needUpdate = true;
        }

        if (needUpdate) {
            right.setLargeStringValue(userOrGroupField, ListClass.getStringFromList(usersOrGroups,
                USERGROUPLISTFIELD_SEP));
        }

        return needUpdate;
    }

    /**
     * Replace a user or a group reference with another one on either the global or local rights
     * of a {@link XWikiDocument}.
     *
     * @param rightsDocument the document where to update the rights
     * @param userOrGroupSourceReference the reference of the user or group that we need to replace
     * @param userOrGroupTargetReference the reference of the user or group that will be used as replacement
     * @param user if {@code true} the reference will be looked in the users properties, else in the groups one
     * @param global if {@code true} update the XWikiGlobalRights objects, else update the XWikiRights objects
     * @return {@code true} if some rights have changed
     * @since 11.9RC1
     */
    private boolean replaceUserOrGroupFromRights(XWikiDocument rightsDocument,
        DocumentReference userOrGroupSourceReference, DocumentReference userOrGroupTargetReference, boolean user,
        boolean global)
    {
        boolean needUpdate = false;

        EntityReference rightClassReference =
            global ? XWikiRightServiceImpl.GLOBALRIGHTCLASS_REFERENCE : XWikiRightServiceImpl.RIGHTCLASS_REFERENCE;

        List<BaseObject> rightObjects = rightsDocument.getXObjects(rightClassReference);
        if (rightObjects != null) {
            for (BaseObject bobj : rightObjects) {
                if (bobj == null) {
                    continue;
                }
                needUpdate |=
                    replaceUserOrGroupFromRight(bobj, userOrGroupSourceReference, userOrGroupTargetReference, user);
            }
        }

        return needUpdate;
    }

    /**
     * Replace a user or a group reference with another one on both the local and global
     * rights of a {@link XWikiDocument}.
     *
     * @param rightsDocument the document where to update the rights
     * @param userOrGroupSourceReference the reference of the user or group that we need to replace
     * @param userOrGroupTargetReference the reference of the user or group that will be used as replacement
     * @param user if {@code true} the reference will be looked in the users properties, else in the groups one
     * @return {@code true} if some rights have changed
     * @since 11.9RC1
     */
    private boolean replaceUserOrGroupFromAllRights(XWikiDocument rightsDocument,
        DocumentReference userOrGroupSourceReference, DocumentReference userOrGroupTargetReference, boolean user)
    {
        return replaceUserOrGroupFromRights(rightsDocument, userOrGroupSourceReference, userOrGroupTargetReference,
            user, true)
            || replaceUserOrGroupFromRights(rightsDocument, userOrGroupSourceReference, userOrGroupTargetReference,
            user, false);
    }

    /**
     * Replace a user or a group reference with another one on all rights of the current wiki.
     *
     * @param userOrGroupSourceReference the reference of the user or group that we need to replace
     * @param userOrGroupTargetReference the reference of the user or group that will be used as replacement
     * @param user if {@code true} the reference will be looked in the users properties, else in the groups one
     * @param context the current context
     * @throws XWikiException in case of errors
     * @since 11.9RC1
     */
    public void replaceUserOrGroupFromAllRights(DocumentReference userOrGroupSourceReference,
        DocumentReference userOrGroupTargetReference, boolean user, XWikiContext context) throws XWikiException
    {
        List<String> parameterValues = new ArrayList<>();

        String fieldName;
        if (user) {
            fieldName = RIGHTSFIELD_USERS;
        } else {
            fieldName = RIGHTSFIELD_GROUPS;
        }

        BaseClass rightClass = context.getWiki().getRightsClass(context);
        BaseClass globalRightClass = context.getWiki().getGlobalRightsClass(context);

        String fieldTypeName = ((PropertyClass) rightClass.get(fieldName)).newProperty().getClass().getSimpleName();

        String where = String.format(ALL_RIGHTS_QUERY, fieldTypeName);

        parameterValues.add(this.localEntityReferenceSerializer.serialize(rightClass.getReference()));
        parameterValues.add(this.localEntityReferenceSerializer.serialize(globalRightClass.getReference()));
        parameterValues.add(fieldName);
        parameterValues.add(HQLLIKE_ALL_SYMBOL
            + this.compactWikiEntityReferenceSerializer.serialize(userOrGroupSourceReference)
            + HQLLIKE_ALL_SYMBOL);

        List<XWikiDocument> documentList =
            context.getWiki().getStore().searchDocuments(where, parameterValues, context);

        for (XWikiDocument groupDocument : documentList) {
            if (replaceUserOrGroupFromAllRights(groupDocument, userOrGroupSourceReference, userOrGroupTargetReference,
                user)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    /**
     * Remove "direct" rights for wiki, space or document. This means that after that inherited right will be used.
     *
     * @param spaceOrPage the space of page where to get XWikiRights. If null get wiki rights.
     * @param levelNames the levels names to check ("view", "edit", etc.).
     * @param comment the comment to use when saving preferences document.
     * @param context the XWiki context.
     * @throws XWikiException error when browsing rights.
     */
    public void removeDirectRights(String spaceOrPage, List<String> levelNames, String comment, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument preferences = getXWikiPreferencesDoc(spaceOrPage, context);

        boolean global = isGlobal(preferences, spaceOrPage);

        EntityReference rightClassReference =
            global ? XWikiRightServiceImpl.GLOBALRIGHTCLASS_REFERENCE : XWikiRightServiceImpl.RIGHTCLASS_REFERENCE;

        List<BaseObject> rightObjects = preferences.getXObjects(rightClassReference);
        if (rightObjects != null && !rightObjects.isEmpty()) {
            preferences.removeXObjects(rightClassReference);
            context.getWiki().saveDocument(preferences, comment, context);
        }
    }

    /**
     * Browse a group and groups it contains to find provided member (user or group).
     *
     * @param groupName the group name where to search for member.
     * @param memberName the name of the member to find.
     * @param groupCacheIn a map containing a set a group and its corresponding members already retrieved.
     * @param context the XWiki context.
     * @return true if the member has been found, false otherwise.
     * @throws XWikiException error when browsing groups.
     */
    public boolean groupContainsMember(String groupName, String memberName,
        Map<String, Collection<String>> groupCacheIn, XWikiContext context) throws XWikiException
    {
        boolean found = false;

        Map<String, Collection<String>> groupCache = groupCacheIn;
        if (groupCache == null) {
            groupCache = new HashMap<>();
        }

        Collection<String> memberList = groupCache.get(groupName);

        if (memberList == null) {
            memberList =
                context.getWiki().getGroupService(context).getAllMembersNamesForGroup(groupName, 0, 0, context);
            groupCache.put(groupName, memberList);
        }

        if (memberList.contains(memberName)
            || memberList.contains(context.getWikiId() + WIKIFULLNAME_SEP + memberName)) {
            found = true;
        } else {
            for (String groupMemberName : memberList) {
                if (groupContainsMember(groupMemberName, memberName, groupCache, context)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    /**
     * Resolve passed user or group into users references list.
     *
     * @param userOrGroup the user or group
     * @param context the XWikiContext the XWiki context
     * @return the list of users references
     * @throws XWikiException error when getting documents
     */
    public Collection<DocumentReference> resolveUsers(DocumentReference userOrGroup, XWikiContext context)
        throws XWikiException
    {
        Collection<DocumentReference> userReferences = new LinkedHashSet<>();
        Iterator<DocumentReference> iterator =
            new ReferenceUserIterator(userOrGroup, this.explicitDocumentReferenceResolver, context);
        while (iterator.hasNext()) {
            userReferences.add(iterator.next());
        }
        return userReferences;
    }

    /**
     * Resolve passed user or group into users references list.
     *
     * @param userOrGroup the user or group
     * @param context the XWikiContext the XWiki context
     * @return the list of users references
     * @throws XWikiException error when getting documents
     */
    public Collection<DocumentReference> resolveUsers(String userOrGroup, XWikiContext context) throws XWikiException
    {
        return resolveUsers(this.currentDocumentReferenceResolver.resolve(userOrGroup), context);
    }

    /**
     * Resolve passed users and groups into users references list.
     *
     * @param referenceList the list of users and groups
     * @param context the XWikiContext the XWiki context
     * @return the list of users references
     * @throws XWikiException error when getting documents
     */
    public Collection<DocumentReference> resolveUsers(List<String> referenceList, XWikiContext context)
        throws XWikiException
    {
        Collection<DocumentReference> users = new LinkedHashSet<>();

        for (String reference : referenceList) {
            users.addAll(resolveUsers(reference, context));
        }

        return users;
    }
}
