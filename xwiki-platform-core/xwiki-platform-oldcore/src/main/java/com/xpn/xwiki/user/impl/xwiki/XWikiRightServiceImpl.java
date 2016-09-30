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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Default implementation of {@link XWikiRightService}.
 *
 * @version $Id$
 * @deprecated since 4.0, use XWikiCachingRightService instead
 */
@Deprecated
public class XWikiRightServiceImpl implements XWikiRightService
{
    public static final EntityReference RIGHTCLASS_REFERENCE = new EntityReference("XWikiRights", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    public static final EntityReference GLOBALRIGHTCLASS_REFERENCE = new EntityReference("XWikiGlobalRights",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRightServiceImpl.class);

    private static final EntityReference XWIKIPREFERENCES_REFERENCE = new EntityReference("XWikiPreferences",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    private static final List<String> ALLLEVELS = Arrays.asList("admin", "view", "edit", "comment", "delete",
        "undelete", "register", "programming");

    private static final EntityReference DEFAULTUSERSPACE = new EntityReference("XWiki", EntityType.SPACE);

    private static Map<String, String> actionMap;

    /**
     * Used to convert a string into a proper Document Reference.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /**
     * Used to convert a proper Document Name to string.
     */
    private EntityReferenceSerializer<String> entityReferenceSerializer = Utils
        .getComponent(EntityReferenceSerializer.TYPE_STRING);

    protected void logAllow(String username, String page, String action, String info)
    {
        LOGGER.debug("Access has been granted for ([{}], [{}], [{}]): [{}]", username, page, action, info);
    }

    protected void logDeny(String username, String page, String action, String info)
    {
        LOGGER.info("Access has been denied for ([{}], [{}], [{}]): [{}]", username, page, action, info);
    }

    protected void logDeny(String name, String resourceKey, String accessLevel, String info, Exception e)
    {
        LOGGER.debug("Access has been denied for ([{}], [{}], [{}]) at [{}]", name, resourceKey, accessLevel, info, e);
    }

    @Override
    public List<String> listAllLevels(XWikiContext context) throws XWikiException
    {
        return new ArrayList<String>(ALLLEVELS);
    }

    public String getRight(String action)
    {
        if (actionMap == null) {
            actionMap = new HashMap<String, String>();
            actionMap.put("login", "login");
            actionMap.put("logout", "login");
            actionMap.put("loginerror", "login");
            actionMap.put("loginsubmit", "login");
            actionMap.put("view", "view");
            actionMap.put("viewrev", "view");
            actionMap.put("get", "view");
            actionMap.put("downloadrev", "view");
            actionMap.put("plain", "view");
            actionMap.put("raw", "view");
            actionMap.put("attach", "view");
            actionMap.put("charting", "view");
            actionMap.put("skin", "view");
            actionMap.put("download", "view");
            actionMap.put("dot", "view");
            actionMap.put("svg", "view");
            actionMap.put("pdf", "view");
            actionMap.put("delete", "delete");
            actionMap.put("deletespace", "admin");
            actionMap.put("deleteversions", "admin");
            actionMap.put("undelete", "undelete");
            actionMap.put("reset", "delete");
            actionMap.put("commentadd", "comment");
            actionMap.put("commentsave", "comment");
            actionMap.put("register", "register");
            actionMap.put("redirect", "view");
            actionMap.put("admin", "admin");
            actionMap.put("export", "view");
            actionMap.put("import", "admin");
            actionMap.put("jsx", "view");
            actionMap.put("ssx", "view");
            actionMap.put("tex", "view");
            actionMap.put("create", "edit");
            actionMap.put("temp", "view");
            actionMap.put("unknown", "view");
        }

        String right = actionMap.get(action);
        if (right == null) {
            return "edit";
        } else {
            return right;
        }
    }

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        LOGGER.debug("checkAccess for [{}], [{}]", action, doc);

        String username = null;
        XWikiUser user = null;
        boolean needsAuth = false;
        String right = getRight(action);

        if (right.equals("login")) {
            user = context.getWiki().checkAuth(context);
            if (user == null) {
                username = XWikiRightService.GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }

            // Save the user
            context.setUser(username);
            logAllow(username, doc.getFullName(), action, "login/logout pages");

            return true;
        }

        if (right.equals("delete")) {
            user = context.getWiki().checkAuth(context);
            String creator = doc.getCreator();
            if ((user != null) && (user.getUser() != null) && (creator != null)) {
                if (user.getUser().equals(creator)) {
                    context.setUser(user.getUser());

                    return true;
                }
            }
        }

        // We do not need to authenticate twice
        // This seems to cause a problem in virtual wikis
        user = context.getXWikiUser();
        if (user == null) {
            needsAuth = needsAuth(right, context);
            try {
                if (context.getMode() != XWikiContext.MODE_XMLRPC) {
                    user = context.getWiki().checkAuth(context);
                } else {
                    user = new XWikiUser(context.getUser());
                }

                if ((user == null) && (needsAuth)) {
                    logDeny("unauthentified", doc.getFullName(), action, "Authentication needed");
                    if (context.getRequest() != null) {
                        if (!context.getWiki().Param("xwiki.hidelogin", "false").equalsIgnoreCase("true")) {
                            context.getWiki().getAuthService().showLogin(context);
                        }
                    }

                    return false;
                }
            } catch (XWikiException e) {
                if (needsAuth) {
                    throw e;
                }
            }

            if (user == null) {
                username = XWikiRightService.GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }

            // Save the user
            context.setUser(username);
        } else {
            username = user.getUser();
        }

        // Check Rights
        try {
            // Verify access rights and return if ok
            String docname;
            if (context.getWikiId() != null) {
                docname = context.getWikiId() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1) {
                    username = context.getWikiId() + ":" + username;
                }
            } else {
                docname = doc.getFullName();
            }

            if (context.getWiki().getRightService().hasAccessLevel(right, username, docname, context)) {
                logAllow(username, docname, action, "access manager granted right");

                return true;
            }
        } catch (Exception e) {
            // This should not happen..
            logDeny(username, doc.getFullName(), action, "access manager exception " + e.getMessage());
            e.printStackTrace();

            return false;
        }

        if (user == null) {
            // Denied Guest need to be authenticated
            logDeny("unauthentified", doc.getFullName(), action, "Guest has been denied");
            if (context.getRequest() != null
                && !context.getWiki().Param("xwiki.hidelogin", "false").equalsIgnoreCase("true")) {
                context.getWiki().getAuthService().showLogin(context);
            }

            return false;
        } else {
            logDeny(username, doc.getFullName(), action, "access manager denied right");

            return false;
        }
    }

