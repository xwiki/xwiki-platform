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

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.internal.XWikiConstants;

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
            .putAction("distribution", Right.VIEW)
            .putAction("admin", Right.ADMIN)
            .putAction("programming", Right.PROGRAM)
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
            .putAction("commentsave", Right.COMMENT)
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
            .putAction("create", Right.VIEW)
            .putAction("deleteversions", Right.ADMIN)
            .putAction("deletespace", Right.ADMIN)
            .putAction("temp", Right.VIEW)
            .putAction("webjars", Right.VIEW);
    }

    /** Resolver for document references. */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> documentReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /** Resolver for user and group document references. */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver
        = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "user");

    /** The rendering context to check PR for signed macro. */
    private final RenderingContext renderingContext
        = Utils.getComponent(RenderingContext.class);

    /** The authorization manager used to really do the job. */
    private final AuthorizationManager authorizationManager
        = Utils.getComponent(AuthorizationManager.class);

    /** The contextual authorization manager used to really do the job. */
    private final ContextualAuthorizationManager contextualAuthorizationManager
        = Utils.getComponent(ContextualAuthorizationManager.class);

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
     * Ensure user authentication if needed.
     *
     * @param context Current XWikiContext
     */
    private void authenticateUser(XWikiContext context)
    {
        DocumentReference contextUserReference = context.getUserReference();
        DocumentReference userReference = contextUserReference;

        if (userReference == null && context.getMode() != XWikiContext.MODE_XMLRPC) {
            try {
                XWikiUser user = context.getWiki().checkAuth(context);
                if (user != null) {
                    userReference = resolveUserName(user.getUser(), new WikiReference(context.getWikiId()));
                }
            } catch (XWikiException e) {
                LOGGER.error("Caught exception while authenticating user.", e);
            }
        }

        if (userReference != null && XWikiConstants.GUEST_USER.equals(userReference.getName())) {
            // Public users (not logged in) should be passed as null in the new API. It may happen that badly
            // design code, and poorly written API does not take care, so we prevent security issue here.
            userReference = null;
        }

        if (userReference != contextUserReference
            && (userReference == null || !userReference.equals(contextUserReference))) {
            context.setUserReference(userReference);
        }
    }

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        Right right = actionToRight(action);
        EntityReference entityReference = doc.getDocumentReference();

        LOGGER.debug("checkAccess for action [{}] on entity [{}].", right, entityReference);

        authenticateUser(context);

        if (contextualAuthorizationManager.hasAccess(right, entityReference)) {
            return true;
        }

        // If the right has been denied, and we have guest user, redirect the user to login page
        // unless the denied is on the login action, which could cause infinite redirection.
        // FIXME: The hasAccessLevel is broken (do not allow document creator) on the delete action in the old
        // implementation, so code that simply want to verify if a user can delete (but is not actually deleting)
        // has to call checkAccess. This happen really often, and this why we should not redirect to login on failed
        // delete, since it would prevent most user to do anything.
        if (context.getUserReference() == null && !DELETE_ACTION.equals(action) && !LOGIN_ACTION.equals(action)) {
            LOGGER.debug("Redirecting unauthenticated user to login, since it have been denied [{}] on [{}].",
                         right, entityReference);
            showLogin(context);
        }

        return false;
    }

    @Override
    public boolean hasAccessLevel(String rightName, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        WikiReference wikiReference = new WikiReference(context.getWikiId());
        DocumentReference document = resolveDocumentName(docname, wikiReference);
        LOGGER.debug("hasAccessLevel() resolved document named [{}] into reference [{}]", docname, document);
        DocumentReference user = resolveUserName(username, wikiReference);

        if (user != null && XWikiConstants.GUEST_USER.equals(user.getName())) {
            // Public users (not logged in) should be passed as null in the new API
            user = null;
        }

        Right right = Right.toRight(rightName);

        return authorizationManager.hasAccess(right, user, document);
    }

    @Override
    public boolean hasProgrammingRights(XWikiContext context)
    {
        return contextualAuthorizationManager.hasAccess(Right.PROGRAM);
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
            wiki = new WikiReference(context.getWikiId());
        }

        if (user != null && XWikiConstants.GUEST_USER.equals(user.getName())) {
            // Public users (not logged in) should be passed as null in the new API. It may happen that badly
            // design code, and poorly written API does not take care, so we prevent security issue here.
            user = null;
        }

        // This method as never check for external contextual aspect like rendering context restriction or dropping of
        // permissions. So we do not use the contextual authorization manager to keep backward compatibility.
        return authorizationManager.hasAccess(Right.PROGRAM, user, wiki);
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        return contextualAuthorizationManager.hasAccess(Right.ADMIN);
    }

    @Override
    public boolean hasWikiAdminRights(XWikiContext context)
    {
        return contextualAuthorizationManager.hasAccess(Right.ADMIN, new WikiReference(context.getWikiId()));
    }

    @Override
    public List<String> listAllLevels(XWikiContext context)
        throws XWikiException
    {
        return Right.getAllRightsAsString();
    }
}
