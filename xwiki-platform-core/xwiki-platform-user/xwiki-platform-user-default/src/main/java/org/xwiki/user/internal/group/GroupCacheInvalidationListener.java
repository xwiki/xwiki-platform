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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * @version $Id$
 * @since 10.8RC1
 */
@Component
@Named(GroupCacheInvalidationListener.NAME)
@Singleton
public class GroupCacheInvalidationListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.user.internal.group.GroupInvalidationListener";

    private static final String GROUPS_CLASSNAME = "XWiki.XWikiGroups";

    private static final String USERS_CLASSNAME = "XWiki.XWikiUsers";

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
        super(NAME, new WikiDeletedEvent(), new XObjectDeletedEvent(BaseObjectReference.any(GROUPS_CLASSNAME)),
            new XObjectAddedEvent(BaseObjectReference.any(GROUPS_CLASSNAME)),
            new XObjectUpdatedEvent(BaseObjectReference.any(GROUPS_CLASSNAME)),
            new XObjectDeletedEvent(BaseObjectReference.any(USERS_CLASSNAME)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            WikiReference wikiReference = new WikiReference(((WikiDeletedEvent) event).getWikiId());
            this.groupsCache.cleanCache(wikiReference.getName());
            this.membersCache.cleanCache(wikiReference.getName());
        } else {
            XWikiDocument document = (XWikiDocument) source;

            DocumentReference documentReference = document.getDocumentReference();

            // Remove the entity from the cache
            this.groupsCache.cleanCache(documentReference);
            this.membersCache.cleanCache(documentReference);

            // Remove the previous and new group members from the cache
            XObjectEvent xobjectEvent = (XObjectEvent) event;
            ObjectReference reference = (ObjectReference) xobjectEvent.getReference();

            BaseObject newXObject = document.getXObject(reference);
            BaseObject previousXObject = document.getOriginalDocument().getXObject(reference);

            clean(newXObject, documentReference);
            clean(previousXObject, documentReference);
        }
    }

    private void clean(BaseObject xobject, DocumentReference groupReference)
    {
        if (xobject == null) {
            return;
        }

        String memberString = xobject.getStringValue("member");
        if (StringUtils.isNotEmpty(memberString)) {
            DocumentReference memberReference = this.resolver.resolve(memberString, groupReference);

            this.groupsCache.cleanCache(memberReference);
            this.membersCache.cleanCache(memberReference);
        }
    }
}
