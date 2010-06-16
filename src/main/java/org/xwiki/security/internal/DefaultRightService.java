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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import org.xwiki.security.RightService;
import org.xwiki.security.RightServiceException;
import org.xwiki.security.Right;
import static org.xwiki.security.Right.*;
import org.xwiki.security.RightState;
import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.RightCacheEntry;
import org.xwiki.security.RightLoader;
import org.xwiki.security.AccessLevel;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Formatter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * The default right service.
 *
 * @version $Id: $
 */
@Component
public class DefaultRightService implements RightService
{
    /** Logger. */
    private static final Log LOG = LogFactory.getLog(RightService.class);

    /** Map containing all known actions. */
    private static Map<String, Right> actionMap;
    /** List of all rights, as strings. */
    private static List<String> allRights = new LinkedList();

    /** The cached rights. */
    @Requirement private RightCache rightCache;

    /** The loader for filling the cache. */
    @Requirement private RightLoader rightLoader;

    /** Resolver for document references. */
    @Requirement private DocumentReferenceResolver<String> documentReferenceResolver;

    /** Resolver for user and group document references. */
    @Requirement("user") private DocumentReferenceResolver<String> userAndGroupReferenceResolver;

    /** Serializer. */
    @Requirement private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Putter to circumvent the checkstyle max number of statements.
     */
    private static class Putter
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
            .put("login", LOGIN)
            .put("logout", LOGIN)
            .put("loginerror", LOGIN)
            .put("loginsubmit", LOGIN)
            .put("view", VIEW)
            .put("viewrev", VIEW)
            .put("get", VIEW)
            //        actionMap.put("downloadrev", "download"); Huh??
            .put("downloadrev", VIEW)
            .put("plain", VIEW)
            .put("raw", VIEW)
            .put("attach", VIEW)
            .put("charting", VIEW)
            .put("skin", VIEW)
            .put("download", VIEW)
            .put("dot", VIEW)
            .put("svg", VIEW)
            .put("pdf", VIEW)
            .put("delete", DELETE)
            .put("deleteversions", ADMIN)
            //        actionMap.put("undelete", "undelete"); Huh??
            .put("undelete", EDIT)
            .put("reset", DELETE)
            .put("commentadd", COMMENT)
            .put("register", REGISTER)
            .put("redirect", VIEW)
            .put("admin", ADMIN)
            .put("export", VIEW)
            .put("import", ADMIN)
            .put("jsx", VIEW)
            .put("ssx", VIEW)
            .put("tex", VIEW)
            .put("unknown", VIEW)
            .put("programming", PROGRAM)
            .put("edit", EDIT)
            .put("save", EDIT)
            .put("preview", EDIT)
            .put("lock", EDIT)
            .put("cancel", EDIT)
            .put("delattachment", EDIT)
            .put("inline", EDIT)
            .put("upload", EDIT);

