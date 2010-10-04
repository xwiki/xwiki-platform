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

package com.xpn.xwiki.user.impl.LDAP;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.securityfilter.realm.SimplePrincipal;
import org.xwiki.model.reference.DocumentReference;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPConfig;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPConnection;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPSearchAttribute;
import com.xpn.xwiki.plugin.ldap.XWikiLDAPUtils;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;

/**
 * This class provides an authentication method that validates a user trough LDAP against a directory. It gives LDAP
 * users access if they belong to a particular group, creates XWiki users if they have never logged in before and
 * synchronizes membership to XWiki groups based on membership to LDAP groups.
 * 
 * @version $Id$
 * @since 1.3 M2
 */
public class XWikiLDAPAuthServiceImpl extends XWikiAuthServiceImpl
{
    /**
     * The XWiki space where users are stored.
     */
    private static final String XWIKI_USER_SPACE = "XWiki";

    /**
     * The name of the XWiki group member field.
     */
    private static final String XWIKI_GROUP_MEMBERFIELD = "member";

    /**
     * Separator between space name and document name in document full name.
     */
    private static final String XWIKI_SPACE_NAME_SEP = ".";

    /**
     * Default unique user field name.
     */
    private static final String LDAP_DEFAULT_UID = "cn";

    /**
     * Logging tool.
     */
    private static final Log LOG = LogFactory.getLog(XWikiLDAPAuthServiceImpl.class);

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl#authenticate(java.lang.String, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public Principal authenticate(String login, String password, XWikiContext context) throws XWikiException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Starting LDAP authentication");
        }

        /*
         * TODO: Put the next 4 following "if" in common with XWikiAuthService to ensure coherence This method was
         * returning null on failure so I preserved that behaviour, while adding the exact error messages to the context
         * given as argument. However, the right way to do this would probably be to throw XWikiException-s.
         */

        if (login == null) {
            // If we can't find the username field then we are probably on the login screen

            if (LOG.isDebugEnabled()) {
                LOG.debug("The provided user is null."
                    + " We don't try to authenticate, it probably means the user is in non logged mode.");
            }

            return null;
        }

        // Check for empty usernames
        if (login.equals("")) {
            context.put("message", "nousername");

            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP authentication failed: login empty");
            }

            return null;
        }

        // Check for empty passwords
        if ((password == null) || (password.trim().equals(""))) {
            context.put("message", "nopassword");

            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP authentication failed: password null or empty");
            }

            return null;
        }

        // Check for superadmin
        if (isSuperAdmin(login)) {
            return authenticateSuperAdmin(password, context);
        }

        // If we have the context then we are using direct mode
        // then we should specify the database
        // This is needed for virtual mode to work
        Principal principal = null;

        // Try authentication against ldap
        principal = ldapAuthenticate(login, password, context);

        if (principal == null) {
            // Fallback to local DB only if trylocal is true
            principal = xwikiAuthenticate(login, password, context);
        }

        if (LOG.isDebugEnabled()) {
            if (principal != null) {
                LOG.debug("LDAP authentication succeed with principal [" + principal.getName() + "]");
            } else {
                LOG.debug("LDAP authentication failed for user [" + login + "]");
            }
        }

        return principal;
    }

    /**
     * @param name the name to convert.
     * @return a valid XWiki user name:
     *         <ul>
     *         <li>Remove '.'</li>
     *         </ul>
     */
    protected String getValidXWikiUserName(String name)
    {
        return name.replace(XWIKI_SPACE_NAME_SEP, "");
    }

    /**
     * Try both local and global ldap login and return {@link Principal}.
     * 
     * @param login the name of the user to log in.
     * @param password the password of the user to log in.
     * @param context the XWiki context.
     * @return the {@link Principal}.
     */
    protected Principal ldapAuthenticate(String login, String password, XWikiContext context)
    {
        Principal principal = null;

        // Remove XWiki. prefix - not sure this is really a good idea or the right way to do it
        String ldapUid = login;
        int i = login.indexOf(XWIKI_USER_SPACE + XWIKI_SPACE_NAME_SEP);
        if (i != -1) {
            ldapUid = login.substring(i + 1);
        }

        String validXWikiUserName = getValidXWikiUserName(ldapUid);

        // First we check in the local context for a valid ldap user
        try {
            principal = ldapAuthenticateInContext(ldapUid, validXWikiUserName, password, context, true);
        } catch (Exception e) {
            // continue
            if (LOG.isDebugEnabled()) {
                LOG.debug("Local LDAP authentication failed.", e);
            }
        }

        // If local ldap failed, try global ldap
        if (principal == null && !context.isMainWiki()) {
            // Then we check in the main database
            String db = context.getDatabase();
            try {
                context.setDatabase(context.getMainXWiki());
                try {
                    principal = ldapAuthenticateInContext(ldapUid, validXWikiUserName, password, context, false);
                } catch (Exception e) {
                    // continue
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Global LDAP authentication failed.", e);
                    }
                }
            } finally {
                context.setDatabase(db);
            }
        }

        return principal;
    }

    /**
     * Try both local and global DB login if trylocal is true {@link Principal}.
     * 
     * @param ldapUid the name of the user to log in.
     * @param ldapPassword the password of the user to log in.
     * @param context the XWiki context.
     * @return the {@link Principal}.
     * @throws XWikiException error when checking user name and password.
     */
    protected Principal xwikiAuthenticate(String ldapUid, String ldapPassword, XWikiContext context)
        throws XWikiException
    {
        Principal principal = null;

        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        String trylocal = config.getLDAPParam("ldap_trylocal", "0", context);

        if ("1".equals(trylocal)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Trying authentication against XWiki DB");
            }

            principal = super.authenticate(ldapUid, ldapPassword, context);
        }

        return principal;
    }

    /**
     * Try LDAP login for given context and return {@link Principal}.
     * 
     * @param ldapUid the name of the ldap user to log in.
     * @param validXWikiUserName the name of the XWiki user to log in.
     * @param password the password of the user to log in.
     * @param context the XWiki context.
     * @return the {@link Principal}.
     * @throws XWikiException error when login.
     * @throws UnsupportedEncodingException error when login.
     * @throws LDAPException error when login.
     */
    protected Principal ldapAuthenticateInContext(String ldapUid, String validXWikiUserName, String password,
        XWikiContext context) throws XWikiException, UnsupportedEncodingException, LDAPException
    {
        return ldapAuthenticateInContext(ldapUid, validXWikiUserName, password, context, false);
    }

    /**
     * Try LDAP login for given context and return {@link Principal}.
     * 
     * @param ldapUid the name of the ldap user to log in.
     * @param validXWikiUserName the name of the XWiki user to log in.
     * @param password the password of the user to log in.
     * @param context the XWiki context.
     * @param local indicate if it's a local authentication. Supposed to return a local user {@link Principal} (whithout
     *            the wiki name).
     * @return the {@link Principal}.
     * @throws XWikiException error when login.
     * @throws UnsupportedEncodingException error when login.
     * @throws LDAPException error when login.
     */
    protected Principal ldapAuthenticateInContext(String ldapUid, String validXWikiUserName, String password,
        XWikiContext context, boolean local) throws XWikiException, UnsupportedEncodingException, LDAPException
    {
        Principal principal = null;

        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        XWikiLDAPConnection connector = new XWikiLDAPConnection();
        XWikiLDAPUtils ldapUtils = new XWikiLDAPUtils(connector);

        ldapUtils.setUidAttributeName(config.getLDAPParam(XWikiLDAPConfig.PREF_LDAP_UID, LDAP_DEFAULT_UID, context));
        ldapUtils.setGroupClasses(config.getGroupClasses(context));
        ldapUtils.setGroupMemberFields(config.getGroupMemberFields(context));
        ldapUtils.setBaseDN(config.getLDAPParam("ldap_base_DN", "", context));
        ldapUtils.setUserSearchFormatString(config.getLDAPParam("ldap_user_search_fmt", "({0}={1})", context));

        // ////////////////////////////////////////////////////////////////////
        // 1. check if ldap authentication is off => authenticate against db
        // ////////////////////////////////////////////////////////////////////

        if (!config.isLDAPEnabled(context)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP authentication failed: LDAP not activ");
            }

            return principal;
        }

        // ////////////////////////////////////////////////////////////////////
        // 2. bind to LDAP => if failed try db
        // ////////////////////////////////////////////////////////////////////

        if (!connector.open(ldapUid, password, context)) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                "Bind to LDAP server failed.");
        }

        // ////////////////////////////////////////////////////////////////////
        // 3. find XWiki user profile page
        // ////////////////////////////////////////////////////////////////////

        XWikiDocument userProfile = getUserProfileByUid(validXWikiUserName, ldapUid, context);

        // ////////////////////////////////////////////////////////////////////
        // 4. if group param, verify group membership (& get DN)
        // ////////////////////////////////////////////////////////////////////

        String ldapDn = null;
        String filterGroupDN = config.getLDAPParam("ldap_user_group", "", context);

        if (filterGroupDN.length() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking if the user belongs to the user group: " + filterGroupDN);
            }

            ldapDn = ldapUtils.isUidInGroup(ldapUid, filterGroupDN, context);

            if (ldapDn == null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                    "LDAP user {0} does not belong to LDAP group {1}.", null, new Object[] {ldapUid, filterGroupDN});
            }
        }

        // ////////////////////////////////////////////////////////////////////
        // 5. if exclude group param, verify group membership
        // ////////////////////////////////////////////////////////////////////

        String excludeGroupDN = config.getLDAPParam("ldap_exclude_group", "", context);

        if (excludeGroupDN.length() > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Checking if the user does not belongs to the exclude group: " + excludeGroupDN);
            }

            if (ldapUtils.isUidInGroup(ldapUid, excludeGroupDN, context) != null) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                    "LDAP user {0} should not belong to LDAP group {1}.", null, new Object[] {ldapUid, filterGroupDN});
            }
        }

        // ////////////////////////////////////////////////////////////////////
        // 6. if no dn search for user
        // ////////////////////////////////////////////////////////////////////

        List<XWikiLDAPSearchAttribute> searchAttributes = null;

        // if we still don't have a dn, search for it. Also get the attributes, we might need
        // them
        if (ldapDn == null) {
            searchAttributes = ldapUtils.searchUserAttributesByUid(ldapUid, getAttributeNameTable(context));

            if (searchAttributes != null) {
                for (XWikiLDAPSearchAttribute searchAttribute : searchAttributes) {
                    if ("dn".equals(searchAttribute.name)) {
                        ldapDn = searchAttribute.value;

                        break;
                    }
                }
            }
        }

        if (ldapDn == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                "Can't find LDAP user DN for [" + ldapUid + "]");
        }

        // ////////////////////////////////////////////////////////////////////
        // 7. apply validate_password property or if user used for LDAP connection is not the one
        // authenticated try to bind
        // ////////////////////////////////////////////////////////////////////

        if ("1".equals(config.getLDAPParam("ldap_validate_password", "0", context))) {
            String passwordField = config.getLDAPParam("ldap_password_field", "userPassword", context);
            if (!connector.checkPassword(ldapDn, password, passwordField)) {
                LOG.debug("Password comparison failed, are you really sure you need validate_password ?"
                    + " If you don't enable it, it does not mean user credentials are not validated."
                    + " The goal of this property is to bypass standard LDAP bind"
                    + " which is usually bad unless you really know what you do.");

                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                    "LDAP authentication failed:" + " could not validate the password: wrong password for " + ldapDn);
            }
        } else {
            String bindDNFormat = config.getLDAPBindDN(context);
            String bindDN = config.getLDAPBindDN(ldapUid, password, context);

            if (bindDNFormat.equals(bindDN)) {
                // Validate user credentials
                connector.bind(ldapDn, password);

                // Rebind admin user
                connector.bind(bindDN, config.getLDAPBindPassword(ldapUid, password, context));

            }
        }

        // ////////////////////////////////////////////////////////////////////
        // 8. sync user
        // ////////////////////////////////////////////////////////////////////

        boolean isNewUser = userProfile.isNew();

        syncUser(userProfile, searchAttributes, ldapDn, ldapUid, ldapUtils, context);

        // from now on we can enter the application
        if (local) {
            principal = new SimplePrincipal(userProfile.getFullName());
        } else {
            principal = new SimplePrincipal(context.getDatabase() + ":" + userProfile.getFullName());
        }

        // ////////////////////////////////////////////////////////////////////
        // 9. sync groups membership
        // ////////////////////////////////////////////////////////////////////

        try {
            syncGroupsMembership(userProfile.getFullName(), ldapDn, isNewUser, ldapUtils, context);
        } catch (XWikiException e) {
            LOG.error("Failed to synchronise user's groups membership", e);
        }

        return principal;
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
     * Update or create XWiki user base on LDAP.
     * 
     * @param userName the name of the user.
     * @param userDN the LDAP user DN.
     * @param searchAttributeListIn the attributes.
     * @param ldapUtils the LDAP communication tool.
     * @param context the XWiki context.
     * @throws XWikiException error when updating or creating XWiki user.
     */
    protected void syncUser(XWikiDocument userProfile, List<XWikiLDAPSearchAttribute> searchAttributeListIn,
        String ldapDn, String ldapUid, XWikiLDAPUtils ldapUtils, XWikiContext context) throws XWikiException
    {
        // check if we have to create the user
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        if (userProfile.isNew() || config.getLDAPParam("ldap_update_user", "0", context).equals("1")) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("LDAP attributes will be used to update XWiki attributes.");
            }

            List<XWikiLDAPSearchAttribute> searchAttributeList = searchAttributeListIn;

            // get attributes from LDAP if we don't already have them
            if (searchAttributeList == null) {
                // didn't get attributes before, so do it now
                searchAttributeList =
                    ldapUtils.getConnection().searchLDAP(ldapDn, null, getAttributeNameTable(context),
                        LDAPConnection.SCOPE_BASE);

                if (searchAttributeList == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.error("Can't find any attributes for user [" + ldapDn + "]");
                    }
                }
            }

            if (userProfile.isNew()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating new XWiki user based on LDAP attribues located at [" + ldapDn + "]");
                }

                userProfile = createUserFromLDAP(userProfile, searchAttributeList, ldapDn, ldapUid, context);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("New XWiki user created: [" + userProfile.getDocumentReference() + "]");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Updating existing user with LDAP attribues located at " + ldapDn);
                }

                try {
                    updateUserFromLDAP(userProfile, searchAttributeList, ldapDn, ldapUid, context);
                } catch (XWikiException e) {
                    LOG.error("Failed to synchronise user's informations", e);
                }
            }
        }
    }

    /**
     * Synchronize user XWiki membership with it's LDAP membership.
     * 
     * @param xwikiUserName the name of the user.
     * @param ldapDn the LDAP DN of the user.
     * @param createuser indicate if the user is created or updated.
     * @param ldapUtils the LDAP communication tool.
     * @param context the XWiki context.
     * @throws XWikiException error when synchronizing user membership.
     */
    protected void syncGroupsMembership(String xwikiUserName, String ldapDn, boolean createuser,
        XWikiLDAPUtils ldapUtils, XWikiContext context) throws XWikiException
    {
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        // got valid group mappings
        Map<String, Set<String>> groupMappings = config.getGroupMappings(context);

        // update group membership, join and remove from given groups
        // sync group membership for this user
        if (groupMappings.size() > 0) {
            // flag if always sync or just on create of the user
            String syncmode = config.getLDAPParam("ldap_mode_group_sync", "always", context);

            if (!syncmode.equalsIgnoreCase("create") || createuser) {
                syncGroupsMembership(xwikiUserName, ldapDn, groupMappings, ldapUtils, context);
            }
        }
    }

    /**
     * Synchronize user XWiki membership with it's LDAP membership.
     * 
     * @param xwikiUserName the name of the user.
     * @param userDN the LDAP DN of the user.
     * @param groupMappings the mapping between XWiki groups names and LDAP groups names.
     * @param ldapUtils the LDAP communication tool.
     * @param context the XWiki context.
     * @throws XWikiException error when synchronizing user membership.
     */
    protected void syncGroupsMembership(String xwikiUserName, String userDN, Map<String, Set<String>> groupMappings,
        XWikiLDAPUtils ldapUtils, XWikiContext context) throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating group membership for the user: " + xwikiUserName);
        }

        Collection<String> xwikiUserGroupList =
            context.getWiki().getGroupService(context).getAllGroupsNamesForMember(xwikiUserName, 0, 0, context);

        if (LOG.isDebugEnabled()) {
            LOG.debug("The user belongs to following XWiki groups: ");
            for (String userGroupName : xwikiUserGroupList) {
                LOG.debug(userGroupName);
            }
        }

        // go through mapped groups to locate the user
        for (Map.Entry<String, Set<String>> entry : groupMappings.entrySet()) {
            String xwikiGrouNamep = entry.getKey();
            Set<String> groupDNSet = entry.getValue();

            if (xwikiUserGroupList.contains(xwikiGrouNamep)) {
                if (!ldapUtils.isMemberOfGroups(userDN, groupDNSet, context)) {
                    removeUserFromXWikiGroup(xwikiUserName, xwikiGrouNamep, context);
                }
            } else {
                if (ldapUtils.isMemberOfGroups(userDN, groupDNSet, context)) {
                    addUserToXWikiGroup(xwikiUserName, xwikiGrouNamep, context);
                }
            }
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
            if (LOG.isDebugEnabled()) {
                LOG.debug(MessageFormat.format("Adding user {0} to xwiki group {1}", xwikiUserName, groupName));
            }

            BaseClass groupClass = context.getWiki().getGroupClass(context);

            // Get document representing group
            XWikiDocument groupDoc = context.getWiki().getDocument(groupName, context);

            // Add a member object to document
            BaseObject memberObj = groupDoc.newXObject(groupClass.getDocumentReference(), context);
            Map<String, String> map = new HashMap<String, String>();
            map.put(XWIKI_GROUP_MEMBERFIELD, xwikiUserName);
            groupClass.fromMap(map, memberObj);

            // Save modifications
            context.getWiki().saveDocument(groupDoc, context);

            if (LOG.isDebugEnabled()) {
                LOG.debug(MessageFormat.format("Finished adding user {0} to xwiki group {1}", xwikiUserName, groupName));
            }

        } catch (Exception e) {
            LOG.error(MessageFormat.format("Failed to add a user [{0}] to a group [{1}]", xwikiUserName, groupName), e);
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

            // Get and remove the specific group membership object for the user
            BaseObject groupObj =
                groupDoc.getXObject(groupClass.getDocumentReference(), XWIKI_GROUP_MEMBERFIELD, xwikiUserName);
            groupDoc.removeXObject(groupObj);

            // Save modifications
            context.getWiki().saveDocument(groupDoc, context);
        } catch (Exception e) {
            LOG.error("Failed to remove a user from a group " + xwikiUserName + " group: " + groupName, e);
        }
    }

    /**
     * Sets attributes on the user object based on attribute values provided by the LDAP.
     * 
     * @param xwikiUserName the XWiki user name.
     * @param searchAttributes the attributes.
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Start synchronising LDAP profile [" + searchAttributes + "] with user profile based on mapping "
                + userMappings);
        }

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
     * Create an XWiki user and set all mapped attributes from LDAP to XWiki attributes.
     * 
     * @param userProfile the XWiki user profile.
     * @param searchAttributes the attributes.
     * @param ldapDN the LDAP DN of the user.
     * @param ldapUid the LDAP unique id of the user.
     * @param context the XWiki context.
     * @return the created user.
     * @throws XWikiException error when creating XWiki user.
     */
    protected XWikiDocument createUserFromLDAP(XWikiDocument userProfile,
        List<XWikiLDAPSearchAttribute> searchAttributes, String ldapDN, String ldapUid, XWikiContext context)
        throws XWikiException
    {
        XWikiLDAPConfig config = XWikiLDAPConfig.getInstance();

        Map<String, String> userMappings = config.getUserMappings(null, context);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Start synchronising LDAP profile [" + searchAttributes + "] with user profile based on mapping "
                + userMappings);
        }

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

        context.getWiki().createUser(userProfile.getName(), map, context);

        // Update ldap profile object
        XWikiDocument createdUserProfile = context.getWiki().getDocument(userProfile.getDocumentReference(), context);
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        if (ldapXClass.updateLDAPObject(createdUserProfile, ldapDN, ldapUid)) {
            context.getWiki().saveDocument(createdUserProfile, context);
        }

        return createdUserProfile;
    }

    protected XWikiDocument getUserProfileByUid(String validXWikiUserName, String ldapUid, XWikiContext context)
        throws XWikiException
    {
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        // Try default profile name (generally in the cache)
        XWikiDocument userProfile =
            context.getWiki().getDocument(
                new DocumentReference(context.getDatabase(), XWIKI_USER_SPACE, validXWikiUserName), context);

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

    protected XWikiDocument getAvailableUserProfile(String validXWikiUserName, String ldapUid, XWikiContext context)
        throws XWikiException
    {
        BaseClass userClass = context.getWiki().getUserClass(context);
        LDAPProfileXClass ldapXClass = new LDAPProfileXClass(context);

        DocumentReference userReference =
            new DocumentReference(context.getDatabase(), XWIKI_USER_SPACE, validXWikiUserName);

        // Check if the default profile document is available
        for (int i = 0; true; ++i) {
            if (i > 0) {
                userReference.setName(validXWikiUserName + "_" + i);
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
