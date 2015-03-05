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
package com.xpn.xwiki.plugin.ldap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPDN;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.rfc2251.RfcFilter;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.impl.LDAP.LDAPProfileXClass;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiLDAPUtils.class);

    /**
     * LDAP objectClass parameter.
     */
    private static final String LDAP_OBJECTCLASS = "objectClass";

    /**
     * The name of the LDAP groups cache.
     */
    private static final String CACHE_NAME_GROUPS = "ldap.groups";

    /**
     * The name of the XWiki group member field.
     */
    private static final String XWIKI_GROUP_MEMBERFIELD = "member";

    /**
     * The XWiki space where users are stored.
     */
    private static final String XWIKI_USER_SPACE = "XWiki";

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
     * @deprecated use {@link #getGroupCache(CacheConfiguration, XWikiContext)} instead since 4.1M1
     */
    @Deprecated
    public Cache<Map<String, String>> getCache(CacheConfiguration configuration, XWikiContext context)
        throws CacheException
    {
        return getGroupCache(configuration, context);
    }

    /**
     * Get the cache with the provided name for a particular LDAP server.
     * 
     * @param configuration the configuration to use to create the cache and to find it if it's already created.
     * @param context the XWiki context.
     * @return the cache.
     * @throws CacheException error when creating the cache.
     * @since 4.1M1
     */
    public Cache<Map<String, String>> getGroupCache(CacheConfiguration configuration, XWikiContext context)
        throws CacheException
    {
        Cache<Map<String, String>> cache;

        String cacheKey =
            getUidAttributeName() + "." + this.connection.getConnection().getHost() + ":"
                + this.connection.getConnection().getPort();

        synchronized (cachePool) {
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
        }

        return cache;
    }

    /**
     * Force to empty the group cache.
     * 
     * @since 4.1M1
     */
    public static void resetGroupCache()
    {
        synchronized (cachePool) {
            for (Map<String, Cache<Map<String, String>>> caches : cachePool.values()) {
                for (Cache<Map<String, String>> cache : caches.values()) {
                    cache.dispose();
                }
            }
        }

        cachePool.clear();
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
     * @throws LDAPException failed to execute LDAP query
     */
    private LDAPSearchResults searchGroupsMembersByDN(String groupDN) throws LDAPException
    {
        String[] attrs = new String[2 + getGroupMemberFields().size()];

        int i = 0;
        attrs[i++] = LDAP_OBJECTCLASS;
        for (String groupMember : getGroupMemberFields()) {
            attrs[i++] = groupMember;
        }

        // in case it's a organization unit get the users ids
        attrs[i++] = getUidAttributeName();

        return getConnection().search(groupDN, null, attrs, LDAPConnection.SCOPE_SUB);
    }

    /**
     * Execute LDAP query to get all group's members.
     * 
     * @param filter the LDAP filter to search with.
     * @return the LDAP search result.
     * @throws LDAPException failed to execute LDAP query
     */
    private LDAPSearchResults searchGroupsMembersByFilter(String filter) throws LDAPException
    {
        String[] attrs = new String[2 + getGroupMemberFields().size()];

        int i = 0;
        attrs[i++] = LDAP_OBJECTCLASS;
        for (String groupMember : getGroupMemberFields()) {
            attrs[i++] = groupMember;
        }

        // in case it's a organization unit get the users ids
        attrs[i++] = getUidAttributeName();

        return getConnection().search(getBaseDN(), filter, attrs, LDAPConnection.SCOPE_SUB);
    }

    /**
     * Extract group's members from provided LDAP search result.
     * 
     * @param searchAttributeList the LDAP search result.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups return all the subgroups identified.
     * @param context the XWiki context.
     */
    private void getGroupMembersFromSearchResult(List<XWikiLDAPSearchAttribute> searchAttributeList,
        Map<String, String> memberMap, List<String> subgroups, XWikiContext context)
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
     * Extract group's members from provided LDAP search result.
     * 
     * @param ldapEntry the LDAP search result.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups return all the subgroups identified.
     * @param context the XWiki context.
     */
    private void getGroupMembersFromLDAPEntry(LDAPEntry ldapEntry, Map<String, String> memberMap,
        List<String> subgroups, XWikiContext context)
    {
        for (String memberField : getGroupMemberFields()) {
            LDAPAttribute attribute = ldapEntry.getAttribute(memberField);
            if (attribute != null) {
                Enumeration<String> values = attribute.getStringValues();
                while (values.hasMoreElements()) {
                    String member = values.nextElement();

                    LOGGER.debug("  |- Member value [{}] found. Trying to resolve it.", member);

                    // we check for subgroups recursive call to scan all subgroups and identify members
                    // and their uid
                    getGroupMembers(member, memberMap, subgroups, context);
                }
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
                LOGGER.error("Could not find attribute [{}] for LDAP dn [{}]", getUidAttributeName(), groupDN);
            }

            if (!memberMap.containsKey(groupDN.toLowerCase())) {
                memberMap.put(groupDN.toLowerCase(), id == null ? "" : id.toLowerCase());
            }
        } else {
            // remember this group
            if (subgroups != null) {
                subgroups.add(groupDN.toLowerCase());
            }

            getGroupMembersFromSearchResult(searchAttributeList, memberMap, subgroups, context);
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param ldapEntry the ldap entry returned by a search members found in LDAP search.
     * @param context the XWiki context.
     * @return whether the groupDN is actually a group.
     * @throws LDAPException error when parsing the provided LDAP entry
     * @since 3.3M1
     */
    public boolean getGroupMembers(Map<String, String> memberMap, List<String> subgroups, LDAPEntry ldapEntry,
        XWikiContext context) throws LDAPException
    {
        boolean isGroup = false;

        // Check if the entry is a group

        LDAPAttribute classAttribute = ldapEntry.getAttribute(LDAP_OBJECTCLASS);
        if (classAttribute != null) {
            Enumeration<String> values = classAttribute.getStringValues();
            Collection<String> groupClasses = getGroupClasses();
            while (values.hasMoreElements()) {
                String value = values.nextElement();
                if (groupClasses.contains(value.toLowerCase())) {
                    isGroup = true;
                }
            }
        }

        // Get members or user id if it's a user

        if (isGroup) {
            // remember this group
            if (subgroups != null) {
                subgroups.add(ldapEntry.getDN().toLowerCase());
            }

            getGroupMembersFromLDAPEntry(ldapEntry, memberMap, subgroups, context);
        } else {
            LDAPAttribute uidAttribute = ldapEntry.getAttribute(getUidAttributeName());

            if (uidAttribute != null) {
                String uid = uidAttribute.getStringValue();

                if (!memberMap.containsKey(ldapEntry.getDN().toLowerCase())) {
                    memberMap.put(ldapEntry.getDN().toLowerCase(), uid.toLowerCase());
                }
            } else {
                LOGGER.debug("Probably a organization unit or a search");
            }
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param userOrGroup the group to retrieve the members of and scan for subgroups. Can be
     *            <ul>
     *            <li>a group DN</li>
     *            <li>a user DN</li>
     *            <li>a group id</li>
     *            <li>a user id</li>
     *            </ul>
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the identifier is actually a group.
     */
    public boolean getGroupMembers(String userOrGroup, Map<String, String> memberMap, List<String> subgroups,
        XWikiContext context)
    {
        boolean isGroup = false;

        int nbMembers = memberMap.size();
        if (LDAPDN.isValid(userOrGroup)) {
            LOGGER.debug("[{}] is a valid DN, lets try to get corresponding entry.", userOrGroup);

            isGroup = getGroupMembersFromDN(userOrGroup, memberMap, subgroups, context);
        }

        if (!isGroup && nbMembers == memberMap.size()) {
            // Probably not a DN, lets try as filter or id
            LOGGER.debug("Looks like [{}] is not a DN, lets try filter or id", userOrGroup);

            try {
                // Test if it's valid LDAP filter syntax
                new RfcFilter(userOrGroup);
                isGroup = getGroupMembersFromFilter(userOrGroup, memberMap, subgroups, context);
            } catch (LDAPException e) {
                LOGGER.debug("[{}] is not a valid LDAP filter, lets try id", userOrGroup, e);

                // Not a valid filter, try as uid
                List<XWikiLDAPSearchAttribute> searchAttributeList =
                    searchUserAttributesByUid(userOrGroup, new String[] {LDAP_FIELD_DN});

                if (searchAttributeList != null && !searchAttributeList.isEmpty()) {
                    String dn = searchAttributeList.get(0).value;
                    isGroup = getGroupMembers(dn, memberMap, subgroups, context);
                }
            }
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param userOrGroupDN the group DN to retrieve the members from or the user DN to add in the members map.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the provided DN is actually a group or not.
     */
    public boolean getGroupMembersFromDN(String userOrGroupDN, Map<String, String> memberMap, List<String> subgroups,
        XWikiContext context)
    {
        boolean isGroup = false;

        // break out if there is a loop of groups
        if (subgroups != null && subgroups.contains(userOrGroupDN.toLowerCase())) {
            LOGGER.debug("[{}] groups already resolved.", userOrGroupDN);

            return true;
        }

        LDAPSearchResults result;
        try {
            result = searchGroupsMembersByDN(userOrGroupDN);
        } catch (LDAPException e) {
            LOGGER.debug("Failed to search for [{}]", userOrGroupDN, e);

            return false;
        }

        try {
            isGroup = getGroupMembersSearchResult(result, memberMap, subgroups, context);
        } finally {
            if (result.hasMore()) {
                try {
                    getConnection().getConnection().abandon(result);
                } catch (LDAPException e) {
                    LOGGER.debug("LDAP Search clean up failed", e);
                }
            }
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the groupDN. If the group contains subgroups get these members as well.
     * Retrieve an identifier for each member.
     * 
     * @param filter the LDAP filter to search with.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the provided DN is actually a group or not.
     */
    public boolean getGroupMembersFromFilter(String filter, Map<String, String> memberMap, List<String> subgroups,
        XWikiContext context)
    {
        boolean isGroup = false;

        LDAPSearchResults result;
        try {
            result = searchGroupsMembersByFilter(filter);
        } catch (LDAPException e) {
            LOGGER.debug("Failed to search for [{}]", filter, e);

            return false;
        }

        try {
            isGroup = getGroupMembersSearchResult(result, memberMap, subgroups, context);
        } finally {
            if (result.hasMore()) {
                try {
                    getConnection().getConnection().abandon(result);
                } catch (LDAPException e) {
                    LOGGER.debug("LDAP Search clean up failed", e);
                }
            }
        }

        return isGroup;
    }

    /**
     * Get all members of a given group based on the the result of a LDAP search. If the group contains subgroups get
     * these members as well. Retrieve an identifier for each member.
     * 
     * @param result the result of a LDAP search.
     * @param memberMap the result: maps DN to member id.
     * @param subgroups all the subgroups identified.
     * @param context the XWiki context.
     * @return whether the provided DN is actually a group or not.
     */
    public boolean getGroupMembersSearchResult(LDAPSearchResults result, Map<String, String> memberMap,
        List<String> subgroups, XWikiContext context)
    {
        boolean isGroup = false;

        LDAPEntry resultEntry = null;
        // For some weird reason result.hasMore() is always true before the first call to next() even if nothing is
        // found
        if (result.hasMore()) {
            try {
                resultEntry = result.next();
            } catch (LDAPException e) {
                LOGGER.debug("Failed to get group members", e);
            }
        }

        if (resultEntry != null) {
            do {
                try {
                    isGroup |= getGroupMembers(memberMap, subgroups, resultEntry, context);

                    resultEntry = result.hasMore() ? result.next() : null;
                } catch (LDAPException e) {
                    LOGGER.debug("Failed to get group members", e);
                }
            } while (resultEntry != null);
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
            cache = getGroupCache(getGroupCacheConfiguration(context), context);

            synchronized (cache) {
                groupMembers = cache.get(groupDN);

                if (groupMembers == null) {
                    Map<String, String> members = new HashMap<String, String>();

                    LOGGER.debug("Retrieving Members of the group [{}]", groupDN);

                    boolean isGroup = getGroupMembers(groupDN, members, new ArrayList<String>(), context);

                    if (isGroup || !members.isEmpty()) {
                        groupMembers = members;
                        cache.set(groupDN, groupMembers);
                    }
                } else {
                    LOGGER.debug("Found cache entry for group [{}]", groupDN);
                }
            }
        } catch (CacheException e) {
            LOGGER.error("Unknown error with cache", e);
        }

        LOGGER.debug("Found group [{}] members [{}]", groupDN, groupMembers);

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
                LOGGER.debug("Unable to retrieve group members of group [{}]", groupDN, e);
            }

            // no match when a user does not have access to the group
            if (groupMembers != null) {
                // check if user is in the list
                userDN = findInGroup(userName, groupMembers, context);

                LOGGER.debug("Found user dn in user group [{}]", userDN);
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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Searching for the user in LDAP: user [{}] base [{}] query [{}] uid [{}]", new Object[] {uid,
            this.baseDN, filter, this.uidAttributeName});
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

    /**
     * Update or create XWiki user base on LDAP.
     * 
     * @param userProfile the name of the user.
     * @param ldapDn the LDAP user DN.
     * @param ldapUid the UID field.
     * @param searchAttributeListIn the attributes of the LDAP user.
     * @param context the XWiki context.
     * @throws XWikiException error when updating or creating XWiki user.
     */
    // TODO this method does a variable re-assignment on userProfile.
    // This need to be investigated and refactored to avoid this need.
    public void syncUser(XWikiDocument userProfile, List<XWikiLDAPSearchAttribute> searchAttributeListIn,
        String ldapDn, String ldapUid, XWikiContext context) throws XWikiException
    {
        // check if we have to create the user
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        if (userProfile.isNew() || config.getLDAPParam("ldap_update_user", "0", context).equals("1")) {

            LOGGER.debug("LDAP attributes will be used to update XWiki attributes.");

            List<XWikiLDAPSearchAttribute> searchAttributeList = searchAttributeListIn;

            // get attributes from LDAP if we don't already have them
            if (searchAttributeList == null) {
                // didn't get attributes before, so do it now
                searchAttributeList =
                    this.getConnection().searchLDAP(ldapDn, null, getAttributeNameTable(context),
                        LDAPConnection.SCOPE_BASE);
            }

            if (searchAttributeList == null) {
                LOGGER.error("Can't find any attributes for user [{}]", ldapDn);
            }

            if (userProfile.isNew()) {
                LOGGER.debug("Creating new XWiki user based on LDAP attribues located at [{}]", ldapDn);

                createUserFromLDAP(userProfile, searchAttributeList, ldapDn, ldapUid, context);

                LOGGER.debug("New XWiki user created: [{}]", userProfile.getDocumentReference());

            } else {

                LOGGER.debug("Updating existing user with LDAP attribues located at [{}]", ldapDn);

                try {
                    updateUserFromLDAP(userProfile, searchAttributeList, ldapDn, ldapUid, context);
                } catch (XWikiException e) {
                    LOGGER.error("Failed to synchronise user's informations", e);
                }
            }
        }
    }

    /**
     * Synchronize user XWiki membership with it's LDAP membership.
     * 
     * @param xwikiUserName the name of the user.
     * @param userDN the LDAP DN of the user.
     * @param groupMappings the mapping between XWiki groups names and LDAP groups names.
     * @param context the XWiki context.
     * @throws XWikiException error when synchronizing user membership.
     */
    public void syncGroupsMembership(String xwikiUserName, String userDN, Map<String, Set<String>> groupMappings,
        XWikiContext context) throws XWikiException
    {
        LOGGER.debug("Updating group membership for the user [{}]", xwikiUserName);

        Collection<String> xwikiUserGroupList =
            context.getWiki().getGroupService(context).getAllGroupsNamesForMember(xwikiUserName, 0, 0, context);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The user belongs to following XWiki groups: ");
            for (String userGroupName : xwikiUserGroupList) {
                LOGGER.debug(userGroupName);
            }
        }

        // go through mapped groups to locate the user
        for (Map.Entry<String, Set<String>> entry : groupMappings.entrySet()) {
            String xwikiGroupName = entry.getKey();
            Set<String> groupDNSet = entry.getValue();

            if (xwikiUserGroupList.contains(xwikiGroupName)) {
                if (!this.isMemberOfGroups(userDN, groupDNSet, context)) {
                    removeUserFromXWikiGroup(xwikiUserName, xwikiGroupName, context);
                }
            } else {
                if (this.isMemberOfGroups(userDN, groupDNSet, context)) {
                    addUserToXWikiGroup(xwikiUserName, xwikiGroupName, context);
                }
            }
        }
    }

    /**
     * @param context the XWiki context.
     * @return the LDAP user attributes names.
     */
    public String[] getAttributeNameTable(XWikiContext context)
    {
        String[] attributeNameTable = null;

        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        List<String> attributeNameList = new ArrayList<String>();
        config.getUserMappings(attributeNameList, context);

        int lsize = attributeNameList.size();
        if (lsize > 0) {
            attributeNameTable = attributeNameList.toArray(new String[lsize]);
        }

        return attributeNameTable;
    }

    /**
     * Create an XWiki user and set all mapped attributes from LDAP to XWiki attributes.
     * 
     * @param userProfile the XWiki user profile.
     * @param searchAttributes the attributes.
     * @param ldapDN the LDAP DN of the user.
     * @param ldapUid the LDAP unique id of the user.
     * @param context the XWiki context.
     * @throws XWikiException error when creating XWiki user.
     */
    protected void createUserFromLDAP(XWikiDocument userProfile, List<XWikiLDAPSearchAttribute> searchAttributes,
        String ldapDN, String ldapUid, XWikiContext context) throws XWikiException
    {
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        Map<String, String> userMappings = config.getUserMappings(null, context);

        LOGGER.debug("Start first synchronization of LDAP profile [{}] with new user profile based on mapping [{}]",
            searchAttributes, userMappings);

        Map<String, String> map = new HashMap<String, String>();
        if (searchAttributes != null) {
            for (XWikiLDAPSearchAttribute lattr : searchAttributes) {
                String lval = lattr.value;
                String xattr = userMappings.get(lattr.name.toLowerCase());

                if (xattr == null) {
                    continue;
                }

                map.put(xattr, lval);
            }
        }

        // Mark user active
        map.put("active", "1");

        context.getWiki().createUser(userProfile.getDocumentReference().getName(), map, context);

        // Update ldap profile object
        XWikiDocument createdUserProfile = context.getWiki().getDocument(userProfile.getDocumentReference(), context);
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        if (ldapXClass.updateLDAPObject(createdUserProfile, ldapDN, ldapUid)) {
            context.getWiki().saveDocument(createdUserProfile, "Created user profile from LDAP server", context);
        }
    }

    /**
     * Sets attributes on the user object based on attribute values provided by the LDAP.
     * 
     * @param userProfile the XWiki user profile document.
     * @param searchAttributes the attributes of the LDAP user to update.
     * @param ldapDN the DN of the LDAP user to update
     * @param ldapUid value of the unique identifier for the user to update.
     * @param context the XWiki context.
     * @throws XWikiException error when updating XWiki user.
     */
    protected void updateUserFromLDAP(XWikiDocument userProfile, List<XWikiLDAPSearchAttribute> searchAttributes,
        String ldapDN, String ldapUid, XWikiContext context) throws XWikiException
    {
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        Map<String, String> userMappings = config.getUserMappings(null, context);

        BaseClass userClass = context.getWiki().getUserClass(context);

        BaseObject userObj = userProfile.getXObject(userClass.getDocumentReference());

        LOGGER.debug("Start synchronization of LDAP profile [{}] with existing user profile based on mapping [{}]",
            searchAttributes, userMappings);

        Map<String, String> map = new HashMap<String, String>();
        if (searchAttributes != null) {
            for (XWikiLDAPSearchAttribute lattr : searchAttributes) {
                String key = userMappings.get(lattr.name.toLowerCase());
                if (key == null || userClass.get(key) == null) {
                    continue;
                }
                String value = lattr.value;

                String objValue = userObj.getStringValue(key);
                if (objValue == null || !objValue.equals(value)) {
                    map.put(key, value);
                }
            }
        }

        boolean needsUpdate = false;
        if (!map.isEmpty()) {
            userClass.fromMap(map, userObj);
            needsUpdate = true;
        }

        // Update ldap profile object
        LDAPProfileXClass ldaXClass = new LDAPProfileXClass(context);
        needsUpdate |= ldaXClass.updateLDAPObject(userProfile, ldapDN, ldapUid);

        if (needsUpdate) {
            context.getWiki().saveDocument(userProfile, "Synchronized user profile with LDAP server", true, context);
        }
    }

    /**
     * Add user name to provided XWiki group.
     * 
     * @param xwikiUserName the full name of the user.
     * @param groupName the name of the group.
     * @param context the XWiki context.
     */
    // TODO move this methods in a toolkit for all platform.
    protected void addUserToXWikiGroup(String xwikiUserName, String groupName, XWikiContext context)
    {
        try {
            LOGGER.debug("Adding user [{}] to xwiki group [{}]", xwikiUserName, groupName);

            BaseClass groupClass = context.getWiki().getGroupClass(context);

            // Get document representing group
            XWikiDocument groupDoc = context.getWiki().getDocument(groupName, context);

            synchronized (groupDoc) {
                // Add a member object to document
                BaseObject memberObj = groupDoc.newXObject(groupClass.getDocumentReference(), context);
                Map<String, String> map = new HashMap<String, String>();
                map.put(XWIKI_GROUP_MEMBERFIELD, xwikiUserName);
                groupClass.fromMap(map, memberObj);

                // If the document is new, set its content
                if (groupDoc.isNew()) {
                    groupDoc.setSyntax(Syntax.XWIKI_2_0);
                    groupDoc.setContent("{{include reference='XWiki.XWikiGroupSheet' /}}");
                }

                // Save modifications
                context.getWiki().saveDocument(groupDoc, context);
            }

            LOGGER.debug("Finished adding user [{}] to xwiki group [{}]", xwikiUserName, groupName);
        } catch (Exception e) {
            LOGGER.error("Failed to add a user [{}] to a group [{}]", new Object[] {xwikiUserName, groupName, e});
        }
    }

    /**
     * Remove user name from provided XWiki group.
     * 
     * @param xwikiUserName the full name of the user.
     * @param groupName the name of the group.
     * @param context the XWiki context.
     */
    // TODO move this methods in a toolkit for all platform.
    protected void removeUserFromXWikiGroup(String xwikiUserName, String groupName, XWikiContext context)
    {
        try {
            BaseClass groupClass = context.getWiki().getGroupClass(context);

            // Get the XWiki document holding the objects comprising the group membership list
            XWikiDocument groupDoc = context.getWiki().getDocument(groupName, context);

            synchronized (groupDoc) {
                // Get and remove the specific group membership object for the user
                BaseObject groupObj =
                    groupDoc.getXObject(groupClass.getDocumentReference(), XWIKI_GROUP_MEMBERFIELD, xwikiUserName);
                groupDoc.removeXObject(groupObj);

                // Save modifications
                context.getWiki().saveDocument(groupDoc, context);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove a user from a group " + xwikiUserName + " group: " + groupName, e);
        }
    }

    /**
     * @param validXWikiUserName the valid XWiki name of the user to get the profile for. Used for fast lookup relying
     *            on the document cache before doing a database search.
     * @param ldapUid the UID to get the profile for
     * @param context the XWiki context
     * @return the XWiki document of the user with the passed UID
     * @throws XWikiException when a problem occurs while retrieving the user profile
     */
    public XWikiDocument getUserProfileByUid(String validXWikiUserName, String ldapUid, XWikiContext context)
        throws XWikiException
    {
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        // Try default profile name (generally in the cache)
        XWikiDocument userProfile =
            context.getWiki().getDocument(
                new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName), context);

        if (!ldapUid.equalsIgnoreCase(ldapXClass.getUid(userProfile))) {
            // Search for existing profile with provided uid
            userProfile = ldapXClass.searchDocumentByUid(ldapUid);

            // Resolve default profile patch of an uid
            if (userProfile == null) {
                userProfile = getAvailableUserProfile(validXWikiUserName, ldapUid, context);
            }
        }

        return userProfile;
    }

    /**
     * @param validXWikiUserName a valid XWiki username for which to get a profile document
     * @param ldapUid ldap UID of the user profile to get
     * @param context the XWiki context
     * @return a (new) XWiki document for the passed username
     * @throws XWikiException when a problem occurs while retrieving the user profile
     */
    private XWikiDocument getAvailableUserProfile(String validXWikiUserName, String ldapUid, XWikiContext context)
        throws XWikiException
    {
        BaseClass userClass = context.getWiki().getUserClass(context);
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        DocumentReference userReference =
            new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName);

        // Check if the default profile document is available
        for (int i = 0; true; ++i) {
            if (i > 0) {
                userReference =
                    new DocumentReference(context.getWikiId(), XWIKI_USER_SPACE, validXWikiUserName + "_" + i);
            }

            XWikiDocument doc = context.getWiki().getDocument(userReference, context);

            // Don't use non user existing document
            if (doc.isNew() || doc.getXObject(userClass.getDocumentReference()) != null) {
                String ldapUidFromObject = ldapXClass.getUid(doc);

                // If the user is a LDAP user compare uids
                if (ldapUidFromObject == null || ldapUid.equalsIgnoreCase(ldapUidFromObject)) {
                    return doc;
                }
            }
        }
    }
}
