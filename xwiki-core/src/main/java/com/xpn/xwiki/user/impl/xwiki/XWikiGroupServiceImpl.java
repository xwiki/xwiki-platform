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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation of {@link XWikiGroupService} users and groups manager.
 * 
 * @version $Id$
 */
public class XWikiGroupServiceImpl implements XWikiGroupService, EventListener
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
     * Name of the "XWiki.XWikiGroups" class.
     */
    private static final String CLASS_XWIKIGROUPS = "XWiki." + CLASS_SUFFIX_XWIKIGROUPS;

    /**
     * Name of the "XWiki.XWikiGroupTemplate" class sheet.
     */
    private static final String CLASSTEMPLATE_XWIKIGROUPS = "XWiki.XWikiGroupTemplate";

    /**
     * Name of the "XWiki.XWikiUserTemplate" class sheet.
     */
    private static final String CLASSTEMPLATE_XWIKIUSERS = "XWiki.XWikiUserTemplate";

    /**
     * Name of the field of class XWiki.XWikiGroups where group's members names are inserted.
     */
    private static final String FIELD_XWIKIGROUPS_MEMBER = "member";

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

    private static final String NAME = "groupservice";

    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
            add(new DocumentDeletedEvent());
        }
    };

    protected Cache<List<DocumentReference>> memberGroupsCache;

    /**
     * Used to convert a string into a proper Document Reference.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "currentmixed");

    private EntityReferenceSerializer<String> entityReferenceSerializer = Utils
        .getComponent(EntityReferenceSerializer.class);

    private EntityReferenceSerializer<String> localWikiEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "local");

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#init(com.xpn.xwiki.XWiki, com.xpn.xwiki.XWikiContext)
     */
    public synchronized void init(XWiki xwiki, XWikiContext context) throws XWikiException
    {
        initCache(context);

        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#initCache(com.xpn.xwiki.XWikiContext)
     */
    public synchronized void initCache(XWikiContext context) throws XWikiException
    {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.authentication.group.cache.capacity");
            if (capacity != null) {
                iCapacity = Integer.parseInt(capacity);
            }
        } catch (Exception e) {
        }
        initCache(iCapacity, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#initCache(int, com.xpn.xwiki.XWikiContext)
     */
    public synchronized void initCache(int iCapacity, XWikiContext context) throws XWikiException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration();
            configuration.setConfigurationId("xwiki.groupservice.usergroups");
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setMaxEntries(iCapacity);
            configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

            this.memberGroupsCache = Utils.getComponent(CacheManager.class).createNewCache(configuration);
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to initialize cache", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#flushCache()
     */
    public void flushCache()
    {
        if (this.memberGroupsCache != null) {
            this.memberGroupsCache.removeAll();
        }
    }

    /**
     * Check whether the configuration specifies that every user is implicitly in XWikiAllGroup. Configured by the
     * {@code xwiki.authentication.group.allgroupimplicit} parameter in {@code xwiki.cfg}.
     * 
     * @param context the current XWiki context
     * @return {@code true} if the group is implicit and all users should be by default in it, {@code false} if the
     *         group behaves as all other groups, containing only the users/subgroups that are explicitly listed inside
     *         the document.
     */
    protected boolean isAllGroupImplicit(XWikiContext context)
    {
        long implicit = context.getWiki().ParamAsLong("xwiki.authentication.group.allgroupimplicit", 0);
        return (implicit == 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#listGroupsForUser(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public Collection<String> listGroupsForUser(String member, XWikiContext context) throws XWikiException
    {
        return getAllGroupsNamesForMember(member, -1, 0, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#addUserToGroup(java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void addUserToGroup(String username, String database, String group, XWikiContext context)
        throws XWikiException
    {
        String shortname = Util.getName(username);
        String key = database + "/" + database + ":" + shortname;

        if (this.memberGroupsCache == null) {
            initCache(context);
        }

        List<DocumentReference> list = this.memberGroupsCache.get(key);

        if (list == null) {
            list = new ArrayList<DocumentReference>();
            this.memberGroupsCache.set(key, list);
        }

        list.add(this.currentMixedDocumentReferenceResolver.resolve(group, new WikiReference(database)));
    }

    /**
     * Check if provided member is equal to member name found in XWiki.XWikiGroups object.
     * 
     * @param currentMember the member name found in XWiki.XWikiGroups object.
     * @param memberWiki the name of the wiki of the member.
     * @param memberSpace the name of the space of the member.
     * @param memberName the name of the member.
     * @param context the XWiki context.
     * @return true if equals, false if not.
     */
    private boolean isMemberEquals(String currentMember, String memberWiki, String memberSpace, String memberName,
        XWikiContext context)
    {
        boolean equals = false;

        if (memberWiki != null) {
            equals |= currentMember.equals(memberWiki + WIKI_FULLNAME_SEP + memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                equals |= currentMember.equals(memberSpace + SAME_NAME_SEP + memberName);
            }
        }

        if (context.getDatabase() == null || context.getDatabase().equalsIgnoreCase(memberWiki)) {
            equals |= currentMember.equals(memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                equals |= currentMember.equals(memberSpace + SAME_NAME_SEP + memberName);
            }
        }

        return equals;
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
    private boolean removeUserOrGroupFromGroup(XWikiDocument groupDocument, String memberWiki, String memberSpace,
        String memberName, XWikiContext context)
    {
        boolean needUpdate = false;

        Vector<BaseObject> groupVector = groupDocument.getObjects(CLASS_XWIKIGROUPS);

        if (groupVector != null) {
            for (BaseObject bobj : groupVector) {
                if (bobj != null) {
                    String member = bobj.getStringValue(FIELD_XWIKIGROUPS_MEMBER);

                    if (isMemberEquals(member, memberWiki, memberSpace, memberName, context)) {
                        needUpdate = groupDocument.removeXObject(bobj);
                    }
                }
            }
        }

        return needUpdate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#removeUserOrGroupFromAllGroups(java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void removeUserOrGroupFromAllGroups(String memberWiki, String memberSpace, String memberName,
        XWikiContext context) throws XWikiException
    {
        List<Object> parameterValues = new ArrayList<Object>();

        StringBuffer where =
            new StringBuffer(
                ", BaseObject as obj, StringProperty as prop where doc.fullName=obj.name and obj.className=?");
        parameterValues.add(CLASS_XWIKIGROUPS);

        where.append(" and obj.id=prop.id.id");

        where.append(" and prop.name=?");
        parameterValues.add(FIELD_XWIKIGROUPS_MEMBER);

        where.append(" and prop.value like ?");

        if (context.getDatabase() == null || context.getDatabase().equalsIgnoreCase(memberWiki)) {
            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + memberName + HQLLIKE_ALL_SYMBOL);
            } else {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + memberSpace + SAME_NAME_SEP + memberName + HQLLIKE_ALL_SYMBOL);
            }
        } else {
            parameterValues.add(HQLLIKE_ALL_SYMBOL + memberWiki + WIKI_FULLNAME_SEP + memberName + HQLLIKE_ALL_SYMBOL);
        }

        List<XWikiDocument> documentList =
            context.getWiki().getStore().searchDocuments(where.toString(), parameterValues, context);

        for (XWikiDocument groupDocument : documentList) {
            if (removeUserOrGroupFromGroup(groupDocument, memberWiki, memberSpace, memberName, context)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#listMemberForGroup(java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public List<String> listMemberForGroup(String group, XWikiContext context) throws XWikiException
    {
        List<String> list = new ArrayList<String>();

        try {
            if (group == null) {
                try {
                    return context.getWiki().getStore().getQueryManager().getNamedQuery("getAllUsers").execute();
                } catch (QueryException ex) {
                    throw new XWikiException(0, 0, ex.getMessage(), ex);
                }
            } else {
                String gshortname = Util.getName(group, context);
                XWikiDocument docgroup = context.getWiki().getDocument(gshortname, context);

                Vector<BaseObject> groups = docgroup.getObjects("XWiki.XWikiGroups");
                if (groups != null) {
                    for (BaseObject bobj : groups) {
                        if (bobj != null) {
                            String member = bobj.getStringValue(FIELD_XWIKIGROUPS_MEMBER);
                            if (StringUtils.isNotEmpty(member)) {
                                list.add(member);
                            }
                        }
                    }
                }

                return list;
            }
        } catch (XWikiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#listAllGroups(com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public List<String> listAllGroups(XWikiContext context) throws XWikiException
    {
        if (context.getWiki().getHibernateStore() != null) {
            String sql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.XWikiGroups'";
            return context.getWiki().getStore().searchDocumentsNames(sql, context);
        } else {
            return null;
        }
    }

    /**
     * currentMixedDocumentReferenceResolver {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiDocument oldDocument = document.getOriginalDocument();

        // if there is any chance some group changed, flush the group cache
        if (document.getObject(CLASS_XWIKIGROUPS) != null || oldDocument.getObject(CLASS_XWIKIGROUPS) != null) {
            flushCache();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMatchedUsers(java.lang.Object[][], boolean, int, int,
     *      java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public List< ? > getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb, int start,
        Object[][] order, XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsersOrGroups(true, matchFields, withdetails, nb, start, order, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMatchedGroups(java.lang.Object[][], boolean, int, int,
     *      java.lang.Object[][], com.xpn.xwiki.XWikiContext)
     */
    public List< ? > getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb, int start,
        Object[][] order, XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsersOrGroups(false, matchFields, withdetails, nb, start, order, context);
    }

    /**
     * Create a "where clause" to use with {@link XWikiStoreInterface} searchDocuments and searchDocumentsNames methods.
     * 
     * @param user if true search for users, otherwise search for groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param order the field to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>asc : a Boolean, if true the order is ascendent</li>
     *            </ul>
     * @param parameterValues the list of values to fill for use with HQL named request.
     * @return the formated HQL named request.
     */
    protected String createMatchUserOrGroupWhereClause(boolean user, Object[][] matchFields, Object[][] order,
        List<Object> parameterValues)
    {
        String documentClass = user ? CLASS_SUFFIX_XWIKIUSERS : CLASS_SUFFIX_XWIKIGROUPS;
        String classtemplate = user ? CLASSTEMPLATE_XWIKIUSERS : CLASSTEMPLATE_XWIKIGROUPS;

        StringBuffer from = new StringBuffer(", BaseObject as obj");

        StringBuffer where = new StringBuffer(" where doc.fullName=obj.name and doc.fullName<>? and obj.className=?");
        parameterValues.add(classtemplate);
        parameterValues.add("XWiki." + documentClass);

        Map<String, String> fieldMap = new HashMap<String, String>();
        int fieldIndex = 0;

        // Manage object match strings
        if (matchFields != null) {
            for (int i = 0; i < matchFields.length; ++i) {
                String fieldName = (String) matchFields[i][0];
                String type = (String) matchFields[i][1];
                String value = (String) matchFields[i][2];

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
                        fieldPrefix = fieldMap.get(fieldName);
                    }

                    where.append(" and lower(" + fieldPrefix + ".value) like ?");
                    parameterValues.add(HQLLIKE_ALL_SYMBOL + value.toLowerCase() + HQLLIKE_ALL_SYMBOL);

                    fieldMap.put(fieldName, fieldPrefix);
                } else {
                    where.append(" and lower(doc." + fieldName + ") like ?");
                    parameterValues.add(HQLLIKE_ALL_SYMBOL + value.toLowerCase() + HQLLIKE_ALL_SYMBOL);
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

                if (i > 0) {
                    orderString.append(",");
                }

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
                        fieldPrefix = fieldMap.get(fieldName);
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
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param withdetails indicate if a {@link List} containing {@link String} names is returned or {@link List}
     *            containing {@link XWikiDocument}.
     * @param nb the maximum number of results to return. Infinite if 0.
     * @param start the index of the first found user or group to return.
     * @param order the field to order from. It is a table of table with :
     *            <ul>
     *            <li>fieldname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            </ul>
     * @param context the {@link XWikiContext}.
     * @return the list of users or groups.
     * @throws XWikiException error when calling for
     *             {@link XWikiStoreInterface#searchDocuments(String, int, int, List, XWikiContext)} or
     *             {@link XWikiStoreInterface#searchDocumentsNames(String, int, int, List, XWikiContext)}
     */
    protected List< ? > getAllMatchedUsersOrGroups(boolean user, Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        List< ? > groups = null;

        if (context.getWiki().getHibernateStore() != null) {
            List<Object> parameterValues = new ArrayList<Object>();
            String where = createMatchUserOrGroupWhereClause(user, matchFields, order, parameterValues);

            if (withdetails) {
                groups =
                    context.getWiki().getStore().searchDocuments(where, false, nb, start, parameterValues, context);
            } else {
                groups = context.getWiki().getStore().searchDocumentsNames(where, nb, start, parameterValues, context);
            }
        } else if (context.getWiki().getStore().getQueryManager().hasLanguage(Query.XPATH)) {
            // TODO : fully implement this methods for XPATH platform
            if ((matchFields != null && matchFields.length > 0) || withdetails) {
                throw new NotImplementedException();
            }

            try {
                groups =
                    context
                        .getWiki()
                        .getStore()
                        .getQueryManager()
                        .createQuery(
                            "/*/*[obj/XWiki/" + (user ? CLASS_SUFFIX_XWIKIUSERS : CLASS_SUFFIX_XWIKIGROUPS)
                                + "]/@fullName", Query.XPATH).setLimit(nb).setOffset(start).execute();
            } catch (QueryException ex) {
                throw new XWikiException(0, 0, ex.getMessage(), ex);
            }
        }

        return groups;
    }

    /**
     * Return number of users or groups with provided constraints.
     * 
     * @param user if true search for users, otherwise search for groups.
     * @param matchFields the field to math with values. It is a table of table with :
     *            <ul>
     *            <li>fiedname : the name of the field</li>
     *            <li>fieldtype : for example StringProperty. If null the field is considered as document field</li>
     *            <li>pattern matching : based on HQL "like" command</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user or group to return.
     * @param context the {@link XWikiContext}.
     * @return the of found users or groups.
     * @throws XWikiException error when calling for
     *             {@link XWikiStoreInterface#search(String, int, int, List, XWikiContext)}
     */
    protected int countAllMatchedUsersOrGroups(boolean user, Object[][] matchFields, XWikiContext context)
        throws XWikiException
    {
        List<Object> parameterValues = new ArrayList<Object>();
        String where = createMatchUserOrGroupWhereClause(user, matchFields, null, parameterValues);

        String sql = "select count(distinct doc) from XWikiDocument doc" + where;

        List< ? > list = context.getWiki().getStore().search(sql, 0, 0, parameterValues, context);

        if (list == null || list.size() == 0) {
            return 0;
        }

        return ((Number) list.get(0)).intValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMatchedUsers(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMatchedUsers(Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        return countAllMatchedUsersOrGroups(true, matchFields, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMatchedGroups(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMatchedGroups(Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        return countAllMatchedUsersOrGroups(false, matchFields, context);
    }

    // ///////////////////////////////////////////////////////////////////
    // Group members and member groups

    /**
     * Create a query to filter provided group members. Generate the entire HQL query except the select part.
     * 
     * @param groupFullName the fill wiki name of the group.
     * @param matchField a string to search in result to filter.
     * @param orderAsc if true, the result is ordered ascendent, if false it descendant. If null no order is applied.
     * @param parameterValues the values to insert in names query.
     * @return the HQL query.
     * @since 1.6M1
     */
    protected String createMatchGroupMembersWhereClause(String groupFullName, String matchField, Boolean orderAsc,
        Map<String, Object> parameterValues)
    {
        StringBuffer queryString = new StringBuffer();

        // Add from clause
        queryString.append(" FROM BaseObject as obj, StringProperty as field");

        // Add where clause
        queryString.append(" WHERE obj.name=:groupdocname "
            + "and obj.className=:groupclassname and obj.id=field.id.id");
        parameterValues.put("groupdocname", groupFullName);
        parameterValues.put("groupclassname", CLASS_XWIKIGROUPS);

        queryString.append(" and (trim(both from field.value)<>'' or "
            + "(trim(both from field.value) is not null and '' is null))");

        if (matchField != null) {
            queryString.append(" and lower(field.value) like :matchfield");
            parameterValues.put("matchfield", HQLLIKE_ALL_SYMBOL + matchField.toLowerCase() + HQLLIKE_ALL_SYMBOL);
        }

        // Add order by clause
        if (orderAsc != null) {
            queryString.append(" ORDER BY field.value ").append(orderAsc ? "asc" : "desc");
        }

        return queryString.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllGroupsNamesForMember(java.lang.String, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Collection<String> getAllGroupsNamesForMember(String member, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        List<String> groupNames = null;

        DocumentReference memberReference = this.currentMixedDocumentReferenceResolver.resolve(member);

        String currentWiki = context.getDatabase();
        try {
            context.setDatabase(memberReference.getWikiReference().getName());

            Collection<DocumentReference> groupReferences =
                getAllGroupsReferencesForMember(memberReference, nb, start, context);

            groupNames = new ArrayList<String>(groupReferences.size());
            for (DocumentReference groupReference : groupReferences) {
                groupNames.add(this.localWikiEntityReferenceSerializer.serialize(groupReference));
            }
        } finally {
            context.setDatabase(currentWiki);
        }

        return groupNames;
    }

    public Collection<DocumentReference> getAllGroupsReferencesForMember(DocumentReference memberReference, int limit,
        int offset, XWikiContext context) throws XWikiException
    {
        List<DocumentReference> groupReferences = null;

        String prefixedFullName = this.entityReferenceSerializer.serialize(memberReference);

        String key = context.getDatabase() + "/" + prefixedFullName;
        synchronized (key) {
            if (this.memberGroupsCache == null) {
                initCache(context);
            }

            // TODO: add cache support for customized limit/offset ?
            boolean supportCache = limit <= 0 && offset <= 0;

            if (supportCache) {
                groupReferences = this.memberGroupsCache.get(key);
            }

            if (groupReferences == null) {
                List<String> groupNames;
                try {
                    Query query;
                    if (memberReference.getWikiReference().getName().equals(context.getDatabase())
                        || (memberReference.getLastSpaceReference().getName().equals("XWiki") && memberReference
                            .getName().equals(XWikiRightService.GUEST_USER))) {
                        query =
                            context
                                .getWiki()
                                .getStore()
                                .getQueryManager()
                                .getNamedQuery("listGroupsForUser")
                                .bindValue("username", prefixedFullName)
                                .bindValue("shortname",
                                    this.localWikiEntityReferenceSerializer.serialize(memberReference))
                                .bindValue("veryshortname", memberReference.getName());
                    } else {
                        query =
                            context.getWiki().getStore().getQueryManager()
                                .getNamedQuery("listGroupsForUserInOtherWiki")
                                .bindValue("prefixedmembername", prefixedFullName);
                    }

                    query.setOffset(offset);
                    query.setLimit(limit);

                    groupNames = query.execute();
                } catch (QueryException ex) {
                    throw new XWikiException(0, 0, ex.getMessage(), ex);
                }

                // If the 'XWiki.XWikiAllGroup' is implicit, all users/groups except XWikiGuest and XWikiAllGroup
                // itself are part of it.
                if (!groupNames.contains(XWikiRightService.ALLGROUP_GROUP_FULLNAME)
                    && isAllGroupImplicit(context)
                    && (!memberReference.getLastSpaceReference().getName().equals("XWiki") || !memberReference
                        .getName().equals(XWikiRightService.ALLGROUP_GROUP)
                        && !memberReference.getName().equals(XWikiRightService.GUEST_USER))) {
                    groupNames.add("XWiki.XWikiAllGroup");
                }

                groupReferences = new ArrayList<DocumentReference>(groupNames.size());
                for (String groupName : groupNames) {
                    groupReferences.add(this.currentMixedDocumentReferenceResolver.resolve(groupName));
                }

                if (supportCache) {
                    this.memberGroupsCache.set(key, groupReferences);
                }
            }
        }

        return groupReferences;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMembersNamesForGroup(java.lang.String, int, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Collection<String> getAllMembersNamesForGroup(String group, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return getAllMatchedMembersNamesForGroup(group, null, nb, start, null, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#getAllMembersNamesForGroup(java.lang.String, java.lang.String, int,
     *      int, java.lang.Boolean, com.xpn.xwiki.XWikiContext)
     * @since 1.6M1
     */
    public Collection<String> getAllMatchedMembersNamesForGroup(String group, String matchField, int nb, int start,
        Boolean orderAsc, XWikiContext context) throws XWikiException
    {
        // TODO: add cache mechanism.
        XWikiDocument groupDocument = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(group));

        Map<String, Object> parameterValues = new HashMap<String, Object>();
        // //////////////////////////////////////
        // Create the query string
        StringBuffer queryString = new StringBuffer("SELECT field.value");

        queryString.append(' ').append(
            createMatchGroupMembersWhereClause(groupDocument.getFullName(), matchField, orderAsc, parameterValues));

        try {
            // //////////////////////////////////////
            // Create the query
            QueryManager qm = context.getWiki().getStore().getQueryManager();

            Query query = qm.createQuery(queryString.toString(), Query.HQL);

            for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                query.bindValue(entry.getKey(), entry.getValue());
            }

            query.setOffset(start);
            query.setLimit(nb);

            query.setWiki(groupDocument.getDocumentReference().getWikiReference().getName());

            return query.execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllGroupsNamesForMember(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllGroupsNamesForMember(String member, XWikiContext context) throws XWikiException
    {
        if (member == null) {
            return 0;
        }

        // TODO: improve using real request
        return getAllGroupsNamesForMember(member, 0, 0, context).size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.api.XWikiGroupService#countAllMembersNamesForGroup(java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public int countAllMembersNamesForGroup(String group, XWikiContext context) throws XWikiException
    {
        if (group == null) {
            return 0;
        }

        // TODO: improve using real request
        return getAllMembersNamesForGroup(group, 0, 0, context).size();
    }
}
