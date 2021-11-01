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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.event.RightUpdatedEvent;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * The instance of this class monitors updates and invalidates right cache entries whenever necessary.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named(DefaultSecurityCacheRulesInvalidatorListener.NAME)
@Singleton
public class DefaultSecurityCacheRulesInvalidatorListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME =
        "org.xwiki.security.authorization.internal.DefaultSecurityCacheRulesInvalidatorListener";

    private static final LocalDocumentReference XWIKISERVER_CLASS =
        new LocalDocumentReference("XWiki", "XWikiServerClass");

    private static final Set<LocalDocumentReference> RIGHT_OBJECTS =
        new HashSet<>(Arrays.asList(XWikiConstants.GROUP_CLASS_REFERENCE, XWikiConstants.GLOBAL_CLASS_REFERENCE,
            XWikiConstants.LOCAL_CLASS_REFERENCE, XWIKISERVER_CLASS));

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
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationManager observation;

    /**
     * Default constructor.
     */
    public DefaultSecurityCacheRulesInvalidatorListener()
    {
        super(NAME, new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    /**
     * @param source an xwiki document, that has just been updated.
     * @return true if and only if the xwiki document corresponds to a group.
     */
    private boolean isGroupDocument(XWikiDocument document)
    {
        DocumentReference docRef = document.getDocumentReference();
        DocumentReference groupClass = resolver.resolve(XWikiConstants.GROUP_CLASS, docRef);
        List<BaseObject> objects = document.getXObjects(groupClass);

        return objects != null && objects.size() > 0;
    }

    /**
     * Drop from the cache all members of a given group.
     * 
     * @param group The group.
     * @param securityCache Right cache instance to invalidate.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    public void invalidateGroupMembers(DocumentReference group, SecurityCache securityCache)
        throws AuthorizationException
    {
        try {
            XWikiContext xwikiContext = this.xcontextProvider.get();
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
            } while (memberNames.size() == nb);
        } catch (XWikiException e) {
            throw new AuthorizationException("Failed to invalidate group member.", e);
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        DocumentReference ref = document.getDocumentReference();
        try {
            deliverUpdateEvent(ref);
            if (isGroupDocument(document)) {
                // When a group receive a new member, the update event is triggered and the above invalidate the
                // group and also all its existing members already in cache, but NOT the new member that could be
                // currently in the cache, and is not yet linked to the group. Here, we invalidate individually all
                // members of the group based on the updated group, which will only have the effect of invalidating
                // new members.
                invalidateGroupMembers(ref, securityCache);
            }
        } catch (AuthorizationException e) {
            this.logger.error("Failed to invalidate group members on the document: {}", ref, e);
        }

        // Make sure to send the RightUpdatedEvent event after the security cache is cleaned
        // FIXME: for some reason if one of the event that listen to RightUpdatedEvent check the right it can put the
        // cache in a bad state. See https://jira.xwiki.org/browse/XWIKI-16381.
        if (shouldSendRightUpdatedEvent(document, (XWikiContext) data)) {
            // Notify that a right may have changed
            this.observation.notify(new RightUpdatedEvent(), source);
        }
    }

    private boolean shouldSendRightUpdatedEvent(XWikiDocument document, XWikiContext xcontext)
    {
        List<List<ObjectDiff>> documentDiff =
            document.getObjectDiff(document.getOriginalDocument(), document, xcontext);

        for (List<ObjectDiff> objecstDiff : documentDiff) {
            for (ObjectDiff objectDiff : objecstDiff) {
                if (RIGHT_OBJECTS.contains(objectDiff.getXClassReference().getLocalDocumentReference())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Describe {@code deliverUpdateEvent} method here.
     *
     * @param ref Reference to the document that should be invalidated.
     */
    private void deliverUpdateEvent(DocumentReference ref)
    {
        if (XWikiConstants.WIKI_DOC_REFERENCE.equals(ref, EntityType.SPACE)) {
            // For XWiki.XWikiPreferences, remove the whole wiki.
            securityCache.remove(securityReferenceFactory.newEntityReference(ref.getWikiReference()));
        } else if (ref.getName().equals(XWikiConstants.SPACE_DOC)) {
            // For WebPreferences, remove the whole space.
            securityCache.remove(securityReferenceFactory.newEntityReference(ref.getParent()));
        } else {
            // For any other documents, remove that document cache.
            securityCache.remove(securityReferenceFactory.newEntityReference(ref));

            // If it's a wiki descriptor remove the wiki reference from the cache
            if (ref.getName().startsWith(XWikiConstants.WIKI_DESCRIPTOR_PREFIX)
                && XWikiConstants.XWIKI_SPACE_REFERENCE.equals(ref.getLastSpaceReference(), EntityType.SPACE)
                && ref.getWikiReference().getName().equals(this.xcontextProvider.get().getMainXWiki())) {
                // For xwiki:XWiki.XWikiServer... documents, also remove the whole corresponding wiki.
                securityCache.remove(securityReferenceFactory.newEntityReference(new WikiReference(
                    ref.getName().substring(XWikiConstants.WIKI_DESCRIPTOR_PREFIX.length()).toLowerCase())));
            }
        }
    }
}
