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
package org.xwiki.notifications.sources.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.sources.NotificationParameters;

/**
 * Handle the "users" parameters of the REST API.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Component(roles = UsersParameterHandler.class)
@Singleton
public class UsersParameterHandler
{
    private static final String FIELD_SEPARATOR = ",";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> defaultDocumentReferenceResolver;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private Logger logger;


    /**
     * Handle the "users" parameters of the REST API.
     * 
     * @param users the parameter
     * @param parameters the notifications parameters to fill
     */
    public void handleUsersParameter(String users, NotificationParameters parameters)
    {
        if (StringUtils.isNotBlank(users)) {
            String[] userArray = users.split(FIELD_SEPARATOR);
            List<String> userList = new ArrayList<>();
            for (int i = 0; i < userArray.length; ++i) {
                String user = userArray[i].trim();
                if (!user.contains(".")) {
                    user = "XWiki." + user;
                }
                DocumentReference userReference = currentDocumentReferenceResolver.resolve(user);
                if (documentAccessBridge.exists(userReference)) {
                    userList.add(entityReferenceSerializer.serialize(userReference));
                } else {
                    userList.add(entityReferenceSerializer.serialize(defaultDocumentReferenceResolver.resolve(user)));
                }
            }
            parameters.filters.add(new FollowedUserOnlyEventFilter(entityReferenceSerializer, userList));

            addFilterPreference(parameters, userList);
        } else if (parameters.user != null) {
            // if we have a user (but no "users") then we should also display personal messages from followed users.
            // the other types of messages get included, but for personal messages the filter needs
            // a matching filter preference so we loop though preferences to see if they have
            // a preference for this (using a copy to guard against unwanted modifications)
            try {
                for (NotificationFilterPreference filterPref
                    : notificationFilterPreferenceManager.getFilterPreferences(parameters.user)) {
                    if (EventUserFilter.FILTER_NAME.equals(filterPref.getFilterName())) {
                        DefaultNotificationFilterPreference personalPref
                            = new DefaultNotificationFilterPreference(filterPref);
                        parameters.filterPreferences.add(personalPref);
                    }
                }
            } catch (NotificationException e) {
                logger.error("failed to fetch the notification preferences for user [{}]:",
                    entityReferenceSerializer.serialize(parameters.user), e);
            }
        }
    }

    private void addFilterPreference(NotificationParameters parameters, List<String> userList)
    {
        Set<NotificationFormat> formats = new HashSet<>();
        formats.add(parameters.format);
        for (String userId : userList) {
            DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
            pref.setId(String.format("userRestFilters_%s", userId));
            pref.setFilterType(NotificationFilterType.INCLUSIVE);
            pref.setEnabled(true);
            pref.setNotificationFormats(formats);
            pref.setUser(userId);
            parameters.filterPreferences.add(pref);
        }
    }
}
