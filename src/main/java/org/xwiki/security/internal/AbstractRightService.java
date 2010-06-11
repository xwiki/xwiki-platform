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
package org.xwiki.security.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Formatter;

import org.xwiki.security.RightService;
import org.xwiki.security.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * @version $Id: $
 */
public abstract class AbstractRightService implements RightService
{
    /** Logger. */
    private static final Log LOG = LogFactory.getLog(RightService.class);

    /** Map containing all known actions. */
    private static Map<String, Right> actionMap;
    /** List of all rights, as strings. */
    private static List<String> allRights = new LinkedList();

    /**
     * Putter to circumvent the checkstyle max number of statements.
     */
    static class Putter
    {
        /**
         * @param key Action string.
         * @param value Right value.
         * @return This object.
         */
        Putter put(String key, Right value)
        {
            actionMap.put(key, value);
            return this;
        }
    }

    static {
        actionMap = new HashMap();
        new Putter()
            .put("login", Right.LOGIN)
            .put("logout", Right.LOGIN)
            .put("loginerror", Right.LOGIN)
            .put("loginsubmit", Right.LOGIN)
            .put("view", Right.VIEW)
            .put("viewrev", Right.VIEW)
            .put("get", Right.VIEW)
            //        actionMap.put("downloadrev", "download"); WTF??
            .put("downloadrev", Right.VIEW)
            .put("plain", Right.VIEW)
            .put("raw", Right.VIEW)
            .put("attach", Right.VIEW)
            .put("charting", Right.VIEW)
            .put("skin", Right.VIEW)
            .put("download", Right.VIEW)
            .put("dot", Right.VIEW)
            .put("svg", Right.VIEW)
            .put("pdf", Right.VIEW)
            .put("delete", Right.DELETE)
            .put("deleteversions", Right.ADMIN)
            //        actionMap.put("undelete", "undelete"); WTF??
            .put("undelete", Right.EDIT)
            .put("reset", Right.DELETE)
            .put("commentadd", Right.COMMENT)
            .put("register", Right.REGISTER)
            .put("redirect", Right.VIEW)
            .put("admin", Right.ADMIN)
            .put("export", Right.VIEW)
            .put("import", Right.ADMIN)
            .put("jsx", Right.VIEW)
            .put("ssx", Right.VIEW)
            .put("tex", Right.VIEW)
            .put("unknown", Right.VIEW)
            .put("programming", Right.PROGRAM);

        for (Right level : Right.values()) {
            if (!level.equals(Right.ILLEGAL)) {
                allRights.add(level.toString());
            }
        }
    }

    /**
     * Convert an action to a right.
     * @param action String representation of action.
     * @return The corresponding right, or {@link Right.ILLEGAL}.
     */
    protected final Right actionToRight(String action)
    {
        Right level = actionMap.get(action);
        if (level == null)
        {
            return Right.ILLEGAL;
        }
        return level;
    }

    /**
     * Get the user name from the context.
     * @param context The current context.
     * @return The user name.
     */
    protected abstract String getUserName(XWikiContext context);

    /**
     * Describe <code>handleLogin</code> method here.
     *
     * @param action a <code>String</code> value
     * @param doc a <code>XWikiDocument</code> value
     * @param context a <code>XWikiContext</code> value
     * @return a <code>boolean</code> value
     * @exception XWikiException if an error occurs
     */
    private boolean handleLogin(String action, XWikiDocument doc, XWikiContext context) throws XWikiException {
        XWikiUser user = context.getWiki().checkAuth(context);
        String username;

        if (user == null) {
            username = RightService.GUEST_USER_FULLNAME;
        } else {
            username = user.getUser();
        }

        // Save the user
        context.setUser(username);
        logAllow(username, doc.getFullName(), action, "login/logout pages");

        return true;
    }
    
