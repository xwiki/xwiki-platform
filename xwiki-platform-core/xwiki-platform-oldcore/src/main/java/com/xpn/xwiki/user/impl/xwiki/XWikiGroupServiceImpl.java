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
package com.xpn.xwiki.user.impl.xwiki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
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
    public static final EntityReference GROUPCLASS_REFERENCE =
        new EntityReference("XWikiGroups", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDocument.class);

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
    private static final String SPACE_NAME_SEP = ".";

    /**
     * Symbol use in HQL "like" command that means "all characters".
     */
    private static final String HQLLIKE_ALL_SYMBOL = "%";

    /**
     * Query to retrieve all groups with a given member.
     */
    private static final String ALL_GROUPS_WITH_MEMBER_QUERY = ", BaseObject as obj, StringProperty as prop "
        + "where doc.fullName=obj.name and obj.className=?1 and obj.id=prop.id.id "
        + "and prop.name=?2 and prop.value like ?3";

    private static final String NAME = "groupservice";

    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent());
            add(new DocumentUpdatedEvent());
            add(new DocumentDeletedEvent());
        }
    };

    /**
     * Used to convert a string into a proper Document Reference.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    private EntityReferenceSerializer<String> entityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);

    private EntityReferenceSerializer<String> localWikiEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");

    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    @Override
    public synchronized void init(XWiki xwiki, XWikiContext context) throws XWikiException
    {
        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    @Override
    public synchronized void initCache(XWikiContext context) throws XWikiException
    {
        // No cache anymore
    }

    @Override
    public synchronized void initCache(int iCapacity, XWikiContext context) throws XWikiException
    {
        // No cache anymore
    }

    @Override
    public void flushCache()
    {
        // No cache anymore
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
        return context.getWiki().isAllGroupImplicit();
    }

    @Override
    @Deprecated
    public Collection<String> listGroupsForUser(String member, XWikiContext context) throws XWikiException
    {
        return getAllGroupsNamesForMember(member, -1, 0, context);
    }

    @Override
    @Deprecated
    public void addUserToGroup(String username, String database, String group, XWikiContext context)
        throws XWikiException
    {
        // Don't do anything and let the listener do its job if there is a need
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
            equals |= currentMember.equals(memberWiki + WIKI_FULLNAME_SEP + memberSpace + SPACE_NAME_SEP + memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                equals |= currentMember.equals(memberSpace + SPACE_NAME_SEP + memberName);
            }
        }

        if (context.getWikiId() == null || context.getWikiId().equalsIgnoreCase(memberWiki)) {
            equals |= currentMember.equals(memberName);

            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                equals |= currentMember.equals(memberSpace + SPACE_NAME_SEP + memberName);
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

        List<BaseObject> groups = groupDocument.getXObjects(GROUPCLASS_REFERENCE);

        if (groups != null) {
            for (BaseObject bobj : groups) {
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

    private boolean replaceMemberFromGroup(XWikiDocument groupDocument, DocumentReference memberSourceReference,
        DocumentReference memberTargetReference, XWikiContext context)
    {
        boolean needUpdate = false;

        List<BaseObject> groups = groupDocument.getXObjects(GROUPCLASS_REFERENCE);
        String memberSource = this.compactWikiEntityReferenceSerializer.serialize(memberSourceReference);
        String memberTarget = this.compactWikiEntityReferenceSerializer.serialize(memberTargetReference);

        if (groups != null) {
            for (BaseObject bobj : groups) {
                if (bobj != null) {
                    String member = bobj.getStringValue(FIELD_XWIKIGROUPS_MEMBER);

                    if (member.equals(memberSource)) {
                        bobj.set(FIELD_XWIKIGROUPS_MEMBER, memberTarget, context);
                        needUpdate = true;
                    }
                }
            }
        }

        return needUpdate;
    }

    @Override
    public void replaceMemberInAllGroups(DocumentReference memberSourceReference,
        DocumentReference memberTargetReference, XWikiContext context) throws XWikiException
    {
        String memberSource = this.compactWikiEntityReferenceSerializer.serialize(memberSourceReference);

        List<Object> parameterValues = new ArrayList<>();
        parameterValues.add(CLASS_XWIKIGROUPS);
        parameterValues.add(FIELD_XWIKIGROUPS_MEMBER);
        parameterValues.add(memberSource);

        List<XWikiDocument> documentList =
            context.getWiki().getStore().searchDocuments(ALL_GROUPS_WITH_MEMBER_QUERY, parameterValues, context);

        for (XWikiDocument groupDocument : documentList) {
            if (replaceMemberFromGroup(groupDocument, memberSourceReference, memberTargetReference, context)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    @Override
    public void removeUserOrGroupFromAllGroups(String memberWiki, String memberSpace, String memberName,
        XWikiContext context) throws XWikiException
    {
        List<Object> parameterValues = new ArrayList<>();
        parameterValues.add(CLASS_XWIKIGROUPS);
        parameterValues.add(FIELD_XWIKIGROUPS_MEMBER);

        if (context.getWikiId() == null || context.getWikiId().equalsIgnoreCase(memberWiki)) {
            if (memberSpace == null || memberSpace.equals(DEFAULT_MEMBER_SPACE)) {
                parameterValues.add(HQLLIKE_ALL_SYMBOL + memberName + HQLLIKE_ALL_SYMBOL);
            } else {
                parameterValues
                    .add(HQLLIKE_ALL_SYMBOL + memberSpace + SPACE_NAME_SEP + memberName + HQLLIKE_ALL_SYMBOL);
            }
        } else {
            parameterValues.add(HQLLIKE_ALL_SYMBOL + memberWiki + WIKI_FULLNAME_SEP + memberSpace + SPACE_NAME_SEP
                + memberName + HQLLIKE_ALL_SYMBOL);
        }

        List<XWikiDocument> documentList =
            context.getWiki().getStore().searchDocuments(ALL_GROUPS_WITH_MEMBER_QUERY, parameterValues, context);

        for (XWikiDocument groupDocument : documentList) {
            if (removeUserOrGroupFromGroup(groupDocument, memberWiki, memberSpace, memberName, context)) {
                context.getWiki().saveDocument(groupDocument, context);
            }
        }
    }

    @Override
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

                List<BaseObject> groups = docgroup.getXObjects(GROUPCLASS_REFERENCE);
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
            LOGGER.error("Failed to get group document", e);
        }

        return null;
    }

    @Override
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

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiDocument oldDocument = document.getOriginalDocument();

        // if there is any chance some group changed, flush the group cache
        if (document.getXObject(GROUPCLASS_REFERENCE) != null || oldDocument.getXObject(GROUPCLASS_REFERENCE) != null) {
            flushCache();
        }
    }

    @Override
    public List<?> getAllMatchedUsers(Object[][] matchFields, boolean withdetails, int nb, int start, Object[][] order,
        XWikiContext context) throws XWikiException
    {
        return getAllMatchedUsersOrGroups(true, matchFields, withdetails, nb, start, order, context);
    }

    @Override
    public List<?> getAllMatchedGroups(Object[][] matchFields, boolean withdetails, int nb, int start, Object[][] order,
        XWikiContext context) throws XWikiException
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

        StringBuilder from = new StringBuilder(", BaseObject as obj");

        StringBuilder where = new StringBuilder(" where doc.fullName=obj.name");
        parameterValues.add(classtemplate);
        where.append(" and doc.fullName<>?" + parameterValues.size());
        parameterValues.add("XWiki." + documentClass);
        where.append(" and obj.className=?" + parameterValues.size());

        Map<String, String> fieldMap = new HashMap<>();
        int fieldIndex = 0;

        // Manage object match strings
        if (matchFields != null) {
            for (Object[] matchField : matchFields) {
                String fieldName = (String) matchField[0];
                String type = (String) matchField[1];
                String value = (String) matchField[2];

                if (type != null) {
                    String fieldPrefix;

                    if (!fieldMap.containsKey(fieldName)) {
                        fieldPrefix = "field" + fieldIndex;
                        from.append(", " + type + " as " + fieldPrefix);

                        where.append(" and obj.id=" + fieldPrefix + ".id.id");
                        parameterValues.add(fieldName);
                        where.append(" and " + fieldPrefix + ".name=?" + parameterValues.size());
                        ++fieldIndex;
                    } else {
                        fieldPrefix = fieldMap.get(fieldName);
                    }

                    parameterValues.add(HQLLIKE_ALL_SYMBOL + value.toLowerCase() + HQLLIKE_ALL_SYMBOL);
                    where.append(" and lower(" + fieldPrefix + ".value) like ?" + parameterValues.size());

                    fieldMap.put(fieldName, fieldPrefix);
                } else if (user && matchFields.length == 1 && fieldName.equals("name")) {
                    // In case we are only looking to mach on the document name, we should also take care of
                    // filtering on the first name or the last name of the user.
                    parameterValues.add(HQLLIKE_ALL_SYMBOL + value.toLowerCase() + HQLLIKE_ALL_SYMBOL);
                    from.append(", StringProperty firstName, StringProperty lastName");
                    where.append(
                        "and obj.id = firstName.id.id and firstName.id.name = 'first_name' "
                      + "and obj.id = lastName.id.id and lastName.id.name = 'last_name' "
                      + String.format("and (lower(doc.name) like ?%s or lower(firstName.value) like ?%s or "
                            + "lower(lastName.value) like ?%s)",
                            parameterValues.size(), parameterValues.size(), parameterValues.size()));
                } else {
                    parameterValues.add(HQLLIKE_ALL_SYMBOL + value.toLowerCase() + HQLLIKE_ALL_SYMBOL);
                    // We do not support OR filters by default, however this may be useful in the case where users or
                    // groups come with a manually defined title.
                    if (fieldName.equals("name")) {
                        where.append(String.format(" and (lower(doc.name) like ?%s or lower(doc.title) like ?%s)",
                            parameterValues.size(), parameterValues.size()));
                    } else {
                        where.append(String.format(" and lower(doc.%s) like ?%s", fieldName, parameterValues.size()));
                    }
                }
            }
        }

        StringBuilder orderString = new StringBuilder();

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

                        parameterValues.add(fieldName);
                        where.append(" and " + fieldPrefix + ".name=?" + parameterValues.size());

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
    protected List<?> getAllMatchedUsersOrGroups(boolean user, Object[][] matchFields, boolean withdetails, int nb,
        int start, Object[][] order, XWikiContext context) throws XWikiException
    {
        List<?> groups = null;

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
                throw new UnsupportedOperationException(
                    "The current storage engine does not support advanced group queries");
            }

            try {
                groups =
                    context
                        .getWiki().getStore().getQueryManager().createQuery("/*/*[obj/XWiki/"
                            + (user ? CLASS_SUFFIX_XWIKIUSERS : CLASS_SUFFIX_XWIKIGROUPS) + "]/@fullName", Query.XPATH)
                        .setLimit(nb).setOffset(start).execute();
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

        List<?> list = context.getWiki().getStore().search(sql, 0, 0, parameterValues, context);

        if (list == null || list.size() == 0) {
            return 0;
        }

        return ((Number) list.get(0)).intValue();
    }

    @Override
    public int countAllMatchedUsers(Object[][] matchFields, XWikiContext context) throws XWikiException
    {
        return countAllMatchedUsersOrGroups(true, matchFields, context);
    }

    @Override
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
        StringBuilder queryString = new StringBuilder();

        // Add from clause.
        queryString.append(" FROM BaseObject as obj, StringProperty as field");

        // Add where clause. Select all group members.
        queryString.append(" WHERE obj.name = :groupdocname and obj.className = :groupclassname")
            .append(" and obj.id = field.id.id and field.id.name = 'member'");
        parameterValues.put("groupdocname", groupFullName);
        parameterValues.put("groupclassname", CLASS_XWIKIGROUPS);

        if (matchField != null) {
            // Filter the members that match the given string.
            // Match users from the same wiki.
            String matchedUsers = "select userDoc.fullName " + "from XWikiDocument userDoc, BaseObject as userObj,"
                + " StringProperty as userFirstName, StringProperty as userLastName "
                + "where userDoc.fullName = userObj.name and userObj.className = 'XWiki.XWikiUsers'"
                + " and userObj.id = userFirstName.id.id and userFirstName.id.name = 'first_name'"
                + " and userObj.id = userLastName.id.id and userLastName.id.name = 'last_name'"
                + " and (lower(userDoc.name) like :matchfield or"
                + " concat(concat(lower(userFirstName.value), ' '), lower(userLastName.value)) like :matchfield)";
            // Match groups from the same wiki.
            String matchedGroups =
                "select distinct(groupDoc.fullName) " + "from XWikiDocument groupDoc, BaseObject as groupObj "
                    + "where groupDoc.fullName = groupObj.name and groupObj.className = 'XWiki.XWikiGroups'"
                    + " and (lower(groupDoc.name) like :matchfield or lower(groupDoc.title) like :matchfield)";
            // Match also the field value because we can have members from a different wiki and we should at least be
            // able to filter them by their reference.
            queryString.append(" and (lower(field.value) like :matchfield or field.value in (").append(matchedUsers)
                .append(") or field.value in (").append(matchedGroups).append("))");
            parameterValues.put("matchfield", HQLLIKE_ALL_SYMBOL + matchField.toLowerCase() + HQLLIKE_ALL_SYMBOL);
        } else {
            // Filter out empty values.
            // Note: We should normally be able to use the 3-argument trim() function which defaults to the whitesapce
            // char to be trimmed. However because of https://hibernate.atlassian.net/browse/HHH-8295 this raises a
            // warning in the XWiki console. Once this is fixed in Hibernate we can start using the 3-argument function
            // again.
            queryString.append(" and (trim(both ' ' from field.value) <> '' or "
                + "(trim(both ' ' from field.value) is not null and '' is null))");
        }

        // Add order by clause
        if (orderAsc != null) {
            queryString.append(" ORDER BY field.value ").append(orderAsc ? "asc" : "desc");
        }

        return queryString.toString();
    }

    @Override
    public Collection<String> getAllGroupsNamesForMember(String member, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        List<String> groupNames = null;

        DocumentReference memberReference = this.currentMixedDocumentReferenceResolver.resolve(member);

        String currentWiki = context.getWikiId();
        try {
            context.setWikiId(memberReference.getWikiReference().getName());

            Collection<DocumentReference> groupReferences =
                getAllGroupsReferencesForMember(memberReference, nb, start, context);

            groupNames = new ArrayList<String>(groupReferences.size());
            for (DocumentReference groupReference : groupReferences) {
                groupNames.add(this.localWikiEntityReferenceSerializer.serialize(groupReference));
            }
        } finally {
            context.setWikiId(currentWiki);
        }

        return groupNames;
    }

    @Override
    public Collection<DocumentReference> getAllGroupsReferencesForMember(DocumentReference memberReference, int limit,
        int offset, XWikiContext context) throws XWikiException
    {
        Collection<DocumentReference> groupReferences = null;

        List<String> groupNames;
        try {
            Query query;
            if (XWikiRightService.isGuest(memberReference)) {
                query = context.getWiki().getStore().getQueryManager().getNamedQuery("listGroupsForUser")
                    .bindValue("username", XWikiRightService.GUEST_USER_FULLNAME)
                    .bindValue("shortname", XWikiRightService.GUEST_USER_FULLNAME)
                    .bindValue("veryshortname", XWikiRightService.GUEST_USER);
            } else if (memberReference.getWikiReference().getName().equals(context.getWikiId())
                || (memberReference.getLastSpaceReference().getName().equals("XWiki")
                    && memberReference.getName().equals(XWikiRightService.GUEST_USER))) {
                query = context.getWiki().getStore().getQueryManager().getNamedQuery("listGroupsForUser")
                    .bindValue("username", this.entityReferenceSerializer.serialize(memberReference))
                    .bindValue("shortname", this.localWikiEntityReferenceSerializer.serialize(memberReference))
                    .bindValue("veryshortname", memberReference.getName());
            } else {
                query = context.getWiki().getStore().getQueryManager().getNamedQuery("listGroupsForUserInOtherWiki")
                    .bindValue("prefixedmembername", this.entityReferenceSerializer.serialize(memberReference));
            }

            query.setOffset(offset);
            query.setLimit(limit);

            groupNames = query.execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }

        groupReferences = new HashSet<DocumentReference>(groupNames.size());
        for (String groupName : groupNames) {
            groupReferences.add(this.currentMixedDocumentReferenceResolver.resolve(groupName));
        }

        // If the 'XWiki.XWikiAllGroup' is implicit, all users/groups except XWikiGuest and XWikiAllGroup
        // itself are part of it.
        if (isAllGroupImplicit(context) && !XWikiRightService.isGuest(memberReference)
            && memberReference.getWikiReference().getName().equals(context.getWikiId())) {
            DocumentReference currentXWikiAllGroup =
                new DocumentReference(context.getWikiId(), "XWiki", XWikiRightService.ALLGROUP_GROUP);

            if (!currentXWikiAllGroup.equals(memberReference)) {
                groupReferences.add(currentXWikiAllGroup);
            }
        }

        return groupReferences;
    }

    @Override
    public Collection<String> getAllMembersNamesForGroup(String group, int nb, int start, XWikiContext context)
        throws XWikiException
    {
        return getAllMatchedMembersNamesForGroup(group, null, nb, start, null, context);
    }

    @Override
    public Collection<String> getAllMatchedMembersNamesForGroup(String group, String matchField, int nb, int start,
        Boolean orderAsc, XWikiContext context) throws XWikiException
    {
        DocumentReference groupReference = this.currentMixedDocumentReferenceResolver.resolve(group);
        String localGroupReference = this.localWikiEntityReferenceSerializer.serialize(groupReference);

        Map<String, Object> parameters = new HashMap<>();
        StringBuilder statement = new StringBuilder("SELECT field.value ")
            .append(createMatchGroupMembersWhereClause(localGroupReference, matchField, orderAsc, parameters));

        try {
            QueryManager queryManager = context.getWiki().getStore().getQueryManager();
            Query query = queryManager.createQuery(statement.toString(), Query.HQL);

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.bindValue(entry.getKey(), entry.getValue());
            }

            query.setOffset(start);
            query.setLimit(nb);
            query.setWiki(groupReference.getWikiReference().getName());

            return query.execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    @Override
    public int countAllMatchedMembersNamesForGroup(String group, String filter, XWikiContext xcontext)
        throws XWikiException
    {
        DocumentReference groupReference = this.currentMixedDocumentReferenceResolver.resolve(group);
        String localGroupReference = this.localWikiEntityReferenceSerializer.serialize(groupReference);

        Map<String, Object> parameters = new HashMap<>();
        StringBuilder statement = new StringBuilder("select count(field.value) ")
            .append(createMatchGroupMembersWhereClause(localGroupReference, filter, null, parameters));

        try {
            QueryManager queryManager = xcontext.getWiki().getStore().getQueryManager();
            Query query = queryManager.createQuery(statement.toString(), Query.HQL);
            query.setWiki(groupReference.getWikiReference().getName());

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.bindValue(entry.getKey(), entry.getValue());
            }

            return ((Long) query.execute().get(0)).intValue();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    @Override
    public int countAllGroupsNamesForMember(String member, XWikiContext context) throws XWikiException
    {
        if (member == null) {
            return 0;
        }

        // TODO: improve using real request
        return getAllGroupsNamesForMember(member, 0, 0, context).size();
    }

    @Override
    public int countAllMembersNamesForGroup(String group, XWikiContext context) throws XWikiException
    {
        if (group == null) {
            return 0;
        }

        // TODO: improve using real request
        return getAllMembersNamesForGroup(group, 0, 0, context).size();
    }
}