        for (Right level : values()) {
            if (!level.equals(ILLEGAL)) {
                allRights.add(level.toString());
            }
        }
    }

    /**
     * Convert an action to a right.
     * @param action String representation of action.
     * @return The corresponding right, or {@link ILLEGAL}.
     */
    protected final Right actionToRight(String action)
    {
        Right level = actionMap.get(action);
        if (level == null)
        {
            Formatter f = new Formatter();
            LOG.error(f.format("No action named '%s'", action.toString()));
            return ILLEGAL;
        }
        return level;
    }

    /**
     * @param right Right to authenticate.
     * @param doc Document that is being accessed.
     * @param context The current context
     * @return a {@link DocumentReference} that uniquely identifies
     * the user, if the authentication was successful.  {@code null}
     * on failure.
     */
    private DocumentReference authenticateUser(Right right, XWikiDocument doc, XWikiContext context)
    {
        XWikiUser user = context.getXWikiUser();
        boolean needsAuth;
        if (user == null) {
            needsAuth = needsAuth(right, context);
            try {

                if (context.getMode() != XWikiContext.MODE_XMLRPC) {
                    user = context.getWiki().checkAuth(context);
                } else {
                    user = new XWikiUser(RightService.GUEST_USER_FULLNAME);
                }

                if ((user == null) && (needsAuth)) {
                    logDeny(null, doc.getDocumentReference(), right, "Authentication needed");
                    return null;
                }
            } catch (XWikiException e) {
                LOG.error("Caught exception while authenticating user.", e);
                return null;
            }

            String username;
            if (user == null) {
                username = RightService.GUEST_USER_FULLNAME;
            } else {
                username = user.getUser();
            }
            context.setUser(username);
            return resolveUserName(username, context.getDatabase());
        } else {
            return resolveUserName(user.getUser(), context.getDatabase());
        }

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
            LOG.error("Failed to show login page.", e);
        }
    }

    /**
     * @param userRef a reference to a user profile document.
     * @return {@code true} if and only if the user is a guest.
     */
    private boolean userIsGuest(DocumentReference userRef)
    {
        return userRef.getName().equals(GUEST_USER)
            && userRef.getSpaceReferences().size() == 1
            && XWIKI_SPACE_PREFIX.startsWith(userRef.getLastSpaceReference().getName());
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        Right right = actionToRight(action);

        DocumentReference document = doc.getDocumentReference();
        DocumentReference user = authenticateUser(right, doc, context);

        boolean allow = false;

        if (user != null) {
            allow = checkAccess(right, user, document, context);
        }

        if (!allow && (user == null || userIsGuest(user))) {
            showLogin(context);
        }

        return allow;
    }

    /**
     * @param username name as a string.
     * @param wikiname default wiki name, if not explicitly specified in the username.
     * @return A document reference that uniquely identifies the user.
     */
    private DocumentReference resolveUserName(String username, String wikiname)
    {
        return userAndGroupReferenceResolver.resolve(username, wikiname);
    }

    /**
     * @param docname name of the document as string.
     * @param wikiname the default wiki where the document will be
     * assumet do be located, unless explicitly specified in docname.
     * @return the document reference.
     */
    private DocumentReference resolveDocName(String docname, String wikiname)
    {
        EntityReference defaultWiki = new EntityReference(wikiname, EntityType.WIKI);
        return documentReferenceResolver.resolve(docname, defaultWiki);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAccessLevel(String rightname, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        String wikiname = context.getDatabase();
        DocumentReference document = resolveDocName(docname, wikiname);
        LOG.debug("Resolved '" + docname + "' into " + document);
        DocumentReference user = resolveUserName(username, wikiname);
        Right right = Right.toRight(rightname);
        if (right == Right.ILLEGAL) {
            Formatter f = new Formatter();
            LOG.error(f.format("No such right: '%s'", rightname));
        }
        return checkAccess(right, user, document, context);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProgrammingRights(XWikiContext context)
    {
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        if (sdoc == null) {
            sdoc = context.getDoc();
        }

        return hasProgrammingRights(sdoc, context);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        DocumentReference user;
        String wikiname;

        if (doc != null) {
            String username = doc.getContentAuthor();
            if (username == null || username.equals("")) {
                logDeny(null, doc.getDocumentReference(), PROGRAM, "No content author.");
                return false;
            }
            wikiname = doc.getDocumentReference().getWikiReference().getName();
            user = resolveUserName(username, wikiname);
        } else {
            user = getUserReference(context);
            wikiname = context.getDatabase();
        }

        EntityReference wiki = new EntityReference(wikiname, EntityType.WIKI, null);
        return checkAccess(PROGRAM, user, wiki, context);
    }

    
    /**
     * @param right The right that will be checked.
     * @param user The user that will be checked
     * @param entity The document that will be checked.
     * @param context The current context.
     * @return {@code true} if and only if the given user have the
     * given right on the given document.
     */
    private boolean checkAccess(Right right,
                                DocumentReference user,
                                EntityReference entity,
                                XWikiContext context)
    {
        AccessLevel accessLevel;
        try {
            accessLevel = getAccessLevel(user, entity);
        } catch (Exception e) {
            LOG.error("Failed to check admin right for user [" + context.getUser() + "]", e);
            return false;
        }

        if (context.getWiki().isReadOnly()) {
            if (right == EDIT || right == DELETE || right == COMMENT || right == REGISTER) {
                logDeny(user, entity, right, "server in read-only mode");
                return true;
            }
        }

        if (accessLevel.get(right) == RightState.ALLOW) {
            logAllow(user, entity, right, "");
            return true;
        } else {
            logDeny(user, entity, right, "");
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAdminRights(XWikiContext context)
    {
        DocumentReference user = getUserReference(context);
        DocumentReference document = context.getDoc().getDocumentReference();
        return checkAccess(ADMIN, user, document, context);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> listAllLevels(XWikiContext context) throws XWikiException
    {
        return allRights;
    }

    /**
     * @param context The current context
     * @return A document reference uniquely identifying the current
     * user.
     */
    private DocumentReference getUserReference(XWikiContext context)
    {
        XWikiUser user = context.getXWikiUser();
        String username;
        LOG.debug("Getting user from context: " + user);
        if (user == null) {
            username = GUEST_USER_FULLNAME;
        } else {
            username = user.getUser();
        }
        String wikiname = context.getDatabase();
        return resolveUserName(username, wikiname);
    }

    /**
     * Obtain the access level for the user on the given entity from
     * the cache, and load it into the cache if unavailable.
     * @param user The user identity.
     * @param entity The entity.  May be of type DOCUMENT, WIKI, or SPACE.
     * @return the cached access level object.
     * @exception RightServiceException if an error occurs
     */
    private AccessLevel getAccessLevel(DocumentReference user, EntityReference entity)
        throws RightServiceException
    {

        for (EntityReference ref = entity; ref != null; ref = ref.getParent()) {
            RightCacheEntry entry = rightCache.get(rightCache.getRightCacheKey(ref));
            if (entry == null) {
                AccessLevel level = rightLoader.load(user, entity);
                Formatter f = new Formatter();
                LOG.debug(f.format("1. Loaded a new entry for %s@%s into cache: %s",
                                   entityReferenceSerializer.serialize(user),
                                   entityReferenceSerializer.serialize(entity),
                                   level));
                return level;
            }
            switch (entry.getType()) {
                case HAVE_OBJECTS:
                    RightCacheKey userKey = rightCache.getRightCacheKey(user);
                    RightCacheKey entityKey = rightCache.getRightCacheKey(ref);
                    entry = rightCache.get(userKey, entityKey);
                    if (entry == null) {
                        AccessLevel level = rightLoader.load(user, entity);
                        Formatter f = new Formatter();
                        LOG.debug(f.format("2. Loaded a new entry for %s@%s into cache: %s",
                                           entityReferenceSerializer.serialize(user),
                                           entityReferenceSerializer.serialize(entity),
                                           level));
                        return level;
                    } else {
                        if (entry.getType() == RightCacheEntry.Type.ACCESS_LEVEL) {
                            LOG.debug("Got cached entry for "
                                      + entityReferenceSerializer.serialize(user)
                                      + "@"
                                      + entityReferenceSerializer.serialize(entity) + ": " + entry);
                            return (AccessLevel) entry;
                        } else {
                            Formatter f = new Formatter();
                            LOG.error(f.format("The cached entry for '%s' at '$s' was of incorrect type: %s", 
                                               user.toString(),
                                               ref.toString(),
                                               entry.getType().toString()));
                            throw new RuntimeException();
                        }
                    }
                case HAVE_NO_OBJECTS:
                    break;
                default:
                    Formatter f = new Formatter();
                    LOG.error(f.format("The cached entry for '%s' was of incorrect type: %s", 
                                       ref.toString(),
                                       entry.getType().toString()));
                    throw new RuntimeException();
            }
        }

        LOG.debug("Returning default access level.");
        return AccessLevel.DEFAULT_ACCESS_LEVEL;
    }

    /**
     * Log allow conclusion.
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    private void logAllow(DocumentReference user, EntityReference entity, Right right, String info)
    {
        if (LOG.isDebugEnabled()) {
            String userName = entityReferenceSerializer.serialize(user);
            String docName = entityReferenceSerializer.serialize(entity);
            Formatter f = new Formatter();
            LOG.debug(f.format("Access has been granted for (%s,%s,%s): %s",
                               userName, docName, right.toString(), info));
        }
    }

    /**
     * Log deny conclusion.
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    protected void logDeny(DocumentReference user, EntityReference entity,  Right right, String info)
    {
        if (LOG.isInfoEnabled()) {
            String userName = entityReferenceSerializer.serialize(user);
            String docName = entityReferenceSerializer.serialize(entity);
            Formatter f = new Formatter();
            LOG.info(f.format("Access has been denied for (%s,%s,%s): %s",
                              userName, docName, right.toString(), info));
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
            LOG.debug(f.format("Access has been denied for (%s,%s,%s) at %s",
                               name, resourceKey, accessLevel, info), e);
        }
    }

    /**
     * @param value a <code>String</code> value
     * @return a <code>Boolean</code> value
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
                LOG.warn(f.format("Failed to interpete preference value: '%s'", value));
            }
        }
        return null;
    }

    /**
     * @param right the right to check.
     * @param context the current context. 
     * @return {@code true} if the given right requires authentication.
     */
    private boolean needsAuth(Right right, XWikiContext context)
    {
        String prefName = "authenticate_" + right.toString();

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

}