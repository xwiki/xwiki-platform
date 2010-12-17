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

package com.xpn.xwiki.plugin.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import com.novell.ldap.LDAPConnection;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * LDAP communication tool.
 * 
 * @version $Id$
 * @since 1.3 M2
 */
public class XWikiLDAPUtils
{
    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPUtils.class);

    /**
     * LDAP objectClass parameter.
     */
    private static final String LDAP_OBJECTCLASS = "objectClass";

    /**
     * The name of the LDAP groups cache.
     */
    private static final String CACHE_NAME_GROUPS = "groups";

    /**
     * The configuration of the LDAP group cache.
     */
    private static CacheConfiguration cacheConfigurationGroups;

    /**
     * Default unique user field name.
     */
    private static final String LDAP_DEFAULT_UID = "cn";

    /**
     * The name of the LDAP object field "dn".
     */
    private static final String LDAP_FIELD_DN = "dn";

    /**
     * Contains caches for each LDAP host:port.
     */
    private static Map<String, Map<String, Cache<Map<String, String>>>> cachePool =
        new HashMap<String, Map<String, Cache<Map<String, String>>>>();

    /**
     * The LDAP connection.
     */
    private XWikiLDAPConnection connection;

    /**
     * The LDAP attribute containing the identifier for a user.
     */
    private String uidAttributeName = LDAP_DEFAULT_UID;

    /**
     * Different LDAP implementations groups classes names.
     */
    private Collection<String> groupClasses = XWikiLDAPConfig.DEFAULT_GROUP_CLASSES;

    /**
     * Different LDAP implementations groups member property name.
     */
    private Collection<String> groupMemberFields = XWikiLDAPConfig.DEFAULT_GROUP_MEMBERFIELDS;

    /**
     * The LDAP base DN from where to executes LDAP queries.
     */
    private String baseDN = "";

    /**
     * LDAP search format string.
     */
    private String userSearchFormatString = "({0}={1})";

    /**
     * Create an instance of {@link XWikiLDAPUtils}.
     * 
     * @param connection the XWiki LDAP connection tool.
     */
    public XWikiLDAPUtils(XWikiLDAPConnection connection)
    {
        this.connection = connection;
    }

    /**
     * @param uidAttributeName the LDAP attribute containing the identifier for a user.
     */
    public void setUidAttributeName(String uidAttributeName)
    {
        this.uidAttributeName = uidAttributeName;
    }

    /**
     * @return the LDAP attribute containing the identifier for a user.
     */
    public String getUidAttributeName()
    {
        return this.uidAttributeName;
    }

    /**
     * @param baseDN the LDAP base DN from where to executes LDAP queries.
     */
    public void setBaseDN(String baseDN)
    {
        this.baseDN = baseDN;
    }

    /**
     * @return the LDAP base DN from where to executes LDAP queries.
     */
    public String getBaseDN()
    {
        return this.baseDN;
    }

    /**
     * @param fmt the user search format string.
     */
    public void setUserSearchFormatString(String fmt)
    {
        this.userSearchFormatString = fmt;
    }

    /**
     * @return the user search format string.
     */
    public String getUserSearchFormatString()
    {
        return this.userSearchFormatString;
    }

    /**
     * @param groupClasses the different LDAP implementations groups classes names.
     */
    public void setGroupClasses(Collection<String> groupClasses)
    {
        this.groupClasses = groupClasses;
    }

    /**
     * @return the different LDAP implementations groups classes names.
     */
    public Collection<String> getGroupClasses()
    {
        return this.groupClasses;
    }

    /**
     * @param groupMemberFields the different LDAP implementations groups member property name.
     */
    public void setGroupMemberFields(Collection<String> groupMemberFields)
    {
        this.groupMemberFields = groupMemberFields;
    }

    /**
     * @return the different LDAP implementations groups member property name.
     */
    public Collection<String> getGroupMemberFields()
    {
        return this.groupMemberFields;
    }

    /**
     * Get the cache with the provided name for a particular LDAP server.
     * 
     * @param configuration the configuration to use to create the cache and to find it if it's already created.
     * @param context the XWiki context.
     * @return the cache.
     * @throws CacheException error when creating the cache.
     */
    public Cache<Map<String, String>> getCache(CacheConfiguration configuration, XWikiContext context)
        throws CacheException
    {
        Cache<Map<String, String>> cache;

        String cacheKey =
            getUidAttributeName() + "." + this.connection.getConnection().getHost() + ":"
                + this.connection.getConnection().getPort();

        Map<String, Cache<Map<String, String>>> cacheMap;

        if (cachePool.containsKey(cacheKey)) {
            cacheMap = cachePool.get(cacheKey);
        } else {
            cacheMap = new HashMap<String, Cache<Map<String, String>>>();
            cachePool.put(cacheKey, cacheMap);
        }

        cache = cacheMap.get(configuration.getConfigurationId());

        if (cache == null) {
            cache = Utils.getComponent(CacheManager.class).createNewCache(configuration);
            cacheMap.put(configuration.getConfigurationId(), cache);
        }

        return cache;
    }

    /**
     * @return get {@link XWikiLDAPConnection}.
     */
    public XWikiLDAPConnection getConnection()
    {
        return this.connection;
    }

    /**
     * Execute LDAP query to get all group's members.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups.
     * @return the LDAP search result.
     */
    private List<XWikiLDAPSearchAttribute> searchGroupsMembers(String groupDN)
    {
        String[] attrs = new String[2 + getGroupMemberFields().size()];

        int i = 0;
        attrs[i++] = LDAP_OBJECTCLASS;
        attrs[i++] = getUidAttributeName();
        for (String groupMember : getGroupMemberFields()) {
            attrs[i++] = groupMember;
        }

        return getConnection().searchLDAP(groupDN, null, attrs, LDAPConnection.SCOPE_BASE);
    }

    /**
     * Extract group's members from provided LDAP search result.
     * 
     * @param searchAttributeList the LDAP search result.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups return all the subgroups identified.
     * @param context the XWiki context.
     */
    private void getGroupMembers(List<XWikiLDAPSearchAttribute> searchAttributeList, Map<String, String> memberMap,
        List<String> subgroups, XWikiContext context)
    {
        for (XWikiLDAPSearchAttribute searchAttribute : searchAttributeList) {
            String key = searchAttribute.name;
            if (getGroupMemberFields().contains(key.toLowerCase())) {

                // or subgroup
                String member = searchAttribute.value;

                // we check for subgroups recursive call to scan all subgroups and identify members
                // and their uid
                getGroupMembers(member, memberMap, subgroups, context);
            }
        }
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param searchAttributeList the groups members found in LDAP search.
     * @param context the XWiki context.
     * @return whether the groupDN is actually a group.
     */
    public boolean getGroupMembers(String groupDN, Map<String, String> memberMap, List<String> subgroups,
        List<XWikiLDAPSearchAttribute> searchAttributeList, XWikiContext context)
    {
        boolean isGroup = false;

        String id = null;

        for (XWikiLDAPSearchAttribute searchAttribute : searchAttributeList) {
            String key = searchAttribute.name;

            if (key.equalsIgnoreCase(LDAP_OBJECTCLASS)) {
                String objectName = searchAttribute.value;
                if (getGroupClasses().contains(objectName.toLowerCase())) {
                    isGroup = true;
                }
            } else if (key.equalsIgnoreCase(getUidAttributeName())) {
                id = searchAttribute.value;
            }
        }

        if (!isGroup) {
            if (id == null) {
                LOG.error("Could not find attribute " + getUidAttributeName() + " for LDAP dn " + groupDN);
            }

            if (!memberMap.containsKey(groupDN)) {
                memberMap.put(groupDN.toLowerCase(), id == null ? "" : id.toLowerCase());
            }
        } else {
            // remember this group
            if (subgroups != null) {
                subgroups.add(groupDN);
            }

            getGroupMembers(searchAttributeList, memberMap, subgroups, context);
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param groupDN the group to retrieve the members of and scan for subgroups. If <code>groupDN</code> is a user DN
     *            or UID, it is added to the <code>memberMap</code> and it will return false.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the groupDN is actually a group.
     */
    public boolean getGroupMembers(String groupDN, Map<String, String> memberMap, List<String> subgroups,
        XWikiContext context)
    {
        boolean isGroup = false;
        String fixedDN = groupDN;

        // break out if there is a look of groups
        if (subgroups != null && subgroups.contains(fixedDN)) {
            return true;
        }

        List<XWikiLDAPSearchAttribute> searchAttributeList = searchGroupsMembers(fixedDN);

        if (searchAttributeList == null) {
            // maybe groupDN is a UID so trying to search for it
            searchAttributeList =
                searchUserAttributesByUid(fixedDN, new String[] {LDAP_FIELD_DN, getUidAttributeName()});

            if (searchAttributeList != null && !searchAttributeList.isEmpty()) {
                fixedDN = searchAttributeList.get(0).value;
            }
        }

        if (searchAttributeList != null) {
            isGroup = getGroupMembers(fixedDN, memberMap, subgroups, searchAttributeList, context);
        }

        return isGroup;
    }

    /**
     * Get group members from cache or update it from LDAP if it is not already cached.
     * 
     * @param groupDN the name of the group.
     * @param context the XWiki context.
     * @return the members of the group.
     * @throws XWikiException error when getting the group cache.
     */
    public Map<String, String> getGroupMembers(String groupDN, XWikiContext context) throws XWikiException
    {
        Map<String, String> groupMembers = null;

        Cache<Map<String, String>> cache;
        try {
            cache = getCache(getGroupCacheConfiguration(context), context);

            synchronized (cache) {
                groupMembers = cache.get(groupDN);

                if (groupMembers == null) {
                    Map<String, String> members = new HashMap<String, String>();

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Retrieving Members of the group: " + groupDN);
                    }

                    boolean isGroup = getGroupMembers(groupDN, members, new ArrayList<String>(), context);

                    if (isGroup) {
                        groupMembers = members;
                        cache.set(groupDN, groupMembers);
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found cache entry for group [" + groupDN + "]");
                    }
                }
            }
        } catch (CacheException e) {
            LOG.error("Unknown error with cache", e);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Found group [" + groupDN + "] members :" + groupMembers);
        }

        return groupMembers;
    }

    /**
     * Check if provided DN is in provided LDAP group.
     * 
     * @param memberDN the DN to find in the provided group.
     * @param groupDN the DN of the group where to search.
     * @param context the XWiki context.
     * @return true if provided members in the provided group.
     * @throws XWikiException error when searching for group members.
     */
    public boolean isMemberOfGroup(String memberDN, String groupDN, XWikiContext context) throws XWikiException
    {
        Map<String, String> groupMembers = getGroupMembers(groupDN, context);

        if (groupMembers != null) {
            for (String memberDNEntry : groupMembers.keySet()) {
                if (memberDNEntry.equals(memberDN.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if provided DN is in one of the provided LDAP groups.
     * 
     * @param memberDN the DN to find in the provided groups.
     * @param groupDNList the list of DN of the groups where to search.
     * @param context the XWiki context.
     * @return true if provided members in one of the provided groups.
     * @throws XWikiException error when searching for group members.
     */
    public boolean isMemberOfGroups(String memberDN, Collection<String> groupDNList, XWikiContext context)
        throws XWikiException
    {
        for (String groupDN : groupDNList) {
            if (isMemberOfGroup(memberDN, groupDN, context)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param context the XWiki context used to get cache configuration.
     * @return the configuration for the LDAP groups cache.
     */
    public static CacheConfiguration getGroupCacheConfiguration(XWikiContext context)
    {
        if (cacheConfigurationGroups == null) {
            XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

            cacheConfigurationGroups = new CacheConfiguration();
            cacheConfigurationGroups.setConfigurationId(CACHE_NAME_GROUPS);
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            lru.setTimeToLive(config.getCacheExpiration(context));
            cacheConfigurationGroups.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        }

        return cacheConfigurationGroups;
    }

    /**
     * Locates the user in the Map: either the user is a value or the key starts with the LDAP syntax.
     * 
     * @param userName the name of the user.
     * @param groupMembers the members of LDAP group.
     * @param context the XWiki context.
     * @return the full user name.
     */
    protected String findInGroup(String userName, Map<String, String> groupMembers, XWikiContext context)
    {
        String ldapuser = getUidAttributeName() + "=" + userName.toLowerCase();

        for (Map.Entry<String, String> entry : groupMembers.entrySet()) {
            // implementing it case-insensitive for now
            if (userName.equalsIgnoreCase(entry.getValue()) || entry.getKey().startsWith(ldapuser)) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Check if user is in provided LDAP group.
     * 
     * @param userName the user name.
     * @param groupDN the LDAP group DN.
     * @param context the XWiki context.
     * @return LDAP user's DN if the user is in the LDAP group, null otherwise.
     * @throws XWikiException error when getting the group cache.
     */
    public String isUidInGroup(String userName, String groupDN, XWikiContext context) throws XWikiException
    {
        String userDN = null;

        if (groupDN.length() > 0) {
            Map<String, String> groupMembers = null;

            try {
                groupMembers = getGroupMembers(groupDN, context);
            } catch (Exception e) {
                // Ignore exception to allow negative match for exclusion
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to retrieve group members of group:" + groupDN, e);
                }
            }

            // no match when a user does not have access to the group
            if (groupMembers != null) {
                // check if user is in the list
                userDN = findInGroup(userName, groupMembers, context);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found user dn in user group:" + userDN);
                }
            }
        }

        return userDN;
    }

    /**
     * @param uid the unique identifier of the user in the LDAP server.
     * @param attributeNameTable the names of the LDAP user attributes to query.
     * @return the found LDAP attributes.
     * @since 1.6M2
     */
    public List<XWikiLDAPSearchAttribute> searchUserAttributesByUid(String uid, String[] attributeNameTable)
    {
        // search for the user in LDAP
        String filter =
            MessageFormat.format(
                this.userSearchFormatString,
                new Object[] {XWikiLDAPConnection.escapeLDAPSearchFilter(this.uidAttributeName),
                    XWikiLDAPConnection.escapeLDAPSearchFilter(uid)});

        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching for the user in LDAP: user:" + uid + " base:" + this.baseDN + " query:" + filter
                + " uid:" + uidAttributeName);
        }

        return this.connection.searchLDAP(this.baseDN, filter, attributeNameTable, LDAPConnection.SCOPE_SUB);
    }

    /**
     * @param uid the unique identifier of the user in the LDAP server.
     * @return the user DN, return null if no user was found.
     * @since 1.6M2
     */
    public String searchUserDNByUid(String uid)
    {
        String userDN = null;

        List<XWikiLDAPSearchAttribute> searchAttributes = searchUserAttributesByUid(uid, new String[] {LDAP_FIELD_DN});

        if (searchAttributes != null && searchAttributes.size() > 0) {
            userDN = searchAttributes.get(0).value;
        }

        return userDN;
    }
}
