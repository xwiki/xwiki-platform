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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;

public class XWikiRightServiceImpl implements XWikiRightService
{
    private static final Log log = LogFactory.getLog(XWikiRightServiceImpl.class);

    private static Map actionMap;

    private static List allLevels =
        Arrays.asList(new String[] {"admin", "view", "edit", "comment", "delete", "undelete",
        "register", "programming"});

    protected void logAllow(String username, String page, String action, String info)
    {
        if (log.isDebugEnabled())
            log.debug("Access has been granted for (" + username + "," + page + "," + action
                + "): " + info);
    }

    protected void logDeny(String username, String page, String action, String info)
    {
        if (log.isInfoEnabled())
            log.info("Access has been denied for (" + username + "," + page + "," + action
                + "): " + info);
    }

    protected void logDeny(String name, String resourceKey, String accessLevel, String info,
        Exception e)
    {
        if (log.isDebugEnabled())
            log.debug("Access has been denied for (" + name + "," + resourceKey + ","
                + accessLevel + ") at " + info, e);
    }

    public List listAllLevels(XWikiContext context) throws XWikiException
    {
        List list = new ArrayList();
        list.addAll(allLevels);
        return list;
    }

    public String getRight(String action)
    {
        if (actionMap == null) {
            actionMap = new HashMap();
            actionMap.put("login", "login");
            actionMap.put("logout", "login");
            actionMap.put("loginerror", "login");
            actionMap.put("loginsubmit", "login");
            actionMap.put("view", "view");
            actionMap.put("viewrev", "view");
            actionMap.put("downloadrev", "download");
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
            actionMap.put("deleteversions", "admin");
            actionMap.put("undelete", "undelete");
            actionMap.put("reset", "delete");
            actionMap.put("commentadd", "comment");
            actionMap.put("register", "register");
            actionMap.put("redirect", "view");
            actionMap.put("admin", "admin");
            actionMap.put("export", "view");
            actionMap.put("import", "admin");
            actionMap.put("unknown", "view");
        }

        String right = (String) actionMap.get(action);
        if (right == null) {
            return "edit";
        } else
            return right;
    }

    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        log.debug("checkAccess for " + action + ", " + doc.getFullName());
        String username = null;
        XWikiUser user = null;
        boolean needsAuth = false;
        String right = getRight(action);

        if (right.equals("login")) {
            user = context.getWiki().checkAuth(context);
            if (user == null)
                username = "XWiki.XWikiGuest";
            else
                username = user.getUser();

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
                if (context.getMode() != XWikiContext.MODE_XMLRPC)
                    user = context.getWiki().checkAuth(context);
                else
                    user = new XWikiUser(context.getUser());

                if ((user == null) && (needsAuth)) {
                    logDeny("unauthentified", doc.getFullName(), action, "Authentication needed");
                    if (context.getRequest() != null) {
                        if (!context.getWiki().Param("xwiki.hidelogin", "false")
                            .equalsIgnoreCase("true")) {
                            context.getWiki().getAuthService().showLogin(context);
                        }
                    }
                    return false;
                }
            } catch (XWikiException e) {
                if (needsAuth)
                    throw e;
            }

            if (user == null)
                username = "XWiki.XWikiGuest";
            else
                username = user.getUser();

