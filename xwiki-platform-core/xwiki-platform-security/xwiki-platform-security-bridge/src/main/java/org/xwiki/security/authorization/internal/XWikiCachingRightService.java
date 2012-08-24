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
package org.xwiki.security.authorization.internal;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

/**
 * Legacy bridge aimed to replace the current RightService until the new API is used in all places.
 * @version $Id$
 * @since 4.0M2
 */
public class XWikiCachingRightService implements XWikiRightService
{
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiCachingRightService.class);

    /** The login action. */
    private static final String DELETE_ACTION = "delete";

    /** The delete action. */
    private static final String LOGIN_ACTION = "login";

    /**
     * Map containing all known actions.
     */
    private static final ActionMap ACTION_MAP = new ActionMap();

    static {
        ACTION_MAP
            .putAction(LOGIN_ACTION, Right.LOGIN)
            .putAction("imagecaptcha", Right.LOGIN)
            .putAction("view", Right.VIEW)
            .putAction(DELETE_ACTION, Right.DELETE)
            .putAction("admin", Right.ADMIN)
            .putAction("programing", Right.PROGRAM)
            .putAction("edit", Right.EDIT)
            .putAction("register", Right.REGISTER)
            .putAction("logout", Right.LOGIN)
            .putAction("loginerror", Right.LOGIN)
            .putAction("loginsubmit", Right.LOGIN)
            .putAction("viewrev", Right.VIEW)
            .putAction("viewattachrev", Right.VIEW)
            .putAction("get", Right.VIEW)
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
            // TODO: The "undelete" action is mapped to the right "undelete" in the legacy
            // implementation.  We should check whether the "undelete" right is actually used or not and
            // if we need to introduce it here as well for compatiblity reasons.
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
            .putAction("propdisable", Right.EDIT)
            .putAction("propenable", Right.EDIT)
            .putAction("objectadd", Right.EDIT)
            .putAction("objectremove", Right.EDIT)
            .putAction("objectsync", Right.EDIT)
            .putAction("rollback", Right.EDIT)
            .putAction("upload", Right.EDIT)
            .putAction("create", Right.EDIT)
            .putAction("deleteversions", Right.ADMIN)
            .putAction("deletespace", Right.ADMIN);
    }

    /** Resolver for document references. */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> documentReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);

    /** Resolver for user and group document references. */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "user");

    /** The authorization manager used to really do the job. */
    private final AuthorizationManager authorizationManager
        = Utils.getComponent(AuthorizationManager.class);

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
    private DocumentReference resolveDocumentName(String docname, WikiReference wikiReference)
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
                /*
                 * We must explicitly check the action from the context, as some templates that are
                 * rendered may call checkAccess with different actions (which, strictly speaking is
                 * incorrect, those templates should use hasAccessLevel).  In particular, 'menuview.vm'
                 * will call checkAccess with action 'view', if the document 'XWiki.XWikiLogin' exists.
                 */
                && !LOGIN_ACTION.equals(context.getAction())
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
     * @param context current {@link XWikiContext}
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
                    user = new XWikiUser(XWikiConstants.GUEST_USER_FULLNAME);
                }

                if ((user == null) && (needsAuth)) {
                    LOGGER.info("Authentication needed for right {} and entity {}.", right, entityReference);
                    return null;
                }
            } catch (XWikiException e) {
                LOGGER.error("Caught exception while authenticating user.", e);
            }

            String username;
            if (user == null) {
                username = XWikiConstants.GUEST_USER_FULLNAME;
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
     * @param context current {@link XWikiContext}
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

        LOGGER.debug("checkAccess for action {} on entity {}.", right, entityReference);

        DocumentReference userReference = authenticateUser(right, entityReference, context);
        if (userReference == null) {
            showLogin(context);
            return false;
        }

        if (authorizationManager.hasAccess(right, userReference, entityReference)) {
            return true;
        }

        // If the right has been denied, and we have guest user, redirect the user to login page
        // unless the denied is on the login action, which could cause infinite redirection.
        // FIXME: The hasAccessLevel is broken (do not allow document creator) on the delete action in the old
        // implementation, so code that simply want to verify if a user can delete (but is not actually deleting)
        // has to call checkAccess. This happen really often, and this why we should not redirect to login on failed
        // delete, since it would prevent most user to do anything.
        if (context.getUserReference() == null && !DELETE_ACTION.equals(action) && !LOGIN_ACTION.equals(action)) {
            LOGGER.debug("Redirecting guest user to login, since it have been denied {} on {}.",
                         right, entityReference);
            showLogin(context);
        }

        return false;
    }
 
    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        WikiReference wikiReference = new WikiReference(context.getDatabase());
        DocumentReference document = resolveDocumentName(docname, wikiReference);
        LOGGER.debug("Resolved '{}' into {}", docname, document);
        DocumentReference user = resolveUserName(username, wikiReference);

        return authorizationManager.hasAccess(Right.toRight(right), user, document);
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

        return authorizationManager.hasAccess(Right.PROGRAM, user, wiki);
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        DocumentReference user = context.getUserReference();
        DocumentReference document = context.getDoc().getDocumentReference();
        return authorizationManager.hasAccess(Right.ADMIN, user, document);
    }

    @Override
    public boolean hasWikiAdminRights(XWikiContext context)
    {
        DocumentReference user = context.getUserReference();
        WikiReference wiki = new WikiReference(context.getDatabase());
        return authorizationManager.hasAccess(Right.ADMIN, user, wiki);
    }

    @Override
    public List<String> listAllLevels(XWikiContext context)
        throws XWikiException
    {
        return Right.getAllRightsAsString();
    }
}
