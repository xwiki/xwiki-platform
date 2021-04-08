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
package org.xwiki.notifications.notifiers.internal.email;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener to event to invalidate the cache of IntervalUsersManager.
 * 
 * @version $Id$
 * @since 11.10.6
 * @since 12.6
 */
@Component
@Named(IntervalUsersManagerInvalidator.NAME)
@Singleton
public class IntervalUsersManagerInvalidator extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "IntervalUsersManagerInvalidator";

    private static final EntityReference USER_OBJECT = BaseObjectReference.any("XWiki.XWikiUsers");

    private static final EntityReference INTERVAL_OBJECT =
        BaseObjectReference.any("XWiki.Notifications.Code.NotificationEmailPreferenceClass");

    @Inject
    private IntervalUsersManager users;

    /**
     * The default constructor.
     */
    public IntervalUsersManagerInvalidator()
    {
        super(NAME, new XObjectAddedEvent(USER_OBJECT), new XObjectDeletedEvent(USER_OBJECT),
            new XObjectUpdatedEvent(USER_OBJECT), new XObjectUpdatedEvent(INTERVAL_OBJECT), new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            this.users.invalidateWiki(((WikiDeletedEvent) event).getWikiId());
        } else {
            this.users.invalidateUser(((XWikiDocument) source).getDocumentReference());
        }
    }
}
