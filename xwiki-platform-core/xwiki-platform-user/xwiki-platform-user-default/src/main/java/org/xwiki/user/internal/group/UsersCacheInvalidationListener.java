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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Clean the {@link UsersCache} based on events.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component
@Named(UsersCacheInvalidationListener.NAME)
@Singleton
@Priority(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY)
public class UsersCacheInvalidationListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.user.internal.group.UserCacheInvalidationListener";

    private static final String USERS_CLASSNAME = "XWiki.XWikiUsers";

    @Inject
    private UsersCache userCache;

    /**
     * Default constructor.
     */
    public UsersCacheInvalidationListener()
    {
        super(NAME, new WikiDeletedEvent(), new XObjectDeletedEvent(BaseObjectReference.any(USERS_CLASSNAME)),
            new XObjectAddedEvent(BaseObjectReference.any(USERS_CLASSNAME)), new XObjectPropertyUpdatedEvent(
                new EntityReference("active", EntityType.OBJECT_PROPERTY, BaseObjectReference.any(USERS_CLASSNAME))));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            WikiReference wikiReference = new WikiReference(((WikiDeletedEvent) event).getWikiId());
            this.userCache.cleanCache(wikiReference.getName());
        } else {
            XWikiDocument document = (XWikiDocument) source;

            this.userCache.cleanCache(document.getDocumentReference().getWikiReference().getName());
        }
    }
}