    private boolean needsAuth(String right, XWikiContext context)
    {
        boolean needsAuth = false;

        try {
            needsAuth =
                context.getWiki().getXWikiPreference("authenticate_" + right, "", context).toLowerCase().equals("yes");
        } catch (Exception e) {
        }

        try {
            needsAuth |= (context.getWiki().getXWikiPreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }

        try {
            needsAuth |=
                context.getWiki().getSpacePreference("authenticate_" + right, "", context).toLowerCase().equals("yes");
        } catch (Exception e) {
        }

        try {
            needsAuth |= (context.getWiki().getSpacePreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }

        return needsAuth;
    }

    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        try {
            return hasAccessLevel(right, username, docname, true, context);
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean checkRight(String userOrGroupName, XWikiDocument doc, String accessLevel, boolean user,
        boolean allow, boolean global, XWikiContext context) throws XWikiRightNotFoundException, XWikiException
    {
        if (!global && ("admin".equals(accessLevel))) {
            // Admin rights do not exist at document level.
            throw new XWikiRightNotFoundException();
        }

        EntityReference rightClassReference = global ? GLOBALRIGHTCLASS_REFERENCE : RIGHTCLASS_REFERENCE;
        String fieldName = user ? "users" : "groups";
        boolean found = false;

        // Here entity is either a user or a group
        DocumentReference userOrGroupDocumentReference =
            this.currentMixedDocumentReferenceResolver.resolve(userOrGroupName);
        String prefixedFullName = this.entityReferenceSerializer.serialize(userOrGroupDocumentReference);
        String shortname = userOrGroupName;
        int i0 = userOrGroupName.indexOf(":");
        if (i0 != -1) {
            shortname = userOrGroupName.substring(i0 + 1);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Checking right: [{}], [{}], [{}], [{}], [{}], [{}]", userOrGroupName, doc.getFullName(),
                accessLevel, user, allow, global);
        }

        List<BaseObject> rightObjects = doc.getXObjects(rightClassReference);
        if (rightObjects != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking objects [{}]", rightObjects.size());
            }

            for (int i = 0; i < rightObjects.size(); i++) {
                LOGGER.debug("Checking object [{}]", i);

                BaseObject bobj = rightObjects.get(i);

                if (bobj == null) {
                    LOGGER.debug("Bypass object [{}]", i);
                    continue;
                }

                String users = bobj.getStringValue(fieldName);
                String levels = bobj.getStringValue("levels");
                boolean allowdeny = (bobj.getIntValue("allow") == 1);

                if (allowdeny == allow) {
                    LOGGER.debug("Checking match: [{}] in [{}]", accessLevel, levels);

                    String[] levelsarray = StringUtils.split(levels, " ,|");
                    if (ArrayUtils.contains(levelsarray, accessLevel)) {
                        LOGGER.debug("Found a right for [{}]", allow);
                        found = true;

                        LOGGER.debug("Checking match: [{}] in [{}]", userOrGroupName, users);

                        String[] userarray = GroupsClass.getListFromString(users).toArray(new String[0]);

                        for (int ii = 0; ii < userarray.length; ii++) {
                            String value = userarray[ii];
                            if (value.indexOf(".") == -1) {
                                userarray[ii] = "XWiki." + value;
                            }
                        }

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Checking match: [{}] in [{}]", userOrGroupName,
                                StringUtils.join(userarray, ","));
                        }

                        // In the case where the document database and the user database is the same
                        // then we allow the usage of the short name, otherwise the fully qualified
                        // name is requested
                        if (doc.getWikiName().equals(userOrGroupDocumentReference.getWikiReference().getName())) {
                            if (ArrayUtils.contains(userarray, shortname)) {
                                LOGGER.debug("Found matching right in [{}] for [{}]", users, shortname);
                                return true;
                            }

                            // We should also allow to skip "XWiki." from the usernames and group
                            // lists
                            String veryshortname = shortname.substring(shortname.indexOf(".") + 1);
                            if (ArrayUtils.contains(userarray, veryshortname)) {
                                LOGGER.debug("Found matching right in [{}] for [{}]", users, shortname);
                                return true;
                            }
                        }

                        if ((context.getWikiId() != null) && (ArrayUtils.contains(userarray, userOrGroupName))) {
                            LOGGER.debug("Found matching right in [{}] for [{}]", users, userOrGroupName);
                            return true;
                        }

                        LOGGER.debug("Failed match: [{}] in [{}]", userOrGroupName, users);
                    }
                } else {
                    LOGGER.debug("Bypass object [{}] because wrong allow/deny", i);
                }
            }
        }

