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

package com.xpn.xwiki.user.impl.xwiki;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

/**
 * Default implementation of {@link XWikiGroupService} users and groups manager.
 * 
 * @version $Id: $
 */
public class XWikiGroupServiceImpl implements XWikiGroupService,
    XWikiDocChangeNotificationInterface
{
    /**
     * Name of the "XWiki.XWikiGroups" class without the space name.
     */
    private static final String CLASS_SUFFIX_XWIKIGROUPS = "XWikiGroups";

    /**
     * Name of the "XWiki.XWikiUsers" class without the space name.
     */
    private static final String CLASS_SUFFIX_XWIKIUSERS = "XWikiUsers";

    /**
     * Name of the "XWiki.XWikiGroups" class without the space name.
     */
    private static final String CLASS_XWIKIGROUPS = "XWiki.XWikiGroups";

    /**
     * Name of the field of class XWiki.XWikiGroups where group's members names are inserted.
     */
    private static final String FIELD_XWIKIGROUPS_MEMBER = "member";

    /**
     * Separator between members names in XWiki.XWikiGroups class "member" field.
     */
    private static final String FIELD_XWIKIGROUPS_MEMBER_SEP = " ,";

    /**
     * Default space name for a group or a user.
     */
    private static final String DEFAULT_MEMBER_SPACE = "XWiki";

    /**
     * String between wiki name and full name in document path.
     */
    private static final String WIKI_FULLNAME_SEP = ":";

    /**
     * String between space and name in document full name.
     */
    private static final String SAME_NAME_SEP = ".";

    /**
     * Symbol use in HQL "like" command that means "all characters".
     */
    private static final String HQLLIKE_ALL_SYMBOL = "%";

    protected XWikiCache groupCache;

    public synchronized void init(XWiki xwiki, XWikiContext context) throws XWikiException
    {
        initCache(context);
        xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(this));
    }

    public synchronized void initCache(XWikiContext context) throws XWikiException
    {
        int iCapacity = 100;
        try {
            String capacity =
                context.getWiki().Param("xwiki.authentication.group.cache.capacity");
            if (capacity != null)
                iCapacity = Integer.parseInt(capacity);
        } catch (Exception e) {
        }
        initCache(iCapacity, context);
    }

    public synchronized void initCache(int iCapacity, XWikiContext context) throws XWikiException
    {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        groupCache = cacheService.newCache("xwiki.authentication.group.cache", iCapacity);
    }

    public void flushCache()
    {
        if (groupCache != null) {
            groupCache.flushAll();
            groupCache = null;
        }
    }

    public Collection listGroupsForUser(String username, XWikiContext context)
        throws XWikiException
    {
        List list = null;
        String database = context.getDatabase();
        try {
            String shortname = Util.getName(username);
            String veryshortname = shortname.substring(shortname.indexOf(".") + 1);
            String key = database + ":" + shortname;
            synchronized (key) {
                if (groupCache == null)
                    initCache(context);
                try {
                    list = (List) groupCache.getFromCache(key);
                } catch (XWikiCacheNeedsRefreshException e) {
                    groupCache.cancelUpdate(key);

                    if (context.getWiki().getNotCacheStore() instanceof XWikiHibernateStore) {
                        list =
                            context.getWiki().getStore()
                                .searchDocumentsNames(
                                    ", BaseObject as obj, StringProperty as prop "
                                        + "where obj.name=" + context.getWiki().getFullNameSQL()
                                        + " and obj.className='XWiki.XWikiGroups' "
                                        + "and obj.id = prop.id.id and prop.id.name='member' "
                                        + "and (prop.value='" + Utils.SQLFilter(username)
                                        + "' or prop.value='" + Utils.SQLFilter(shortname)
                                        + "' or prop.value='" + Utils.SQLFilter(veryshortname)
                                        + "')", context);
                    } else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
                        list =
                            ((XWikiJcrStore) context.getWiki().getNotCacheStore())
                                .listGroupsForUser(username, context);
                    }
                    groupCache.putInCache(key, list);
                }
            }
            return list;
        } finally {
            context.setDatabase(database);
        }
    }

    /*
     * Adding the user to the group cache
     */
    public void addUserToGroup(String username, String database, String group,
        XWikiContext context) throws XWikiException
    {
        String shortname = Util.getName(username);
        List list = null;
        String key = database + ":" + shortname;
        if (groupCache == null)
            initCache(context);
        synchronized (key) {
            try {
                list = (List) groupCache.getFromCache(key);
            } catch (XWikiCacheNeedsRefreshException e) {
                groupCache.cancelUpdate(key);
                list = new ArrayList();
                groupCache.putInCache(key, list);
            }
        }
        list.add(group);
    }

    /**
     * Remove a user or group name from a list of string used in XWiki.XWikiGroups class "member"
     * field.
     * 
     * @param list the list of members names where to find the one to remove.
     * @param memberWiki the name of the wiki of the member.
     * @param memberSpace the name of the space of the member.
     * @param memberName the name of the member.
     * @param context the XWiki context.
     * @return true if at least one member has been removed from the list.
     */
    private boolean removeMemberFromList(List list, String memberWiki, String memberSpace,
        String memberName, XWikiContext context)
    {
        boolean needUpdate = false;

        if (memberWiki != null) {
            needUpdate |= list.remove(memberWiki + WIKI_FULLNAME_SEP + memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                needUpdate |= list.remove(memberSpace + SAME_NAME_SEP + memberName);
            }
        }

        if (context.getDatabase() == null || context.getDatabase().equalsIgnoreCase(memberWiki)) {
            needUpdate |= list.remove(memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                needUpdate |= list.remove(memberSpace + SAME_NAME_SEP + memberName);
            }
        }

        return needUpdate;
    }

    /**
     * Remove user or group name from a group {@link XWikiDocument}.
     * 
     * @param groupDocument the {@link XWikiDocument} containing group object.
     * @param memberWiki the name of the wiki of the member.
     * @param memberSpace the name of the space of the member.
     * @param memberName the name of the member.
     * @param context the XWiki context.
     * @return true if at least one member has been removed from the list.
     */
    private boolean removeUserOrGroupFromGroup(XWikiDocument groupDocument, String memberWiki,
        String memberSpace, String memberName, XWikiContext context)
    {
        boolean needUpdate = false;

        Vector groupVector = groupDocument.getObjects(CLASS_XWIKIGROUPS);

        if (groupVector != null) {
            for (Iterator itGroups = groupVector.iterator(); itGroups.hasNext();) {
                BaseObject bobj = (BaseObject) itGroups.next();
                if (bobj != null) {
                    String members = bobj.getStringValue(FIELD_XWIKIGROUPS_MEMBER);
                    List memberList =
                        ListClass.getListFromString(members, FIELD_XWIKIGROUPS_MEMBER_SEP, false);

                    needUpdate |=
                        removeMemberFromList(memberList, memberWiki, memberSpace, memberName,
                            context);

                    if (needUpdate) {
                        if (memberList.size() > 0) {
                            bobj.setStringValue(FIELD_XWIKIGROUPS_MEMBER, StringUtils.join(
                                memberList.toArray(), FIELD_XWIKIGROUPS_MEMBER_SEP.charAt(0)));
                        } else {
                            groupDocument.removeObject(bobj);
                        }
                    }
                }
            }
        }

        return needUpdate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#removeUserOrGroupFromAllGroups(java.lang.String,
     *      java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void removeUserOrGroupFromAllGroups(String memberWiki, String memberSpace,
        String memberName, XWikiContext context) throws XWikiException
    {
        List parameterValues = new ArrayList();

        StringBuffer where =
            new StringBuffer(", BaseObject as obj, StringProperty as prop where doc.fullName=obj.name and obj.className='"
                + CLASS_XWIKIGROUPS + "'");

        where.append(" and obj.id=prop.id.id");

        where.append(" and prop.name=?");
        parameterValues.add(FIELD_XWIKIGROUPS_MEMBER);

        where.append(" and prop.value like ?");

        if (context.getDatabase() == null || context.getDatabase().equalsIgnoreCase(memberWiki)) {
            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + memberName + HQLLIKE_ALL_SYMBOL);
            } else {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + memberSpace + SAME_NAME_SEP + memberName
                    + HQLLIKE_ALL_SYMBOL);
            }
        } else {
            parameterValues.add(HQLLIKE_ALL_SYMBOL + memberWiki + WIKI_FULLNAME_SEP + memberName
                + HQLLIKE_ALL_SYMBOL);
        }

        List documentList =
            context.getWiki().getStore().searchDocuments(where.toString(), parameterValues,
                context);

        for (Iterator it = documentList.iterator(); it.hasNext();) {
            XWikiDocument groupDocument = (XWikiDocument) it.next();
            if (removeUserOrGroupFromGroup(groupDocument, memberWiki, memberSpace, memberName,
                context)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    public List listMemberForGroup(String group, XWikiContext context) throws XWikiException
    {
        List list = new ArrayList();
        String database = context.getDatabase();
        String sql = "";

        try {
            if (group == null) {
                if (context.getWiki().getHibernateStore() != null) {
                    sql =
                        ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.XWikiUsers'";
                    return context.getWiki().getStore().searchDocumentsNames(sql, context);
                } else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
                    String xpath = "/*/*/obj/XWiki/XWikiUsers/jcr:deref(@doc, '*')/@fullName";
                    QueryPlugin qp = (QueryPlugin) context.getWiki().getPlugin("query", context);
                    return qp.xpath(xpath).list();
                } else
                    return list;
            } else {
                String gshortname = Util.getName(group, context);
                XWikiDocument docgroup = context.getWiki().getDocument(gshortname, context);

                Vector groups = docgroup.getObjects("XWiki.XWikiGroups");
                if (groups != null) {
                    for (Iterator itGroups = groups.iterator(); itGroups.hasNext();) {
                        BaseObject bobj = (BaseObject) itGroups.next();
                        if (bobj != null) {
                            String members = bobj.getStringValue(FIELD_XWIKIGROUPS_MEMBER);
                            list.addAll(ListClass.getListFromString(members, " ,", false));
                        }
                    }
                }

                return list;
            }
        } catch (XWikiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            context.setDatabase(database);
        }
        return null;
    }

    public List listAllGroups(XWikiContext context) throws XWikiException
    {
        if (context.getWiki().getHibernateStore() != null) {
            String sql =
                ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.XWikiGroups'";
            return context.getWiki().getStore().searchDocumentsNames(sql, context);
        } else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
            String xpath = "/*/*/obj/XWiki/XWikiGroups/jcr:deref(@doc, '*')/@fullName";
            QueryPlugin qp = (QueryPlugin) context.getWiki().getPlugin("query", context);
            return qp.xpath(xpath).list();
        } else
            return null;
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc,
        int event, XWikiContext context)
    {
        try {
            if (event == XWikiNotificationInterface.EVENT_CHANGE) {
                boolean flushCache = false;

                if ((olddoc != null) && (olddoc.getObjects("XWiki.XWikiGroups") != null))
                    flushCache = true;

                if ((newdoc != null) && (newdoc.getObjects("XWiki.XWikiGroups") != null))
                    flushCache = true;

                if (flushCache)
                    flushCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMatchedUsers(java.lang.Object[][],
     *      boolean, int, int, java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public List getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsersOrGroups(true, matchFields, withdetails, nb, start, order,
            context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMatchedGroups(java.lang.Object[][],
     *      boolean, int, int, java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public List getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsersOrGroups(false, matchFields, withdetails, nb, start, order,
            context);
    }

    /**
     * Create a "where clause" to use with {@link XWikiStoreInterface} searchDocuments and
     * searchDocumentsNames methods.
     * 
     * @param documentClass a filter to search only document containing this XWiki class.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as
     *            document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>.
     * @param order the field to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as
     *            document field</li>
     *            </ul>
     * @param parameterValues the list of values to fill for use with HQL named request.
     * @return the formated HQL named request.
     */
    protected String createWhereClause(String documentClass, Object[][] matchFields,
        Object[][] order, List parameterValues)
    {
        StringBuffer from = new StringBuffer(", BaseObject as obj");

        StringBuffer where =
            new StringBuffer(" where doc.fullName=obj.name and obj.className='XWiki."
                + documentClass + "'");

        Map fieldMap = new HashMap();
        int fieldIndex = 0;

        // Manage object match strings
        if (matchFields != null) {
            for (; fieldIndex < matchFields.length; ++fieldIndex) {
                String fieldName = (String) matchFields[fieldIndex][0];
                String type = (String) matchFields[fieldIndex][1];
                String value = (String) matchFields[fieldIndex][2];

                if (type != null) {
                    String fieldPrefix;

                    if (!fieldMap.containsKey(fieldName)) {
                        fieldPrefix = "field" + fieldIndex;
                        from.append(", " + type + " as " + fieldPrefix);

                        where.append(" and obj.id=" + fieldPrefix + ".id.id");
                        where.append(" and " + fieldPrefix + ".name=?");
                        parameterValues.add(fieldName);
                        ++fieldIndex;
                    } else {
                        fieldPrefix = (String) fieldMap.get(fieldName);
                    }

                    where.append(" and lower(" + fieldPrefix + ".value) like ?");
                    parameterValues.add(value.toLowerCase() + HQLLIKE_ALL_SYMBOL);

                    fieldMap.put(fieldName, fieldPrefix);
                } else {
                    where.append(" and lower(doc." + fieldName + ") like ?");
                    parameterValues.add(value.toLowerCase() + HQLLIKE_ALL_SYMBOL);
                }
            }
        }

        StringBuffer orderString = new StringBuffer();

        // Manage order
        if (order != null && order.length > 0) {
            orderString.append(" order by");

            for (int i = 0; i < order.length; ++i) {
                String fieldName = (String) order[i][0];
                String type = (String) order[i][1];
                Boolean asc = (Boolean) order[i][2];

                if (i > 0)
                    orderString.append(",");

                if (type != null) {
                    String fieldPrefix;

                    if (!fieldMap.containsKey(fieldName)) {
                        fieldPrefix = "field" + fieldIndex;
                        from.append(", " + type + " as " + fieldPrefix);

                        where.append(" and obj.id=" + fieldPrefix + ".id.id");
                        where.append(" and " + fieldPrefix + ".name=?");
                        parameterValues.add(fieldName);
                        ++fieldIndex;
                    } else {
                        fieldPrefix = (String) fieldMap.get(fieldName);
                    }

                    orderString.append(" " + fieldPrefix + ".value");
                } else {
                    orderString.append(" doc." + fieldName);
                }

                orderString.append(asc == null || asc.booleanValue() ? " asc" : " desc");
            }
        }

        return from.append(where).append(orderString).toString();
    }

    /**
     * Search for all users or group with provided constraints and in a provided order.
     * 
     * @param user if true search for users, otherwise search for groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as
     *            document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>.
     * @param withdetails indicate if a {@link List} containing {@link String} names is returned or
     *            {@link List} containing {@link XWikiDocument}.
     * @param nb the maximum number od result to return.
     * @param start the index of the first found user or group to return.
     * @param order the field to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as
     *            document field</li>
     *            </ul>
     * @param context the {@link XWikiContext}.
     * @return the list of users or groups.
     * @throws XWikiException error when calling for
     *             {@link XWikiStoreInterface#searchDocuments(String, int, int, List, XWikiContext)}
     *             or
     *             {@link XWikiStoreInterface#searchDocumentsNames(String, int, int, List, XWikiContext)}
     */
    protected List getAllMatchedUsersOrGroups(boolean user, Object[][] matchFields,
        boolean withdetails, int nb, int start, Object[][] order, XWikiContext context)
        throws XWikiException
    {
        String documentClass = user ? CLASS_SUFFIX_XWIKIUSERS : CLASS_SUFFIX_XWIKIGROUPS;

        if (context.getWiki().getHibernateStore() != null) {
            List parameterValues = new ArrayList();
            String where = createWhereClause(documentClass, matchFields, order, parameterValues);

            if (withdetails)
                return context.getWiki().getStore().searchDocuments(where, nb, start,
                    parameterValues, context);
            else
                return context.getWiki().getStore().searchDocumentsNames(where, nb, start,
                    parameterValues, context);
        } else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
            // TODO : fully implement this methods for XPATH platform

            if ((matchFields != null && matchFields.length > 0) || withdetails) {
                throw new NotImplementedException();
            }

            List list = listAllGroups(context);

            if (nb > 0 || start > 0) {
                int fromIndex = start < 0 ? 0 : start;
                int toIndex = fromIndex + (nb <= 0 ? list.size() - 1 : nb);

                list = list.subList(fromIndex, toIndex);
            }

            return list;
        } else
            return null;
    }

    /**
     * Return number of users or groups with provided constraints.
     * 
     * @param user if true search for users, otherwise search for groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as
     *            document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user or group to return.
     * @param context the {@link XWikiContext}.
     * @return the of found users or groups.
     * @throws XWikiException error when calling for
     *             {@link XWikiStoreInterface#search(String, int, int, List, XWikiContext)}
     */
    protected int countAllMatchedUsersOrGroups(boolean user, Object[][] matchFields,
        XWikiContext context) throws XWikiException
    {
        String documentClass = user ? CLASS_SUFFIX_XWIKIUSERS : CLASS_SUFFIX_XWIKIGROUPS;

        List parameterValues = new ArrayList();
        String where = createWhereClause(documentClass, matchFields, null, parameterValues);

        String sql = "select count(doc) from XWikiDocument doc" + where;

        List list = context.getWiki().getStore().search(sql, 0, 0, parameterValues, context);

        if (list == null || list.size() == 0)
            return 0;

        return ((Integer) list.get(0)).intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMatchedUsers(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMatchedUsers(Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        return countAllMatchedUsersOrGroups(true, matchFields, context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMatchedGroups(java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public int countAllMatchedGroups(Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        return countAllMatchedUsersOrGroups(false, matchFields, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllGroupsNamesForMember(java.lang.String,
     *      int, int, com.xpn.xwiki.XWikiContext)
     */
    public Collection getAllGroupsNamesForMember(String member, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        // TODO: improve using real request
        List groupNameList = new ArrayList(listGroupsForUser(member, context));

        if (start <= 0 & (nb <= 0 || nb >= groupNameList.size()))
            return groupNameList;

        if (nb == 0 && start == 0)
            return groupNameList;

        if (start + nb > groupNameList.size())
            return groupNameList.subList(start, groupNameList.size());
        else
            return groupNameList.subList(start, start + nb);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMembersNamesForGroup(java.lang.String,
     *      int, int, com.xpn.xwiki.XWikiContext)
     */
    public Collection getAllMembersNamesForGroup(String group, int nb, int start,
        XWikiContext context) throws XWikiException
    {
        // TODO: improve using real request
        List userNameList = listMemberForGroup(group, context);

        if (nb == 0 && start == 0)
            return userNameList;

        if (start + nb > userNameList.size())
            return userNameList.subList(start, userNameList.size());
        else
            return userNameList.subList(start, start + nb);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllGroupsNamesForMember(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllGroupsNamesForMember(String member, XWikiContext context)
        throws XWikiException
    {
        if (member == null)
            return countAllMatchedGroups(null, context);

        // TODO: improve using real request
        return getAllGroupsNamesForMember(member, 0, 0, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMembersNamesForGroup(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMembersNamesForGroup(String group, XWikiContext context)
        throws XWikiException
    {
        if (group == null)
            return countAllMatchedUsers(null, context);
        
        // TODO: improve using real request
        return getAllMembersNamesForGroup(group, 0, 0, context).size();
    }
}
