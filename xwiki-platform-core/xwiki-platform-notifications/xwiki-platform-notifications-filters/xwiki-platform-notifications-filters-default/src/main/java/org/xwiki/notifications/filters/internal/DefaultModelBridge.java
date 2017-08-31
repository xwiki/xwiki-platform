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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link ModelBridge}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
            new SpaceReference("Notifications",
                    new SpaceReference("XWiki", new WikiReference("xwiki"))
            )
    );

    private static final DocumentReference NOTIFICATION_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "NotificationFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final DocumentReference TOGGLEABLE_FILTER_PREFERENCE_CLASS = new DocumentReference(
            "ToggleableFilterPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final String FILTER_NAME = "filterName";

    private static final String IS_ENABLED = "isEnabled";

    private static final String IS_ACTIVE = "isActive";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationFilterPreferenceClass
                = NOTIFICATION_FILTER_PREFERENCE_CLASS.setWikiReference(user.getWikiReference());

        Set<NotificationFilterPreference> preferences = new HashSet<>();

        try {
            XWikiDocument doc = xwiki.getDocument(user, context);
            List<BaseObject> preferencesObj = doc.getXObjects(notificationFilterPreferenceClass);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        Map<NotificationFilterProperty, List<String>> filterPreferenceProperties = new HashMap<>();

                        filterPreferenceProperties.put(NotificationFilterProperty.APPLICATION,
                                obj.getListValue("applications"));
                        filterPreferenceProperties.put(NotificationFilterProperty.EVENT_TYPE,
                                obj.getListValue("eventTypes"));
                        filterPreferenceProperties.put(NotificationFilterProperty.PAGE,
                                obj.getListValue("pages"));
                        filterPreferenceProperties.put(NotificationFilterProperty.SPACE,
                                obj.getListValue("spaces"));
                        filterPreferenceProperties.put(NotificationFilterProperty.WIKI,
                                obj.getListValue("wikis"));

                        NotificationFilterType filterType = NotificationFilterType.valueOf(
                                obj.getStringValue("filterType").toUpperCase());

                        Set<NotificationFormat> filterFormats = new HashSet<>();
                        for (String format : (List<String>) obj.getListValue("filterFormats")) {
                            filterFormats.add(NotificationFormat.valueOf(format.toUpperCase()));
                        }

                        // Create the new filter preference and add it to the list of preferences
                        DefaultNotificationFilterPreference notificationFilterPreference
                                = new DefaultNotificationFilterPreference(
                                        obj.getStringValue("filterPreferenceName"));

                        notificationFilterPreference.setProviderHint("userProfile");
                        notificationFilterPreference.setFilterName(obj.getStringValue(FILTER_NAME));
                        notificationFilterPreference.setEnabled(obj.getIntValue(IS_ENABLED, 1) == 1);
                        notificationFilterPreference.setActive(obj.getIntValue(IS_ACTIVE, 1) == 1);
                        notificationFilterPreference.setFilterType(filterType);
                        notificationFilterPreference.setNotificationFormats(filterFormats);
                        notificationFilterPreference.setPreferenceProperties(filterPreferenceProperties);

                        preferences.add(notificationFilterPreference);
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences scope for the user [%s].",
                            user), e);
        }

        return preferences;
    }

    @Override
    public Set<String> getDisabledNotificationFiltersHints(DocumentReference userReference)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesScopeClass
                = TOGGLEABLE_FILTER_PREFERENCE_CLASS.setWikiReference(userReference.getWikiReference());

        Set<String> disabledFilters = new HashSet<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            List<BaseObject> preferencesObj = doc.getXObjects(notificationPreferencesScopeClass);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj.getIntValue(IS_ENABLED, 1) == 0) {
                        disabledFilters.add(obj.getStringValue(FILTER_NAME));
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the toggleable filters preferences for the user [%s].",
                            userReference), e);
        }

        return disabledFilters;
    }
}
