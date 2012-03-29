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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * The instance of this class monitors updates and invalidates right
 * cache entries whenever necessary.
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityCacheRulesInvalidator implements SecurityCacheRulesInvalidator, EventListener
{
    /**
     * Fair read-write lock to suspend the delivery of
     * cache updates while there are loads in progress.
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    /** Logger. **/
    @Inject
    private Logger logger;

    /** The right cache. */
    @Inject
    private SecurityCache securityCache;

    /** The security reference factory. */
    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    /** Document reference resolver. */
    @Inject
    private DocumentReferenceResolver<String> resolver;

    /** User document reference resolver. */
    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> userResolver;

    /** Entity reference serializer. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** Execution object. */
    @Inject
    private Execution execution;

    /**
     * @return the current {@code XWikiContext}
     */
    private XWikiContext getXWikiContext() {
        return ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY));
    }

    @Override
    public void suspend()
    {
        readWriteLock.readLock().lock();
    }

    @Override
    public void resume()
    {
        readWriteLock.readLock().unlock();
    }

    @Override
    public String getName()
    {
        return getClass().getName();
    }

    @Override
    public List<Event> getEvents()
    {
        Event[] events = {
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(),
        };
        return Arrays.asList(events);
    }

    /**
     * Obtain a document reference to the {@link com.xpn.xwiki.doc.XWikiDocument} given
     * as parameter.
     * @param xwikiDocument The xwiki document.
     * @return The document reference.
     */
    private static DocumentReference getDocumentReference(Object xwikiDocument)
    {
        XWikiDocument doc = (XWikiDocument) xwikiDocument;
        return doc.getDocumentReference();
    }

    /**
     * @param source an xwiki document, that has just been updated.
     * @return true if and only if the xwiki document corresponds to a group.
     */
    private boolean isGroupDocument(Object source)
    {
        XWikiDocument doc = (XWikiDocument) source;
        DocumentReference docRef = doc.getDocumentReference();
        DocumentReference groupClass = resolver.resolve(XWikiConstants.GROUP_CLASS, docRef);
        List objects = doc.getXObjects(groupClass);
        return objects != null && objects.size() > 0;
    }

    /**
     * Due to the special case where a user have been added to the
     * group, we need to step through all members of the group and
     * remove all corresponding user entries.
     * @param group The group.
     * @param securityCache Right cache instance to invalidate.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    public void invalidateGroupMembers(DocumentReference group, SecurityCache securityCache)
        throws AuthorizationException
    {
        try {
            XWikiContext xwikiContext = getXWikiContext();
            XWikiGroupService groupService = xwikiContext.getWiki().getGroupService(xwikiContext);
            String groupName = serializer.serialize(group);
            
            // The group members inherit the wiki from the group
            // itself, unless the wiki name is explicitly given.

            WikiReference wikiReference = group.getWikiReference();
            final int nb = 100;
            int i = 0;
            Collection<String> memberNames;
            do {
                memberNames = groupService.getAllMembersNamesForGroup(groupName, nb, i * nb, xwikiContext);
                for (String member : memberNames) {
                    DocumentReference memberRef = userResolver.resolve(member, wikiReference);

                    // Avoid infinite loops.

                    if (!memberRef.equals(group)) {
                        securityCache.remove(securityReferenceFactory.newUserReference(memberRef));
                    }
                }
                i++;
            } while(memberNames.size() == nb);
        } catch (XWikiException e) {
            throw new AuthorizationException("Failed to invalidate group member.", e);
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference ref = getDocumentReference(source);
        readWriteLock.writeLock().lock();
        try {
            deliverUpdateEvent(ref);
            if (isGroupDocument(source)) {
                invalidateGroupMembers(ref, securityCache);
            }
        } catch (AuthorizationException e) {
            this.logger.error("Failed to invalidate group members on the document: {}", ref, e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Describe {@code deliverUpdateEvent} method here.
     *
     * @param ref Reference to the document that should be
     * invalidated.
     */
    private void deliverUpdateEvent(DocumentReference ref)
    {
        if (ref.getName().equals(XWikiConstants.WIKI_DOC)
            && ref.getLastSpaceReference().getName().equals(XWikiConstants.WIKI_SPACE)) {
            securityCache.remove(securityReferenceFactory.newEntityReference(ref.getWikiReference()));
        } else if (ref.getName().equals(XWikiConstants.SPACE_DOC)) {
            securityCache.remove(securityReferenceFactory.newEntityReference(ref.getParent()));
        } else {
            securityCache.remove(securityReferenceFactory.newEntityReference(ref));
        }
    }
}