        LOGGER.debug("Searching for matching rights at group level");

        // Didn't found right at this level.. Let's go to group level
        Map<String, Collection<String>> grouplistcache = (Map<String, Collection<String>>) context.get("grouplist");
        if (grouplistcache == null) {
            grouplistcache = new HashMap<String, Collection<String>>();
            context.put("grouplist", grouplistcache);
        }

        Collection<String> grouplist = new HashSet<String>();

        // Get member groups from document's wiki
        addMemberGroups(doc.getWikiName(), prefixedFullName, userOrGroupDocumentReference, grouplist, context);

        // Get member groups from member's wiki
        if (!context.getWikiId().equalsIgnoreCase(userOrGroupDocumentReference.getWikiReference().getName())) {
            addMemberGroups(userOrGroupDocumentReference.getWikiReference().getName(), prefixedFullName,
                userOrGroupDocumentReference, grouplist, context);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Searching for matching rights for [{}] groups: [{}]", grouplist.size(), grouplist);
        }

        for (String group : grouplist) {
            try {
                // We need to construct the full group name to make sure the groups are
                // handled separately
                boolean result = checkRight(group, doc, accessLevel, false, allow, global, context);
                if (result) {
                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            } catch (Exception e) {
                LOGGER.error("Failed to check right [{}] for group [{}] on document [¶}]", accessLevel, group,
                    doc.getPrefixedFullName(), e);
            }
        }

        LOGGER.debug("Finished searching for rights for [{}]: [{}]", userOrGroupName, found);

        if (found) {
            return false;
        } else {
            throw new XWikiRightNotFoundException();
        }
    }

