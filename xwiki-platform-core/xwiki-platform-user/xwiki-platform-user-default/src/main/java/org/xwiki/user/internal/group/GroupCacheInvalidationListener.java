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
package org.xwiki.user.internal.group;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.cache.SecurityCache;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiGroupsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named(GroupCacheInvalidationListener.NAME)
@Singleton
// We want to invalidate the group cache before the security cache
@Priority(SecurityCache.CACHE_INVALIDATION_PRIORITY - 1)
public class GroupCacheInvalidationListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.user.internal.group.GroupInvalidationListener";

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private GroupsCache groupsCache;

    @Inject
    private MembersCache membersCache;

    /**
     * Default constructor.
     */
    public GroupCacheInvalidationListener()
    {
        super(NAME, new WikiDeletedEvent(), new DocumentCreatedEvent(), new DocumentUpdatedEvent(),
            new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            WikiReference wikiReference = new WikiReference(((WikiDeletedEvent) event).getWikiId());
            this.groupsCache.cleanCache(wikiReference.getName());
            this.membersCache.cleanCache(wikiReference.getName());
        } else {
            XWikiDocument newDocument = (XWikiDocument) source;
            XWikiDocument previousDocument = newDocument.getOriginalDocument();

            DocumentReference documentReference = newDocument.getDocumentReference();

            // Remove the entity from the cache
            this.groupsCache.cleanCache(documentReference);
            this.membersCache.cleanCache(documentReference);

            // Remove the previous and new group members from the cache
            Set<DocumentReference> previousMembers = getMembers(previousDocument);
            Set<DocumentReference> newMembers = getMembers(newDocument);

            invalidate(previousMembers, newMembers);
            invalidate(newMembers, previousMembers);
        }
    }

    private void invalidate(Set<DocumentReference> members1, Set<DocumentReference> members2)
    {
        for (DocumentReference member : members1) {
            if (!members2.contains(member)) {
                this.groupsCache.cleanCache(member);
                this.membersCache.cleanCache(member);
            }
        }
    }

    private Set<DocumentReference> getMembers(XWikiDocument document)
    {
        List<BaseObject> memberObjects =
            document.getXObjects(XWikiGroupsDocumentInitializer.XWIKI_GROUPS_DOCUMENT_REFERENCE);

        if (memberObjects.isEmpty()) {
            // It's not a group
            return Set.of();
        }

        Set<DocumentReference> members = new HashSet<>(memberObjects.size());
        for (BaseObject memberObject : memberObjects) {
            if (memberObject != null) {
                String memberString = memberObject.getStringValue("member");
                if (StringUtils.isNotEmpty(memberString)) {
                    members.add(this.resolver.resolve(memberString, document.getDocumentReference()));
                }
            }
        }

        return members;
    }
}
