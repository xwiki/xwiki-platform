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
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.notifications.filters.NotificationFilterType;

@Component(roles = NotificationFilterLiveDataTranslationHelper.class)
@Singleton
public class NotificationFilterLiveDataTranslationHelper
{
    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    private String getTranslationWithFallback(String translationKey)
    {
        String translationPlain = this.contextualLocalizationManager.getTranslationPlain(translationKey);
        if (translationPlain == null) {
            translationPlain = translationKey;
        }
        return translationPlain;
    }

    public String getTranslationWithPrefix(String prefix, String key)
    {
        return getTranslationWithFallback(prefix + key);
    }

    public String getFilterTypeTranslation(NotificationFilterType filterType)
    {
        return getTranslationWithPrefix("notifications.filters.type.custom.", filterType.name().toLowerCase());
    }

    public String getScopeTranslation(NotificationFiltersLiveDataConfigurationProvider.Scope scope)
    {
        return getTranslationWithPrefix("notifications.filters.preferences.scopeNotificationFilter.",
            scope.name().toLowerCase());
    }

    public String getAllEventTypesTranslation()
    {
        return getTranslationWithFallback("notifications.filters.preferences.allEvents");
    }

    public String getEventTypeTranslation(String eventType) throws LiveDataException
    {
        try {
            RecordableEventDescriptor descriptor =
                this.recordableEventDescriptorManager.getDescriptorForEventType(eventType, true);
            return getTranslationWithFallback(descriptor.getDescription());
        } catch (EventStreamException e) {
            throw new LiveDataException(
                String.format("Error while getting description for event type [%s]", eventType), e);
        }
    }

    public List<Map<String, String>> getAllEventTypesOptions(boolean allFarm) throws LiveDataException
    {
        try {
            List<RecordableEventDescriptor> recordableEventDescriptors =
                this.recordableEventDescriptorManager.getRecordableEventDescriptors(allFarm);
            return recordableEventDescriptors.stream().map(descriptor -> Map.of(
                "value", descriptor.getEventType(),
                "label", getTranslationWithFallback(descriptor.getDescription())
            )).collect(Collectors.toList());
        } catch (EventStreamException e) {
            throw new LiveDataException("Error while retrieving event descriptors", e);
        }
    }
}
