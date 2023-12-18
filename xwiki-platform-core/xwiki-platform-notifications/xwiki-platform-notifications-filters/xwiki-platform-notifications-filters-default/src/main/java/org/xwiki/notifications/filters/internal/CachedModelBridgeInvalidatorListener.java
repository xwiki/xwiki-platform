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
package org.xwiki.notifications.filters.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceAddOrUpdatedEvent;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceDeletedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * A listener used to invalidate the notification event cache when a new event is stored.
 * 
 * @version $Id$
 * @since 13.4.4
 * @since 12.10.10
 * @since 13.8RC1
 */
@Component
@Singleton
@Named(CachedModelBridgeInvalidatorListener.NAME)
public class CachedModelBridgeInvalidatorListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.notifications.filters.internal.CachedModelBridgeInvalidatorListener";

    @Inject
    @Named("cached")
    private FilterPreferencesModelBridge bridge;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    /**
     * The default constructor.
     */
    public CachedModelBridgeInvalidatorListener()
    {
        super(NAME, new NotificationFilterPreferenceAddOrUpdatedEvent(),
            new NotificationFilterPreferenceDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (source instanceof DefaultNotificationFilterPreference) {
            DefaultNotificationFilterPreference preferences = (DefaultNotificationFilterPreference) source;

            String owner = preferences.getOwner();

            if (owner != null) {
                if (StringUtils.contains(owner, ':')) {
                    // Assume it's a document reference
                    ((CachedFilterPreferencesModelBridge) this.bridge)
                        .invalidatePreferencefilter(this.resolver.resolve(owner));
                } else {
                    // Assume it's a wiki reference
                    ((CachedFilterPreferencesModelBridge) this.bridge)
                        .invalidatePreferencefilter(new WikiReference(owner));
                }
            }
        } else if (source instanceof DocumentReference) {
            ((CachedFilterPreferencesModelBridge) this.bridge).invalidatePreferencefilter((DocumentReference) source);
        } else if (source instanceof WikiReference) {
            ((CachedFilterPreferencesModelBridge) this.bridge).invalidatePreferencefilter((WikiReference) source);
        }
    }
}
