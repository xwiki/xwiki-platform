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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.event.RightUpdatedEvent;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiGroupsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;

/**
 * This class monitors updates and invalidates right cache entries whenever necessary.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named(DefaultSecurityCacheRulesInvalidatorListener.NAME)
@Singleton
@Priority(SecurityCache.CACHE_INVALIDATION_PRIORITY)
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
     * @param document an xwiki document, that has just been updated.
     * @return true if and only if the xwiki document corresponds to a group.
     */
    private boolean isGroupDocument(XWikiDocument document)
    {
        DocumentReference docRef = document.getDocumentReference();
        DocumentReference groupClass = resolver.resolve(XWikiConstants.GROUP_CLASS, docRef);
        List<BaseObject> objects = document.getXObjects(groupClass);

        return objects != null && objects.size() > 0;
    }

    private void invalidateNewGroupMembers(XWikiDocument document)
    {
        DocumentReference documentReference = document.getDocumentReference();
        List<BaseObject> newMembers =
            document.getXObjects(XWikiGroupsDocumentInitializer.XWIKI_GROUPS_DOCUMENT_REFERENCE);
        List<BaseObject> originalMembers =
            document.getOriginalDocument().getXObjects(XWikiGroupsDocumentInitializer.XWIKI_GROUPS_DOCUMENT_REFERENCE);

        for (BaseObject newMemberObject : newMembers) {
            if (newMemberObject != null) {
                String newMember = newMemberObject.getStringValue(XWikiGroupsDocumentInitializer.PROPERTY_MEMBER);

                BaseObject originalMemberObject = originalMembers.size() > newMemberObject.getNumber()
                    ? originalMembers.get(newMemberObject.getNumber()) : null;

                if (originalMemberObject == null) {
                    // Invalidate new member
                    DocumentReference memberRefeference = this.userResolver.resolve(newMember, documentReference);
                    this.securityCache.remove(this.securityReferenceFactory.newUserReference(memberRefeference));
                } else {
                    String originalMember = originalMemberObject.getStringValue("member");

                    if (!Objects.equals(newMember, originalMember)) {
                        // Invalidate modified member
                        DocumentReference memberRefeference = this.userResolver.resolve(newMember, documentReference);
                        this.securityCache.remove(this.securityReferenceFactory.newUserReference(memberRefeference));
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        DocumentReference ref = document.getDocumentReference();
        deliverUpdateEvent(ref);
        if (isGroupDocument(document)) {
            // When a group receive a new member, the update event is triggered and the above invalidate the
            // group and also all its existing members already in cache, but NOT the new member that could be
            // currently in the cache, and is not yet linked to the group. The following method is in charge of
            // invalidating any member which changed during the save (which include new group member or more complex
            // changed of the group like replacing a member by another).
            invalidateNewGroupMembers(document);
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
