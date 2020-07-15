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
package org.xwiki.mentions.internal.descriptors;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.internal.WikiNotificationPreferenceProvider;

import com.xpn.xwiki.XWikiContext;

import static org.xwiki.mentions.events.MentionEvent.EVENT_TYPE;

/**
 * Description of the user mentions notification. 
 * Used for instance in the notifications settings. 
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named(EVENT_TYPE)
public class MentionEventDescriptor implements RecordableEventDescriptor, Initializable
{
    @Inject
    private TargetableNotificationPreferenceBuilder targetableNotificationPreferenceBuilder;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * {@inheritDoc}
     *
     * This initialization save a new notification filter in preferences to enable automatically mentions notification.
     */
    @Override
    public void initialize() throws InitializationException
    {
        WikiReference wikiReference = this.contextProvider.get().getWikiReference();
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, MentionEvent.EVENT_TYPE);

        // Create the preference
        TargetableNotificationPreference notificationPreference = this.targetableNotificationPreferenceBuilder
            .prepare()
            .setCategory(NotificationPreferenceCategory.DEFAULT)
            .setEnabled(true)
            .setFormat(NotificationFormat.ALERT)
            .setProperties(properties)
            .setProviderHint(WikiNotificationPreferenceProvider.NAME)
            .setStartDate(new Date())
            .setTarget(wikiReference)
            .build();

        // Save it
        try {
            this.notificationPreferenceManager.savePreferences(Collections.singletonList(notificationPreference));
        } catch (NotificationException e) {
            // We don't throw an InitializationException since it doesn't prevent the component to be used.
            this.logger.warn("Error while enabling MentionEvent for the wiki {}: {}", wikiReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public String getEventType()
    {
        return EVENT_TYPE;
    }

    @Override
    public String getApplicationName()
    {
        return "mentions.application.name";
    }

    @Override
    public String getDescription()
    {
        return "mentions.mention.event.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "bell";
    }
}