            // Save the user
            context.setUser(username);
        } else {
            username = user.getUser();
        }

        // Check Rights
        try {
            // Verify access rights and return if ok
            String docname;
            if (context.getDatabase() != null) {
                docname = context.getDatabase() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1)
                    username = context.getDatabase() + ":" + username;
            } else
                docname = doc.getFullName();

            if (context.getWiki().getRightService().hasAccessLevel(right, username, docname,
                context)) {
                logAllow(username, docname, action, "access manager granted right");
                return true;
            }
        } catch (Exception e) {
            // This should not happen..
            logDeny(username, (doc == null) ? "" : doc.getFullName(), action,
                "access manager exception " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        if (user == null) {
            // Denied Guest need to be authenticated
            logDeny("unauthentified", (doc == null) ? "" : doc.getFullName(), action,
                "Guest has been denied");
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
                context.getWiki().getXWikiPreference("authenticate_" + right, "", context)
                    .toLowerCase().equals("yes");
        } catch (Exception e) {
        }
        try {
            needsAuth |=
                (context.getWiki().getXWikiPreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }
        try {
            needsAuth |=
                context.getWiki().getWebPreference("authenticate_" + right, "", context)
                    .toLowerCase().equals("yes");
        } catch (Exception e) {
        }
        try {
            needsAuth |=
                (context.getWiki().getWebPreferenceAsInt("authenticate_" + right, 0, context) == 1);
        } catch (Exception e) {
        }
        return needsAuth;
    }

    public boolean hasAccessLevel(String right, String username, String docname,
        XWikiContext context) throws XWikiException
    {
        try {
            return hasAccessLevel(right, username, docname, true, context);
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean checkRight(String name, XWikiDocument doc, String accessLevel, boolean user,
        boolean allow, boolean global, XWikiContext context) throws XWikiRightNotFoundException,
        XWikiException
    {
        String className = global ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights";
        String fieldName = user ? "users" : "groups";
        boolean found = false;

        // Get the userdb and the shortname
        String userdatabase = null;
        String shortname = name;
        int i0 = name.indexOf(":");
        if (i0 != -1) {
            userdatabase = name.substring(0, i0);
            shortname = name.substring(i0 + 1);
        }

        if (log.isDebugEnabled())
            log.debug("Checking right: " + name + "," + doc.getFullName() + "," + accessLevel
                + "," + user + "," + allow + "," + global);

        Vector vobj = doc.getObjects(className);
        if (vobj != null) {
            if (log.isDebugEnabled())
                log.debug("Checking objects " + vobj.size());
            for (int i = 0; i < vobj.size(); i++) {
                if (log.isDebugEnabled())
                    log.debug("Checking object " + i);

                BaseObject bobj = (BaseObject) vobj.get(i);

                if (bobj == null) {
                    if (log.isDebugEnabled())
                        log.debug("Bypass object " + i);
                    continue;
                }
                String users = bobj.getStringValue(fieldName);
                String levels = bobj.getStringValue("levels");
                boolean allowdeny = (bobj.getIntValue("allow") == 1);

                if (allowdeny == allow) {
                    if (log.isDebugEnabled())
                        log.debug("Checking match: " + accessLevel + " in " + levels);

                    String[] levelsarray = StringUtils.split(levels, " ,|");
                    if (ArrayUtils.contains(levelsarray, accessLevel)) {
                        if (log.isDebugEnabled())
                            log.debug("Found a right for " + allow);
                        found = true;

                        if (log.isDebugEnabled())
                            log.debug("Checking match: " + name + " in " + users);

                        String[] userarray = StringUtils.split(users, " ,|");

                        for (int ii = 0; ii < userarray.length; ii++) {
                            String value = userarray[ii];
                            if (value.indexOf(".") == -1)
                                userarray[ii] = "XWiki." + value;
                        }

                        if (log.isDebugEnabled())
                            log.debug("Checking match: " + name + " in "
                                + StringUtils.join(userarray, ","));

                        // In the case where the document database and the user database is the same
                        // then we allow the usage of the short name, otherwise the fully qualified
                        // name is requested
                        if (context.getDatabase().equals(userdatabase)) {
                            if (ArrayUtils.contains(userarray, shortname)) {
                                if (log.isDebugEnabled())
                                    log.debug("Found matching right in " + users + " for "
                                        + shortname);
                                return true;
                            }
                            // We should also allow to skip "XWiki." from the usernames and group
                            // lists
                            String veryshortname =
                                shortname.substring(shortname.indexOf(".") + 1);
                            if (ArrayUtils.contains(userarray, veryshortname)) {
                                if (log.isDebugEnabled())
                                    log.debug("Found matching right in " + users + " for "
                                        + shortname);
                                return true;
                            }
                        }

                        if ((context.getDatabase() != null)
                            && (ArrayUtils.contains(userarray, name))) {
                            if (log.isDebugEnabled())
                                log.debug("Found matching right in " + users + " for " + name);
                            return true;
                        }

                        if (log.isDebugEnabled())
                            log.debug("Failed match: " + name + " in " + users);
                    }
                } else {
                    if (log.isDebugEnabled())
                        log.debug("Bypass object because wrong allow/deny" + i);
                }
            }
        }

        if (log.isDebugEnabled())
            log.debug("Searching for matching rights at group level");

        // Didn't found right at this level.. Let's go to group level
        Map grouplistcache = (Map) context.get("grouplist");
        if (grouplistcache == null) {
            grouplistcache = new HashMap();
            context.put("grouplist", grouplistcache);
        }

        Collection grouplist = new ArrayList();
        XWikiGroupService groupService = context.getWiki().getGroupService(context);
        String key = context.getDatabase() + ":" + name;
        Collection grouplist1 = (Collection) grouplistcache.get(key);

        if (grouplist1 == null) {
            grouplist1 = new ArrayList();
            try {
                Collection glist = groupService.listGroupsForUser(name, context);
                Iterator it = glist.iterator();

                while (it.hasNext()) {
                    grouplist1.add(context.getDatabase() + ":" + it.next());
                }
            } catch (Exception e) {

            }

            if (grouplist1 != null)
                grouplistcache.put(key, grouplist1);
            else
                grouplistcache.put(key, new ArrayList());
        }

        if (grouplist1 != null)
            grouplist.addAll(grouplist1);

        if (context.isVirtual()) {
            String database = context.getDatabase();
            try {
                shortname = Util.getName(name, context);

                if (!database.equals(context.getDatabase())) {
                    String key2 = context.getDatabase() + ":" + name;
                    Collection grouplist2 = (Collection) grouplistcache.get(key2);

                    if (grouplist2 == null) {
                        Collection glist = groupService.listGroupsForUser(shortname, context);
                        Iterator it = glist.iterator();
                        while (it.hasNext()) {
                            grouplist2.add(context.getDatabase() + ":" + it.next());
                        }
                        if (grouplist2 != null)
                            grouplistcache.put(key2, grouplist2);
                        else
                            grouplistcache.put(key2, new ArrayList());
                    }

                    if (grouplist2 != null)
                        grouplist.addAll(grouplist2);
                }
            } catch (Exception e) {
            } finally {
                context.setDatabase(database);
            }
        }

        if (log.isDebugEnabled())
            log.debug("Searching for matching rights for "
                + ((grouplist == null) ? "0" : "" + grouplist.size()) + " groups: " + grouplist);

        if (grouplist != null) {
            Iterator groupit = grouplist.iterator();
            while (groupit.hasNext()) {
                String group = (String) groupit.next();
                try {
                    // We need to construct the full group name to make sure the groups are
                    // handled separately
                    boolean result =
                        checkRight(group, doc, accessLevel, false, allow, global, context);
                    if (result)
                        return true;
                } catch (XWikiRightNotFoundException e) {
                } catch (Exception e) {
                    // This should not happen
                    e.printStackTrace();
                }
            }
        }

        if (log.isDebugEnabled())
            log.debug("Finished searching for rights for " + name + ": " + found);

        if (found)
            return false;
        else
            throw new XWikiRightNotFoundException();
    }

    public boolean hasAccessLevel(String accessLevel, String name, String resourceKey,
        boolean user, XWikiContext context) throws XWikiException
    {
        log.debug("hasAccessLevel for " + accessLevel + ", " + name + ", " + resourceKey);
        boolean deny = false;
        boolean allow = false;
        boolean allow_found = false;
        boolean deny_found = false;
        boolean isReadOnly = context.getWiki().isReadOnly();
        String database = context.getDatabase();
        XWikiDocument currentdoc = null;

        if (isReadOnly) {
            if ("edit".equals(accessLevel) || "delete".equals(accessLevel)
                || "undelete".equals(accessLevel) || "comment".equals(accessLevel)
                || "register".equals(accessLevel)) {
                logDeny(name, resourceKey, accessLevel, "server in read-only mode");
                return false;
            }
        }

        if (name.equals("XWiki.XWikiGuest") || name.endsWith(":XWiki.XWikiGuest")) {
            if (needsAuth(accessLevel, context)) {
                return false;
            }
        }

        // Fast return for delete right: allow the creator to delete the document
        if (accessLevel.equals("delete") && user) {
            currentdoc =
                (currentdoc == null) ? context.getWiki().getDocument(resourceKey, context)
                    : currentdoc;
            String creator = currentdoc.getCreator();
            if ((name != null) && (creator != null)) {
                if (name.equals(creator)) {
                    logAllow(name, resourceKey, accessLevel,
                        "delete right from document ownership");
                    return true;
                }
            }
        }

        allow = isSuperAdminOrProgramming(name, resourceKey, accessLevel, user, context);
        if ((allow == true) || (accessLevel.equals("programming"))) {
            return allow;
        }

        try {
            // Verify Wiki Owner
            String wikiOwner = context.getWiki().getWikiOwner(database, context);
            if (wikiOwner != null) {
                if (wikiOwner.equals(name)) {
                    logAllow(name, resourceKey, accessLevel, "admin level from wiki ownership");
                    return true;
                }
            }

            XWikiDocument xwikidoc =
                context.getWiki().getDocument("XWiki.XWikiPreferences", context);

            // Verify XWiki register right
            if (accessLevel.equals("register")) {
                try {
                    allow = checkRight(name, xwikidoc, "register", user, true, true, context);
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "register level");
                        return true;
                    } else {
                        logDeny(name, resourceKey, accessLevel, "register level");
                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
                logDeny(name, resourceKey, accessLevel, "register level (no right found)");
                return false;
            }

            int maxRecursiveSpaceChecks = context.getWiki().getMaxRecursiveSpaceChecks(context);
            boolean isSuperUser =
                isSuperUser(accessLevel, name, resourceKey, user, xwikidoc,
                    maxRecursiveSpaceChecks, context);
            if (isSuperUser) {
                logAllow(name, resourceKey, accessLevel, "admin level");
                return true;
            }

            // check has deny rights
            if (hasDenyRights()) {
                // First check if this document is denied to the specific user
                resourceKey = Util.getName(resourceKey, context);
                try {
                    currentdoc =
                        (currentdoc == null) ? context.getWiki()
                            .getDocument(resourceKey, context) : currentdoc;
                    deny = checkRight(name, currentdoc, accessLevel, user, false, false, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(name, resourceKey, accessLevel, "document level");
                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            try {
                currentdoc =
                    (currentdoc == null) ? context.getWiki().getDocument(resourceKey, context)
                        : currentdoc;
                allow = checkRight(name, currentdoc, accessLevel, user, true, false, context);
                allow_found = true;
                if (allow) {
                    logAllow(name, resourceKey, accessLevel, "document level");
                    return true;
                }
            } catch (XWikiRightNotFoundException e) {
            }

            // Check if this document is denied/allowed
            // through the web WebPreferences Global Rights

            String web = Util.getWeb(resourceKey);
            ArrayList spacesChecked = new ArrayList();
            int recursiveSpaceChecks = 0;
            while ((web != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
                // Add one to the recursive space checks
                recursiveSpaceChecks++;
                // add to list of spaces already checked
                spacesChecked.add(web);
                XWikiDocument webdoc =
                    context.getWiki().getDocument(web, "WebPreferences", context);
                if (!webdoc.isNew()) {
                    if (hasDenyRights()) {
                        try {
                            deny =
                                checkRight(name, webdoc, accessLevel, user, false, true, context);
                            deny_found = true;
                            if (deny) {
                                logDeny(name, resourceKey, accessLevel, "web level");
                                return false;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // If a right was found at the previous level
                    // then we cannot check the web rights anymore
                    if (!allow_found) {
                        try {
                            allow =
                                checkRight(name, webdoc, accessLevel, user, true, true, context);
                            allow_found = true;
                            if (allow) {
                                logAllow(name, resourceKey, accessLevel, "web level");
                                return true;
                            }
                        } catch (XWikiRightNotFoundException e) {
                        }
                    }

                    // find the parent web to check rights on it
                    web = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                    if ((web == null) || (web.trim().equals("")) || spacesChecked.contains(web)) {
                        // no parent space or space already checked (recursive loop). let's finish
                        // the loop
                        web = null;
                    }
                } else {
                    // let's finish the loop
                    web = null;
                }
            }

            // Check if this document is denied/allowed
            // through the XWiki.XWikiPreferences Global Rights
            if (hasDenyRights()) {
                try {
                    deny = checkRight(name, xwikidoc, accessLevel, user, false, true, context);
                    deny_found = true;
                    if (deny) {
                        logDeny(name, resourceKey, accessLevel, "xwiki level");
                        return false;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If a right was found at the document or web level
            // then we cannot check the web rights anymore
            if (!allow_found) {
                try {
                    allow = checkRight(name, xwikidoc, accessLevel, user, true, true, context);
                    allow_found = true;
                    if (allow) {
                        logAllow(name, resourceKey, accessLevel, "xwiki level");
                        return true;
                    }
                } catch (XWikiRightNotFoundException e) {
                }
            }

            // If neither doc, web or topic had any allowed ACL
            // and that all users that were not denied
            // should be allowed.
            if (!allow_found) {
                // Should these rights be denied only if no deny rights were found?
                if (accessLevel.equals("register") || accessLevel.equals("delete")) {
                    logDeny(name, resourceKey, accessLevel, "global level (" + accessLevel
                        + " right must be explicit)");
                    return false;
                } else {
                    logAllow(name, resourceKey, accessLevel,
                        "global level (no restricting right)");
                    return true;
                }
            } else {
                logDeny(name, resourceKey, accessLevel,
                    "global level (restricting right was found)");
                return false;
            }

        } catch (XWikiException e) {
            logDeny(name, resourceKey, accessLevel, "global level (exception)", e);
            e.printStackTrace();
            return false;
        } finally {
            context.setDatabase(database);
        }
    }

    private boolean hasDenyRights()
    {
        return true;
    }

    private boolean isSuperAdminOrProgramming(String name, String resourceKey,
        String accessLevel, boolean user, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        boolean allow;
        if (name.equals("XWiki.superadmin") || name.endsWith(":XWiki.superadmin")) {
            logAllow(name, resourceKey, accessLevel, "super admin level");
            return true;
        }

        try {
            // The master user and programming rights are checked in the main wiki
            context.setDatabase(context.getMainXWiki());
            XWikiDocument xwikimasterdoc =
                context.getWiki().getDocument("XWiki.XWikiPreferences", context);
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
                if (!name.startsWith(context.getWiki().getDatabase() + ":"))
                    return false;

                try {
                    allow =
                        checkRight(name, xwikimasterdoc, "programming", user, true, true, context);
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
            context.setDatabase(database);
        }

        return false;
    }

    private boolean isSuperUser(String accessLevel, String name, String resourceKey,
        boolean user, XWikiDocument xwikidoc, int maxRecursiveSpaceChecks, XWikiContext context)
        throws XWikiException
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

        // Verify Web super user
        String web = Util.getWeb(resourceKey);
        ArrayList spacesChecked = new ArrayList();
        int recursiveSpaceChecks = 0;
        while ((web != null) && (recursiveSpaceChecks <= maxRecursiveSpaceChecks)) {
            // Add one to the recursive space checks
            recursiveSpaceChecks++;
            // add to list of spaces already checked
            spacesChecked.add(web);
            XWikiDocument webdoc = context.getWiki().getDocument(web, "WebPreferences", context);
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
                web = webdoc.getStringValue("XWiki.XWikiPreferences", "parent");
                if ((web == null) || (web.trim().equals("")) || spacesChecked.contains(web)) {
                    // no parent space or space already checked (recursive loop). let's finish the
                    // loop
                    web = null;
                }
            } else {
                web = null;
            }
        }
        return false;
    }

    public boolean hasProgrammingRights(XWikiContext context)
    {
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        if (sdoc == null)
            sdoc = context.getDoc();
        return hasProgrammingRights(sdoc, context);
    }

    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        try {
            if (doc == null)
                return false;

            String username = doc.getAuthor();

            if (username == null)
                return false;

            String docname;
            if (doc.getDatabase() != null) {
                docname = doc.getDatabase() + ":" + doc.getFullName();
                if (username.indexOf(":") == -1)
                    username = doc.getDatabase() + ":" + username;
            } else
                docname = doc.getFullName();

            // programming rights can only been given for user of the main wiki
            if (context.getWiki().isVirtual()) {
                String maindb = context.getWiki().getDatabase();
                if ((maindb == null) || (!username.startsWith(maindb)))
                    return false;
            }

            return hasAccessLevel("programming", username, docname, context);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasAdminRights(XWikiContext context)
    {
        boolean hasAdmin = false;
        try {
            hasAdmin =
                hasAccessLevel("admin", context.getUser(), "XWiki.XWikiPreferences", context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!hasAdmin) {
            try {
                hasAdmin =
                    hasAccessLevel("admin", context.getUser(), context.getDoc().getSpace()
                        + ".WebPreferences", context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hasAdmin;
    }

}