    private void addMemberGroups(String wiki, String prefixedFullName, DocumentReference userOrGroupDocumentReference,
        Collection<String> grouplist, XWikiContext context) throws XWikiException
    {
        XWikiGroupService groupService = context.getWiki().getGroupService(context);

        Map<String, Collection<String>> grouplistcache = (Map<String, Collection<String>>) context.get("grouplist");
        if (grouplistcache == null) {
            grouplistcache = new HashMap<String, Collection<String>>();
            context.put("grouplist", grouplistcache);
        }

        // the key is for the entity <code>prefixedFullName</code> in current wiki
        String key = wiki + ":" + prefixedFullName;

        Collection<String> tmpGroupList = grouplistcache.get(key);
        if (tmpGroupList == null) {
            String currentWiki = context.getWikiId();
            try {
                context.setWikiId(wiki);

                Collection<DocumentReference> groupReferences =
                    groupService.getAllGroupsReferencesForMember(userOrGroupDocumentReference, 0, 0, context);

                tmpGroupList = new ArrayList<String>(groupReferences.size());
                for (DocumentReference groupReference : groupReferences) {
                    tmpGroupList.add(this.entityReferenceSerializer.serialize(groupReference));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get groups for user or group [{}] in wiki [{}]", prefixedFullName, wiki, e);
                tmpGroupList = Collections.emptyList();
            } finally {
                context.setWikiId(currentWiki);
            }

            grouplistcache.put(key, tmpGroupList);
        }

        grouplist.addAll(tmpGroupList);
    }

    public boolean hasAccessLevel(String accessLevel, String userOrGroupName, String entityReference, boolean user,
        XWikiContext context) throws XWikiException
    {
        LOGGER.debug("hasAccessLevel for [{}], [{}], [{}]", accessLevel, userOrGroupName, entityReference);

        DocumentReference userOrGroupNameReference =
            this.currentMixedDocumentReferenceResolver.resolve(userOrGroupName);

        if (!userOrGroupNameReference.getName().equals(XWikiRightService.GUEST_USER) && context.getWikiId() != null) {
            // Make sure to have the prefixed full name of the user or group
            userOrGroupName =
                this.entityReferenceSerializer.serialize(this.currentMixedDocumentReferenceResolver.resolve(
                    userOrGroupName, DEFAULTUSERSPACE));

            // Make sure to have the prefixed full name of the resource
            entityReference =
                this.entityReferenceSerializer.serialize(this.currentMixedDocumentReferenceResolver
                    .resolve(entityReference));
        }

        boolean deny = false;
        boolean allow = false;
        boolean allow_found = false;
        boolean deny_found = false;
        boolean isReadOnly = context.getWiki().isReadOnly();
        String database = context.getWikiId();
        XWikiDocument currentdoc = null;

        if (isReadOnly) {
            if ("edit".equals(accessLevel) || "delete".equals(accessLevel) || "undelete".equals(accessLevel)
                || "comment".equals(accessLevel) || "register".equals(accessLevel)) {
                logDeny(userOrGroupName, entityReference, accessLevel, "server in read-only mode");

                return false;
            }
        }

        if (userOrGroupNameReference.getName().equals(XWikiRightService.GUEST_USER)) {
            if (needsAuth(accessLevel, context)) {
                return false;
            }
        }

        // Fast return for delete right: allow the creator to delete the document
        if (accessLevel.equals("delete") && user) {
            currentdoc = context.getWiki().getDocument(entityReference, context);
            DocumentReference creator = currentdoc.getCreatorReference();
            if (ObjectUtils.equals(userOrGroupNameReference, creator)) {
                logAllow(userOrGroupName, entityReference, accessLevel, "delete right from document ownership");
                return true;
            }
        }

        allow = isSuperAdminOrProgramming(userOrGroupName, entityReference, accessLevel, user, context);
        if ((allow == true) || (accessLevel.equals("programming"))) {
            return allow;
        }

        try {
            currentdoc = currentdoc == null ? context.getWiki().getDocument(entityReference, context) : currentdoc;

            DocumentReference docReference = currentdoc.getDocumentReference();

            if (accessLevel.equals("edit")
                && (docReference.getName().equals("WebPreferences") || (docReference.getLastSpaceReference().getName()
                    .equals("XWiki") && docReference.getName().equals("XWikiPreferences")))) {
                // Since edit rights on these documents would be sufficient for a user to elevate himself to
                // admin or even programmer, we will instead check for admin access on these documents.
                // See http://jira.xwiki.org/browse/XWIKI-6987 and http://jira.xwiki.org/browse/XWIKI-2184.
                accessLevel = "admin";
            }

            // We need to make sure we are in the context of the document which rights is being checked
            context.setWikiId(currentdoc.getDatabase());

            // Verify Wiki Owner
            String wikiOwner = context.getWiki().getWikiOwner(currentdoc.getDatabase(), context);
            if (wikiOwner != null) {
                if (wikiOwner.equals(userOrGroupName)) {
                    logAllow(userOrGroupName, entityReference, accessLevel, "admin level from wiki ownership");

                    return true;
                }
            }

            XWikiDocument entityWikiPreferences = context.getWiki().getDocument(XWIKIPREFERENCES_REFERENCE, context);

            // Verify XWiki register right
            if (accessLevel.equals("register")) {
                try {
                    allow = checkRight(userOrGroupName, entityWikiPreferences, "register", user, true, true, context);
                    if (allow) {
                        logAllow(userOrGroupName, entityReference, accessLevel, "register level");

                        return true;
                    } else {
                        logDeny(userOrGroupName, entityReference, accessLevel, "register level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                    try {
                        deny =
                            checkRight(userOrGroupName, entityWikiPreferences, "register", user, false, true, context);
                        if (deny) {
                            return false;
                        }
                    } catch (XWikiRightNotFoundException e1) {
                    }
                }

                logAllow(userOrGroupName, entityReference, accessLevel, "register level (no right found)");

                return true;
            }

            int maxRecursiveSpaceChecks = context.getWiki().getMaxRecursiveSpaceChecks(context);
            boolean isSuperUser =
                isSuperUser(accessLevel, userOrGroupName, entityReference, user, entityWikiPreferences,
                    maxRecursiveSpaceChecks, context);
            if (isSuperUser) {
                logAllow(userOrGroupName, entityReference, accessLevel, "admin level");

                return true;
            }

            // check has deny rights
            if (hasDenyRights()) {
                // First check if this document is denied to the specific user
                entityReference = Util.getName(entityReference, context);
                try {
                    currentdoc =
                        currentdoc == null ? context.getWiki().getDocument(entityReference, context) : currentdoc;
                    deny = checkRight(userOrGroupName, currentdoc, accessLevel, user, false, false, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(userOrGroupName, entityReference, accessLevel, "document level");
                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            try {
                currentdoc = currentdoc == null ? context.getWiki().getDocument(entityReference, context) : currentdoc;
                allow = checkRight(userOrGroupName, currentdoc, accessLevel, user, true, false, context);
                allow_found = true;
                if (allow) {
                    logAllow(userOrGroupName, entityReference, accessLevel, "document level");

                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            }

            // Check if this document is denied/allowed
            // through the space WebPreferences Global Rights

            String space = currentdoc.getSpace();
            ArrayList<String> spacesChecked = new ArrayList<String>();
            int recursiveSpaceChecks = 0;
            while ((space != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
                // Add one to the recursive space checks
                recursiveSpaceChecks++;
                // add to list of spaces already checked
                spacesChecked.add(space);
                XWikiDocument webdoc = context.getWiki().getDocument(space, "WebPreferences", context);
                if (!webdoc.isNew()) {
                    if (hasDenyRights()) {
                        try {
                            deny = checkRight(userOrGroupName, webdoc, accessLevel, user, false, true, context);
                            deny_found = true;
                            if (deny) {
                                logDeny(userOrGroupName, entityReference, accessLevel, "web level");

                                return false;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // If a right was found at the previous level
                    // then we cannot check the web rights anymore
                    if (!allow_found) {
                        try {
                            allow = checkRight(userOrGroupName, webdoc, accessLevel, user, true, true, context);
                            allow_found = true;
                            if (allow) {
                                logAllow(userOrGroupName, entityReference, accessLevel, "web level");

                                return true;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // find the parent web to check rights on it
                    space = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                    if ((space == null) || (space.trim().equals("")) || spacesChecked.contains(space)) {
                        // no parent space or space already checked (recursive loop). let's finish
                        // the loop
                        space = null;
                    }
                } else {
                    // let's finish the loop
                    space = null;
                }
            }

            // Check if this document is denied/allowed
            // through the XWiki.XWikiPreferences Global Rights
            if (hasDenyRights()) {
                try {
                    deny = checkRight(userOrGroupName, entityWikiPreferences, accessLevel, user, false, true, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(userOrGroupName, entityReference, accessLevel, "xwiki level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If a right was found at the document or web level
            // then we cannot check the web rights anymore
            if (!allow_found) {
                try {
                    allow = checkRight(userOrGroupName, entityWikiPreferences, accessLevel, user, true, true, context);
                    allow_found = true;
                    if (allow) {
                        logAllow(userOrGroupName, entityReference, accessLevel, "xwiki level");

                        return true;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If neither doc, web or topic had any allowed ACL
            // and that all users that were not denied
            // should be allowed.
            if (!allow_found) {
                // Delete must be denied by default.
                if ("delete".equals(accessLevel)) {
                    if (hasAccessLevel("admin", userOrGroupName, entityReference, user, context)) {
                        logAllow(userOrGroupName, entityReference, accessLevel,
                            "admin rights imply delete on empty wiki");
                        return true;
                    }
                    logDeny(userOrGroupName, entityReference, accessLevel,
                        "global level (delete right must be explicit)");

                    return false;
                } else {
                    logAllow(userOrGroupName, entityReference, accessLevel, "global level (no restricting right)");

                    return true;
                }
            } else {
                logDeny(userOrGroupName, entityReference, accessLevel, "global level (restricting right was found)");

                return false;
            }

        } catch (XWikiException e) {
            logDeny(userOrGroupName, entityReference, accessLevel, "global level (exception)", e);
            e.printStackTrace();

            return false;
        } finally {
            context.setWikiId(database);
        }
    }

    private boolean hasDenyRights()
    {
        return true;
    }

    /**
     * @param username Any flavor of username. Examples: "xwiki:XWiki.superadmin", "XWiki.superAdmin", "superadmin", etc
     * @return true if the username is that of the superadmin (whatever the case) or false otherwise
     */
    // TODO: this method is a candidate for the the XWikiRightService API.
    private boolean isSuperAdmin(String username)
    {
        // Note 1: we use the default document reference resolver here but it doesn't matter since we only care about
        // the resolved page name.
        // Note 2: we use a resolver since the passed username could contain the wiki and/or space too and we want
        // to retrieve only the page name
        DocumentReference userReference =
            Utils.<DocumentReferenceResolver<String>>getComponent(DocumentReferenceResolver.TYPE_STRING).resolve(
                username);
        return StringUtils.equalsIgnoreCase(userReference.getName(), SUPERADMIN_USER);
    }

    private boolean isSuperAdminOrProgramming(String name, String resourceKey, String accessLevel, boolean user,
        XWikiContext context) throws XWikiException
    {
        if (name == null) {
            return false;
        }

        String database = context.getWikiId();
        boolean allow;

        if (isSuperAdmin(name)) {
            logAllow(name, resourceKey, accessLevel, "super admin level");
            return true;
        }

        try {
            // The master user and programming rights are checked in the main wiki
            context.setWikiId(context.getMainXWiki());
            XWikiDocument xwikimasterdoc = context.getWiki().getDocument(XWIKIPREFERENCES_REFERENCE, context);
            // Verify XWiki Master super user
            try {
                allow = checkRight(name, xwikimasterdoc, "admin", true, true, true, context);
                if (allow) {
                    logAllow(name, resourceKey, accessLevel, "master admin level");
                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            }

            // Verify XWiki programming right
            if (accessLevel.equals("programming")) {
                // Programming right can only been given if user is from main wiki
                if (!name.startsWith(context.getMainXWiki() + ":")) {
                    return false;
                }

                try {
                    allow = checkRight(name, xwikimasterdoc, "programming", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "programming level");

                        return true;
                    } else {
                        logDeny(name, resourceKey, accessLevel, "programming level");

                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }

                logDeny(name, resourceKey, accessLevel, "programming level (no right found)");

                return false;
            }
        } finally {
            // The next rights are checked in the virtual wiki
            context.setWikiId(database);
        }

        return false;
    }

    private boolean isSuperUser(String accessLevel, String name, String resourceKey, boolean user,
        XWikiDocument xwikidoc, int maxRecursiveSpaceChecks, XWikiContext context) throws XWikiException
    {
        boolean allow;

        // Verify XWiki super user
        try {
            allow = checkRight(name, xwikidoc, "admin", user, true, true, context);
            if (allow) {
                logAllow(name, resourceKey, accessLevel, "admin level");

                return true;
            }
        } catch (XWikiRightNotFoundException e) {
        }

        XWikiDocument documentName = new XWikiDocument();
        documentName.setFullName(resourceKey);

        // Verify Web super user
        String space = documentName.getSpace();
        ArrayList<String> spacesChecked = new ArrayList<String>();
        int recursiveSpaceChecks = 0;
        while ((space != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
            // Add one to the recursive space checks
            recursiveSpaceChecks++;
            // add to list of spaces already checked
            spacesChecked.add(space);
            XWikiDocument webdoc = context.getWiki().getDocument(space, "WebPreferences", context);
            if (!webdoc.isNew()) {
                try {
                    allow = checkRight(name, webdoc, "admin", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "web admin level");
                        return true;
                    }
                } catch (XWikiRightNotFoundException e) {
                }

                // find the parent web to check rights on it
                space = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                if ((space == null) || (space.trim().equals("")) || spacesChecked.contains(space)) {
                    // no parent space or space already checked (recursive loop). let's finish the
                    // loop
                    space = null;
                }
            } else {
                space = null;
            }
        }

        return false;
    }

    @Override
    public boolean hasProgrammingRights(XWikiContext context)
    {
        // Once dropPermissions has been called, the document in the
        // context cannot have programming permission.
        if (context.hasDroppedPermissions()) {
            return false;
        }
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        if (sdoc == null) {
            sdoc = context.getDoc();
        }

        return hasProgrammingRights(sdoc, context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            if (doc == null) {
                // If no context document is set, then check the rights of the current user
                return isSuperAdminOrProgramming(this.entityReferenceSerializer.serialize(context.getUserReference()),
                    null, "programming", true, context);
            }

            String username = doc.getContentAuthor();

            if (username == null) {
                return false;
            }

            String docname;
            if (doc.getDatabase() != null) {
                docname = doc.getDatabase() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1) {
                    username = doc.getDatabase() + ":" + username;
                }
            } else {
                docname = doc.getFullName();
            }

            // programming rights can only been given for user of the main wiki
            // FIXME: Isn't this wrong? The main db is context.getMainWikiName(), not context.getWiki().getDatabase()
            // (which is the current db).
            String maindb = context.getWiki().getDatabase();
            if ((maindb == null) || (!username.startsWith(maindb))) {
                return false;
            }

            return hasAccessLevel("programming", username, docname, context);
        } catch (Exception e) {
            LOGGER.error("Failed to check programming right for document [{}]", doc.getPrefixedFullName(), e);

            return false;
        }
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        boolean hasAdmin = hasWikiAdminRights(context);

        if (!hasAdmin) {
            try {
                hasAdmin =
                    hasAccessLevel("admin", context.getUser(), context.getDoc().getSpace() + ".WebPreferences", context);
            } catch (Exception e) {
                LOGGER.error("Failed to check space admin right for user [{}]", context.getUser(), e);
            }
        }

        return hasAdmin;
    }

    @Override
    public boolean hasWikiAdminRights(XWikiContext context)
    {
        try {
            return hasAccessLevel("admin", context.getUser(), "XWiki.XWikiPreferences", context);
        } catch (Exception e) {
            LOGGER.error("Failed to check wiki admin right for user [{}]", context.getUser(), e);
            return false;
        }
    }

}
