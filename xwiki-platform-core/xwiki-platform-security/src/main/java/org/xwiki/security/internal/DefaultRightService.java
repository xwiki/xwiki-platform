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

import java.util.Formatter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.AccessDeniedException;
import org.xwiki.security.AccessLevel;
import org.xwiki.security.InsufficientAuthenticationException;
import org.xwiki.security.Right;
import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheEntry;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.RightDescription;
import org.xwiki.security.RightLoader;
import org.xwiki.security.RightService;
import org.xwiki.security.RightServiceException;
import org.xwiki.security.RightState;
import org.xwiki.security.UnableToRegisterRightException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.xwiki.security.Right.COMMENT;
import static org.xwiki.security.Right.DELETE;
import static org.xwiki.security.Right.EDIT;
import static org.xwiki.security.Right.REGISTER;
import static org.xwiki.security.RightState.ALLOW;
import static org.xwiki.security.RightState.DENY;

/**
 * The default right service.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultRightService implements RightService
{
    /** Logger. **/
    @Inject
    private Logger logger;
    
    /** Execution to retrieve the XWiki context if needed */
    @Inject
    private Execution execution;
    
    /** The cached rights. */
    @Inject
    private RightCache rightCache;

    /** The loader for filling the cache. */
    @Inject
    private RightLoader rightLoader;

    /** Serializer. */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext() {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    /**
     * @param context the current {@code XWikiContext}
     * @return the current document in context used for security checks
     */
    private XWikiDocument getCurrentDocument(XWikiContext context)
    {
        // TODO: Is there still a sdoc in the context sometimes ?
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        return (sdoc != null) ? sdoc : context.getDoc();
    }

    /**
     * @param right the right to check
     * @param context the current {@code XWikiContext}
     * @throws AccessDeniedException if the access should be denied due to a readonly wiki
     */
    private void checkReadOnlyWiki(Right right, XWikiContext context) throws AccessDeniedException
    {
        if (context.getWiki().isReadOnly()) {
            if (right == EDIT || right == DELETE || right == COMMENT || right == REGISTER) {
                throw new AccessDeniedException("server in read-only mode");
            }
        }
    }

    @Override
    public void checkUserAccess(Right right) throws InsufficientAuthenticationException, AccessDeniedException
    {
        XWikiContext context = getXWikiContext();
        DocumentReference userReference = context.getUserReference();
        if (userReference == null) {
            throw new InsufficientAuthenticationException();
        }

        checkReadOnlyWiki(right, context);


        try {
            if (!hasAccessLevel(right, userReference, getCurrentDocument(context).getDocumentReference())) {
                throw new AccessDeniedException();
            }
        } catch(Exception e) {
            throw new AccessDeniedException(e);
        }
    }

    @Override
    public void checkAuthorAccess(Right right) throws InsufficientAuthenticationException, AccessDeniedException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument document = getCurrentDocument(context);
        DocumentReference userReference = document.getAuthorReference();
        if (userReference == null) {
            throw new InsufficientAuthenticationException();
        }

        checkReadOnlyWiki(right, context);

        try {
            if (!hasAccessLevel(right, userReference, document.getDocumentReference())) {
                throw new AccessDeniedException();
            }
        } catch(Exception e) {
            throw new AccessDeniedException(e);
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        try{
            return hasAccessLevel(right, userReference, entityReference);
        } catch (Exception e) {
            this.logger.error("Failed to load rights for user " + userReference + ".", e);
            return false;
        }
    }
    
    private boolean hasAccessLevel(Right right, DocumentReference userReference, EntityReference entityReference)
        throws RightServiceException
    {
        if (userReference == null) {
            logDeny(userReference, entityReference, right, "no user");
            return false;
        }

        if (right == Right.ILLEGAL) {
            logDeny(userReference, entityReference, right, "no such right");
            return false;
        }

        AccessLevel accessLevel = getAccessLevel(userReference, entityReference);

        RightState access = accessLevel.get(right); 
        logAccess(access, userReference, entityReference, right, "access checked");
        return access == ALLOW;
    }

    @Override
    public Right register(RightDescription rightDescription) throws UnableToRegisterRightException
    {
        try {
            return new Right(rightDescription);
        } catch(Throwable e) {
            throw new UnableToRegisterRightException(rightDescription, e);
        }
    }

    /**
     * Obtain the access level for the user on the given entity from
     * the cache, and load it into the cache if unavailable.
     * @param user The user identity.
     * @param entity The entity.  May be of type DOCUMENT, WIKI, or SPACE.
     * @return the cached access level object.
     * @exception org.xwiki.security.RightServiceException if an error occurs
     */
    private AccessLevel getAccessLevel(DocumentReference user, EntityReference entity)
        throws RightServiceException
    {

        for (EntityReference ref = entity; ref != null; ref = ref.getParent()) {
            RightCacheEntry entry = rightCache.get(rightCache.getRightCacheKey(ref));
            if (entry == null) {
                AccessLevel level = rightLoader.load(user, entity);
                Formatter f = new Formatter();
                this.logger.debug(f.format("1. Loaded a new entry for %s@%s into cache: %s",
                                           entityReferenceSerializer.serialize(user),
                                           entityReferenceSerializer.serialize(entity),
                                           level).toString());
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
                        this.logger.debug(f.format("2. Loaded a new entry for %s@%s into cache: %s",
                                                   entityReferenceSerializer.serialize(user),
                                                   entityReferenceSerializer.serialize(entity),
                                                   level).toString());
                        return level;
                    } else {
                        if (entry.getType() == RightCacheEntry.Type.ACCESS_LEVEL) {
                            this.logger.debug("Got cached entry for "
                                      + entityReferenceSerializer.serialize(user)
                                      + "@"
                                      + entityReferenceSerializer.serialize(entity) + ": " + entry);
                            return (AccessLevel) entry;
                        } else {
                            Formatter f = new Formatter();
                            this.logger.error(f.format("The cached entry for '%s' at '%s' was of incorrect type: %s",
                                                       user.toString(),
                                                       ref.toString(),
                                                       entry.getType().toString()).toString());
                            throw new InternalError();
                        }
                    }
                case HAVE_NO_OBJECTS:
                    break;
                default:
                    Formatter f = new Formatter();
                    this.logger.error(f.format("The cached entry for '%s' was of incorrect type: %s", 
                                               ref.toString(),
                                               entry.getType().toString()).toString());
                    throw new InternalError();
            }
        }

        this.logger.debug("Returning default access level.");
        return AccessLevel.getDefaultAccessLevel();
    }

    /**
     * Log allow conclusion.
     * @param access The ALLOW or DENY state
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    private void logAccess(RightState access, DocumentReference user, EntityReference entity, Right right, String info)
    {
        if ((access == ALLOW && this.logger.isDebugEnabled()) || (access != ALLOW && this.logger.isInfoEnabled())) {
            String userName = (user != null) ? entityReferenceSerializer.serialize(user) : "no user";
            String docName = (entity != null) ?entityReferenceSerializer.serialize(entity) : "no entity";
            Formatter f = new Formatter();
            if (access == ALLOW) {
                this.logger.debug(f.format("Access has been granted for (%s,%s,%s): %s",
                    userName, docName, right.getName(), info).toString());
            } else {
                this.logger.info(f.format("Access has been denied for (%s,%s,%s): %s",
                    userName, docName, right.getName(), info).toString());
            }
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
        logAccess(DENY, user, entity, right, info);
    }
}
