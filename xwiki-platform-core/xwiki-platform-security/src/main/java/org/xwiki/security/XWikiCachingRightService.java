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
package org.xwiki.security;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * Legacy bridge aimed to replace the current Right Service until the new API is used in all places.
 * @version $Id$
 */
public class XWikiCachingRightService implements XWikiRightService
{
    /**
     * The Guest username.
     */
    public static String GUEST_USER = "XWikiGuest";

    /**
     * The Guest full name.
     */
    public static String GUEST_USER_FULLNAME = RightService.XWIKI_SPACE_PREFIX + GUEST_USER;


    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiCachingRightService.class);

    /** Resolver for document references. */
    private DocumentReferenceResolver<String> documentReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.class);

    /** Resolver for user and group document references. */
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.class, "user");

    /** The actual right service compoennt. */
    private final RightService rightService
        = Utils.getComponent(RightServiceConfigurationManager.class).getConfiguredRightService();

    /**
     * Specialized map with a chainable put action to avoid exceeding code complexity during initialization.
     */
    private static class ActionMap extends HashMap<String, Right>
    {
        /** Serialization identifier for conformance to Serializable. */
        private static final long serialVersionUID = 1;

        /** Allow filling the map in the initializer without exceeding code complexity.
         * @param action the action name
         * @param right the corresponding right required
         * @return this action map to allow code chaining
         */
        public ActionMap putAction(String action, Right right)
        {
            put(action, right);
            return this;
        }
    }

    /**
     * Map containing all known actions. 
     */
    private static final ActionMap ACTION_MAP = new ActionMap();
    
    static {
        ACTION_MAP
            .putAction("login", Right.LOGIN)
            .putAction("view", Right.VIEW)
            .putAction("delete", Right.DELETE)
            .putAction("admin", Right.ADMIN)
            .putAction("programing", Right.PROGRAM)
            .putAction("edit", Right.EDIT)
            .putAction("register", Right.REGISTER)
            .putAction("logout", Right.LOGIN)
            .putAction("loginerror", Right.LOGIN)
            .putAction("loginsubmit", Right.LOGIN)
            .putAction("viewrev", Right.VIEW)
            .putAction("get", Right.VIEW)
                // .putAction("downloadrev", "download"); Huh??
            .putAction("downloadrev", Right.VIEW)
            .putAction("plain", Right.VIEW)
            .putAction("raw", Right.VIEW)
            .putAction("attach", Right.VIEW)
            .putAction("charting", Right.VIEW)
            .putAction("skin", Right.VIEW)
            .putAction("download", Right.VIEW)
            .putAction("dot", Right.VIEW)
            .putAction("svg", Right.VIEW)
            .putAction("pdf", Right.VIEW)
            .putAction("deleteversions", Right.ADMIN)
                // .putAction("undelete", "undelete"); Huh??
            .putAction("undelete", Right.EDIT)
            .putAction("reset", Right.DELETE)
            .putAction("commentadd", Right.COMMENT)
            .putAction("redirect", Right.VIEW)
            .putAction("export", Right.VIEW)
            .putAction("import", Right.ADMIN)
            .putAction("jsx", Right.VIEW)
            .putAction("ssx", Right.VIEW)
            .putAction("tex", Right.VIEW)
            .putAction("unknown", Right.VIEW)
            .putAction("save", Right.EDIT)
            .putAction("preview", Right.EDIT)
            .putAction("lock", Right.EDIT)
            .putAction("cancel", Right.EDIT)
            .putAction("delattachment", Right.EDIT)
            .putAction("inline", Right.EDIT)
            .putAction("propadd", Right.EDIT)
            .putAction("propupdate", Right.EDIT)
            .putAction("propdelete", Right.EDIT)
            .putAction("objectadd", Right.EDIT)
            .putAction("objectremove", Right.EDIT)
            .putAction("objectsync", Right.EDIT)
            .putAction("rollback", Right.EDIT)
            .putAction("upload", Right.EDIT)
            .putAction("create", Right.EDIT);
    }

    /**
     * Map an action represented by a string to a right.
     * @param action String representation of action.
     * @return right The corresponding Right instance, or
     * {@code ILLEGAL}.
     */
    public static Right actionToRight(String action)
    {
        Right right = ACTION_MAP.get(action);
        if (right == null) {
            return Right.ILLEGAL;
        }
        return right;
    }

    /**
     * @param username name as a string.
     * @param wikiReference default wiki, if not explicitly specified in the username.
     * @return A document reference that uniquely identifies the user.
     */
    private DocumentReference resolveUserName(String username, WikiReference wikiReference)
    {
        return userAndGroupReferenceResolver.resolve(username, wikiReference);
    }

    /**
     * @param docname name of the document as string.
     * @param wikiReference the default wiki where the document will be
     * assumed do be located, unless explicitly specified in docname.
     * @return the document reference.
     */
    private DocumentReference resolveDocName(String docname, WikiReference wikiReference)
    {
        return documentReferenceResolver.resolve(docname, wikiReference);
    }

    /**
     * Show the login page, unless the wiki is configured otherwise.
     * @param context the context
     */
    private void showLogin(XWikiContext context)
    {
        try {
            if (context.getRequest() != null
                && !context.getWiki().Param("xwiki.hidelogin", "false").equalsIgnoreCase("true")) {
                context.getWiki().getAuthService().showLogin(context);
            }
        } catch (XWikiException e) {
            LOGGER.error("Failed to show login page.", e);
        }
    }

    /**
     * @param right Right to authenticate.
     * @param entityReference Document that is being accessed.
     * @return a {@link DocumentReference} that uniquely identifies
     * the user, if the authentication was successful.  {@code null}
     * on failure.
     */
    private DocumentReference authenticateUser(Right right, EntityReference entityReference, XWikiContext context)
    {
        XWikiUser user = context.getXWikiUser();
        boolean needsAuth;
        if (user == null) {
            needsAuth = needsAuth(right, context);
            try {
                if (context.getMode() != XWikiContext.MODE_XMLRPC) {
                    user = context.getWiki().checkAuth(context);
                } else {
                    user = new XWikiUser(GUEST_USER_FULLNAME);
                }

                if ((user == null) && (needsAuth)) {
                    LOGGER.info("Authentication needed for right " + right + " and entity " + entityReference + ".");
                    return null;
                }
            } catch (XWikiException e) {
                LOGGER.error("Caught exception while authenticating user.", e);
            }

            String username;
            if (user == null) {
                username = GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }
            context.setUser(username);
            return resolveUserName(username, new WikiReference(context.getDatabase()));
        } else {
            return resolveUserName(user.getUser(), new WikiReference(context.getDatabase()));
        }

    }

    /**
     * @param value a {@code String} value
     * @return a {@code Boolean} value
     */
    private Boolean checkNeedsAuthValue(String value)
    {
        if (value != null && !value.equals("")) {
            if (value.toLowerCase().equals("yes")) {
                return true;
            }
            try {
                if (Integer.parseInt(value) > 0) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Formatter f = new Formatter();
                LOGGER.warn(f.format("Failed to parse preference value: '%s'", value).toString());
            }
        }
        return null;
    }

    /**
     * @param right the right to check.
     * @return {@code true} if the given right requires authentication.
     */
    private boolean needsAuth(Right right, XWikiContext context)
    {
        String prefName = "authenticate_" + right.getName();

        String value = context.getWiki().getXWikiPreference(prefName, "", context);
        Boolean result = checkNeedsAuthValue(value);
        if (result != null) {
            return result;
        }

        value = context.getWiki().getSpacePreference(prefName, "", context).toLowerCase();
        result = checkNeedsAuthValue(value);
        if (result != null) {
            return result;
        }

        return false;
    }

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        Right right = actionToRight(action);
        EntityReference entityReference = doc.getDocumentReference();

        LOGGER.debug("checkAccess for action " + right + " on entity " + entityReference + ".");

        DocumentReference userReference = authenticateUser(right, entityReference, context);
        if (userReference == null) {
            showLogin(context);
            return false;
        }
        return rightService.hasAccess(right, userReference, entityReference);
    }
 
    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        WikiReference wikiReference = new WikiReference(context.getDatabase());
        DocumentReference document = resolveDocName(docname, wikiReference);
        LOGGER.debug("Resolved '" + docname + "' into " + document);
        DocumentReference user = resolveUserName(username, wikiReference);

        return rightService.hasAccess(Right.toRight(right), user, document);
    }

    @Override
    public boolean hasProgrammingRights(XWikiContext context)
    {
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        return hasProgrammingRights((sdoc != null) ? sdoc : context.getDoc(), context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        DocumentReference user;
        WikiReference wiki;

        if (doc != null) {
            user = doc.getContentAuthorReference();
            wiki = doc.getDocumentReference().getWikiReference();
        } else {
            user = context.getUserReference();
            wiki = new WikiReference(context.getDatabase());
        }

        return rightService.hasAccess(Right.PROGRAM, user, wiki);
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        DocumentReference user = context.getUserReference();
        DocumentReference document = context.getDoc().getDocumentReference();
        return rightService.hasAccess(Right.ADMIN, user, document);
    }

    @Override
    public boolean hasWikiAdminRights(XWikiContext context)
    {
        DocumentReference user = context.getUserReference();
        WikiReference wiki = new WikiReference(context.getDatabase());
        return rightService.hasAccess(Right.ADMIN, user, wiki);
    }

    @Override
    public List<String> listAllLevels(XWikiContext context)
        throws XWikiException
    {
        return Right.getAllRightsAsString();
    }
}
