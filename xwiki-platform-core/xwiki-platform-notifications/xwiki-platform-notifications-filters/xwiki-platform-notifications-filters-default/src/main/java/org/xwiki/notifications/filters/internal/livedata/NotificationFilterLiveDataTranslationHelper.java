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
package org.xwiki.notifications.filters.internal.livedata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.livedata.custom.NotificationCustomFiltersLiveDataConfigurationProvider;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Helper for getting various translations for live data custom sources.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component(roles = NotificationFilterLiveDataTranslationHelper.class)
@Singleton
public class NotificationFilterLiveDataTranslationHelper
{
    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    private String getTranslationWithFallback(String translationKey)
    {
        String translationPlain = this.contextualLocalizationManager.getTranslationPlain(translationKey);
        if (translationPlain == null) {
            translationPlain = translationKey;
        }
        return translationPlain;
    }

    /**
     * Get a plain text translation with a fallback to the full key if none can be found.
     * @param prefix the translation prefix to use
     * @param key the key to add to the prefix
     * @return the plain text translation or the prefix and key if none can be found
     */
    public String getTranslationWithPrefix(String prefix, String key)
    {
        return getTranslationWithFallback(prefix + key);
    }

    /**
     * @param filterType the filter type for which to get a translation
     * @return the plain text translation of the filter type
     */
    public String getFilterTypeTranslation(NotificationFilterType filterType)
    {
        return getTranslationWithPrefix("notifications.filters.type.custom.", filterType.name().toLowerCase());
    }

    /**
     * @param scope the scope for which to get a translation
     * @return the plain text translation of the scope
     */
    public String getScopeTranslation(NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope)
    {
        return getTranslationWithPrefix("notifications.filters.preferences.scopeNotificationFilter.",
            scope.name().toLowerCase());
    }

    /**
     * @return the translation for all events.
     */
    public String getAllEventTypesTranslation()
    {
        return getTranslationWithFallback("notifications.filters.preferences.allEvents");
    }

    /**
     * @param eventType the event type for which to get a description translation
     * @return the plain text event type description translation
     * @throws LiveDataException if the event type descriptor cannot be found
     */
    public String getEventTypeTranslation(String eventType)
    {
        String result = eventType;
        try {
            RecordableEventDescriptor descriptor =
                this.recordableEventDescriptorManager.getDescriptorForEventType(eventType, true);
            if (descriptor != null) {
                result = getTranslationWithFallback(descriptor.getDescription());
            }
        } catch (EventStreamException e) {
            this.logger.error("Error while getting description for event type [{}] falling back on event name",
                eventType, e);
        }
        return result;
    }

    /**
     * @param format the notification format for which to get a translation
     * @return the plain text translation of the format
     */
    public String getFormatTranslation(NotificationFormat format)
    {
        return getTranslationWithPrefix("notifications.format.", format.name().toLowerCase());
    }

    /**
     * Get event type information from all descriptor to populate a select.
     * @return a list of maps containing two information: {@code value} holding the event type, and {@code label}
     * holding a translation of the description
     * @throws LiveDataException in case of problem to load the descriptors
     */
    public List<Map<String, String>> getAllEventTypesOptions() throws LiveDataException
    {
        try {
            boolean isMainWiki = this.wikiDescriptorManager.isMainWiki(this.wikiDescriptorManager.getCurrentWikiId());
            List<RecordableEventDescriptor> recordableEventDescriptors =
                this.recordableEventDescriptorManager.getRecordableEventDescriptors(isMainWiki);
            return recordableEventDescriptors.stream().map(descriptor -> Map.of(
                "value", descriptor.getEventType(),
                "label", getTranslationWithFallback(descriptor.getDescription())
            )).collect(Collectors.toList());
        } catch (EventStreamException e) {
            throw new LiveDataException("Error while retrieving event descriptors", e);
        }
    }
}
