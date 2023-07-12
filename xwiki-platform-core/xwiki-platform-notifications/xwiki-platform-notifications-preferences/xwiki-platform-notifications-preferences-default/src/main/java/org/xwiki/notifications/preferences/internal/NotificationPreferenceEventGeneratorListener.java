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
package org.xwiki.notifications.preferences.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceAddedEvent;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceDeletedEvent;
import org.xwiki.notifications.preferences.internal.event.NotificationPreferenceUpdatedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * A listener used to generate notification preference event when corresponding xobjects are manipulated.
 * 
 * @version $Id$
 * @since 10.11.4
 * @since 11.2
 */
@Component
@Singleton
@Named(NotificationPreferenceEventGeneratorListener.NAME)
public class NotificationPreferenceEventGeneratorListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "NotificationPreferenceEventGeneratorListener";

    private static final RegexEntityReference REFERENCE =
        BaseObjectReference.any(DefaultNotificationPreferenceModelBridge.NOTIFICATION_PREFERENCE_CLASS_STRING);

    private static final RegexEntityReference GROUPING_STRATEGY_PREFERENCE_REFERENCE =
        BaseObjectReference.any(NotificationEventGroupingStrategyPreferenceDocumentInitializer.REFERENCE_STRING);

    @Inject
    private ObservationManager observation;

    /**
     * The default constructor.
     */
    public NotificationPreferenceEventGeneratorListener()
    {
        super(NAME,
            new XObjectAddedEvent(REFERENCE),
            new XObjectUpdatedEvent(REFERENCE),
            new XObjectDeletedEvent(REFERENCE),
            new XObjectAddedEvent(GROUPING_STRATEGY_PREFERENCE_REFERENCE),
            new XObjectUpdatedEvent(GROUPING_STRATEGY_PREFERENCE_REFERENCE),
            new XObjectDeletedEvent(GROUPING_STRATEGY_PREFERENCE_REFERENCE)
        );
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof XObjectAddedEvent) {
            this.observation.notify(new NotificationPreferenceAddedEvent(),
                getEntityReference(((XWikiDocument) source).getDocumentReference()));
        } else if (event instanceof XObjectUpdatedEvent) {
            this.observation.notify(new NotificationPreferenceUpdatedEvent(),
                getEntityReference(((XWikiDocument) source).getDocumentReference()));
        } else if (event instanceof XObjectDeletedEvent) {
            this.observation.notify(new NotificationPreferenceDeletedEvent(),
                getEntityReference(((XWikiDocument) source).getDocumentReference()));
        }
    }

    private EntityReference getEntityReference(DocumentReference source)
    {
        if (source.getLocalDocumentReference().equals(DefaultNotificationPreferenceModelBridge.GLOBAL_PREFERENCES)) {
            return source.getWikiReference();
        } else {
            return source;
        }
    }
}