    /**
     * Checks if the wiki current user has the right to execute (@code action} on the document {@code doc}, along with
     * redirecting to the login if it's not the case and there is no logged in user (the user is the guest user).
     * 
     * @param action the action to be executed on the document
     * @param doc the document to perform action on
     * @param context the xwiki context in which to perform the verification (from which to get the user, for example)
     * @return {@code true} if the user has right to execute {@code action} on {@code doc}, {@code false} otherwise
     *         <strong> and requests the login from the authentication service (redirecting to the login page in the
     *         case of a form authenticator, for example) when no user is logged in. </strong>
     * @throws XWikiException if something goes wrong during the rights checking process
     */
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        Right level = actionToRight(action);

        if (level == Right.LOGIN) {
            return handleLogin(action, doc, context);
        }

        return false;
    }

    /**
     * Verifies if the user identified by {@code username} has the access level identified by {@code right} on the
     * document with the name {@code docname}.
     * 
     * @param right the access level to check (for example, 'view' or 'edit' or 'comment').
     * @param username the name of the user to check the right for
     * @param docname the document on which to check the right
     * @param context the xwiki context in which to perform the verification
     * @return {@code true} if the user has the specified right on the document, {@code false} otherwise
     * @throws XWikiException if something goes wrong during the rights checking process
     */
    public abstract boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException;

    /**
     * Checks if the author of the context document (last editor of the content of the document) has programming rights
     * (used to determine if the protected calls in the script contained in the document should be executed or not).
     * 
     * @param context the xwiki context of this request
     * @return {@code true} if the author of the context document has programming rights, {@code false} otherwise.
     */
    public abstract boolean hasProgrammingRights(XWikiContext context);

    /**
     * Checks if the author of the passed document (last editor of the content of the document) has programming rights
     * (used to determine if the protected calls in the script contained in the document should be executed or not).
     * 
     * @param doc the document to check programming rights for
     * @param context the xwiki context of this request
     * @return {@code true} if the author of {@code doc} has programming rights, {@code false} otherwise.
     */
    public abstract boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context);

    /**
     * Checks that the current user in the context (the currently authenticated user) has administration rights on the
     * current wiki.
     * 
     * @param context the xwiki context of this request
     * @return {@code true} if the current user in the context has the {@code admin} right, {@code false} otherwise
     */
    public abstract boolean hasAdminRights(XWikiContext context);

    /**
     * @param context the xwiki context of this request
     * @return the list of all the known access levels
     * @throws XWikiException if something goes wrong during the rights checking process
     */
    public List<String> listAllLevels(XWikiContext context) throws XWikiException
    {
        return allRights;
    }


    /**
     * Log allow conclusion.
     * @param username The user name that was checked.
     * @param page The page that was checked.
     * @param action The action that was requested.
     * @param info Additional information.
     */
    protected void logAllow(String username, String page, String action, String info)
    {
        if (LOG.isDebugEnabled()) {
            Formatter f = new Formatter();
            LOG.debug(f.format("Access has been granted for (%s,%s,%s): %s", username, page, action, info));
        }
    }

    /**
     * Log deny conclusion.
     * @param username The user name that was checked.
     * @param page The page that was checked.
     * @param action The action that was requested.
     * @param info Additional information.
     */
    protected void logDeny(String username, String page, String action, String info)
    {
        if (LOG.isInfoEnabled()) {
            Formatter f = new Formatter();
            LOG.info(f.format("Access has been denied for (%s,%s,%s): %s", username, page, action, info));
        }
    }
    
    /**
     * Log deny conclusion.
     * @param name The user name that was checked.
     * @param resourceKey The page that was checked.
     * @param accessLevel The action that was requested.
     * @param info Additional information.
     * @param e Exception that was caught.
     */
    protected void logDeny(String name, String resourceKey, String accessLevel, String info, Exception e)
    {
        if (LOG.isDebugEnabled()) {
            Formatter f = new Formatter();
            LOG.debug(f.format("Access has been denied for (%s,%s,%s) at %s", name, resourceKey, accessLevel, info), e);
        }
    }

}
