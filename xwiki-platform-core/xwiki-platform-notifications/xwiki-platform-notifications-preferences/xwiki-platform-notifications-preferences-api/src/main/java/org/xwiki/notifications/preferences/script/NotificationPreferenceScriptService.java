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
package org.xwiki.notifications.preferences.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.notifications.preferences.email.NotificationEmailDiffType;
import org.xwiki.notifications.preferences.email.NotificationEmailUserPreferenceManager;
import org.xwiki.notifications.preferences.internal.UserProfileNotificationPreferenceProvider;
import org.xwiki.notifications.preferences.internal.WikiNotificationPreferenceProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.internal.document.DocumentUserReference;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Script service for the notification preferences.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.preferences")
@Singleton
public class NotificationPreferenceScriptService implements ScriptService
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private Provider<TargetableNotificationPreferenceBuilder> targetableNotificationPreferenceBuilderProvider;

    @Inject
    private NotificationEmailUserPreferenceManager emailUserPreferenceManager;

    private void saveNotificationPreferences(String json, String providerHint, EntityReference target,
        NotificationPreferenceCategory category) throws NotificationException
    {
        /*
            The JSON we get is a "snapshot" of the states of the buttons the user has in front of her eyes when she is
            managing her preferences.

            We did that so we can save several preferences in the same time without making too much requests (for
            example when the user enable an application, it actually enable all the app's event types).

            However, this snapshot also "freeze" the default preferences, ie the preferences set at the wiki level and
            that the user has not changed.

            Example:
            1. Wiki Administrator enables the application A by default, and doesn't touch the settings for the
               application B.
            2. John Doe disables the application A on HIS profile. He doesn't touch the setting for the application B.
               A "snapshot" of his preferences is saved.
            3. Wiki Administrator enables the application B by default.
            4. On the John Doe's preferences, application B is still disabled, because of the snapshot done on step 2.

            I don't think this situation is good. If a user did not EXPLICITLY change a setting, the default settings
            should be applied.

            For this reason, this code will only save the settings THAT ARE DIFFERENT FROM THE DEFAULT (INHERITED).

            Since this logic is totally related to the way the UI is built, I think it does not deserve its own
            component or API. In a way, it should even be internal.
        */

        List<NotificationPreference> existingPreferences = Collections.emptyList();
        if (target instanceof DocumentReference) {
            existingPreferences = notificationPreferenceManager.getAllPreferences((DocumentReference) target);
        } else if (target instanceof WikiReference) {
            existingPreferences = notificationPreferenceManager.getAllPreferences((WikiReference) target);
        }

        // Instantiate a new copy of TargetableNotificationPreferenceBuilder because this component is not thread-safe.
        TargetableNotificationPreferenceBuilder targetableNotificationPreferenceBuilder
                = targetableNotificationPreferenceBuilderProvider.get();

        List<NotificationPreference> toSave = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> preferences = objectMapper.reader().forType(List.class).readValue(json);
            for (Map<String, Object> item : preferences) {
                String eventType = (String) item.get("eventType");
                Map<NotificationPreferenceProperty, Object> propertyMap = Collections.emptyMap();

                if (!StringUtils.isEmpty(eventType)) {
                    propertyMap = Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, eventType);
                }

                NotificationFormat format = NotificationFormat.valueOf(((String) item.get("format")).toUpperCase());
                boolean enabled = (Boolean) item.get("enabled");

                TargetableNotificationPreference newPreference = targetableNotificationPreferenceBuilder.prepare()
                    .setEnabled(enabled)
                    .setFormat(format)
                    .setProviderHint(providerHint)
                    .setProperties(propertyMap)
                    .setTarget(target)
                    .setCategory(category)
                    .build();

                // This part is explained by the long comment below
                NotificationPreference correspondingPreference = getCorrespondingPreference(existingPreferences,
                        newPreference);
                if (correspondingPreference == null
                        || correspondingPreference.isNotificationEnabled() != newPreference.isNotificationEnabled()) {
                    toSave.add(newPreference);
                }
            }

            notificationPreferenceManager.savePreferences(toSave);

        } catch (Exception e) {
            throw new NotificationException("Failed to save preferences for notifications given as JSON.", e);
        }
    }

    private NotificationPreference getCorrespondingPreference(List<NotificationPreference> existingPreferences,
        TargetableNotificationPreference newPreference)
    {
        for (NotificationPreference pref: existingPreferences) {
            // This code heavily
            // depends on org.xwiki.notifications.preferences.internal.AbstractNotificationPreference.equals()
            // Please look at it.
            if (pref.equals(newPreference)) {
                return pref;
            }
        }
        return null;
    }

    /**
     * Save preferences given as JSON.
     * @param json a list of preferences as JSON
     * @param userReference reference of the user concerned by the preferences
     * @throws NotificationException if error happens
     * @throws AccessDeniedException if the current user has not the right to edit the given document
     */
    public void saveNotificationPreferences(String json, DocumentReference userReference)
            throws NotificationException, AccessDeniedException
    {
        authorizationManager.checkAccess(Right.EDIT, userReference);
        saveNotificationPreferences(json, UserProfileNotificationPreferenceProvider.NAME, userReference,
                NotificationPreferenceCategory.DEFAULT);
    }

    /**
     * Update notification preferences of the given user.
     *
     * @param json a list of notification preferences represented as JSON
     * @throws NotificationException if an error occurs
     */
    public void saveNotificationPreferences(String json) throws NotificationException
    {
        try {
            saveNotificationPreferences(json, documentAccessBridge.getCurrentUserReference());
        } catch (AccessDeniedException e) {
            // Should never happen
            throw new NotificationException(String.format("User [%s] has not the right to edit its own profile!",
                    documentAccessBridge.getCurrentDocumentReference()), e);
        }
    }

    /**
     * Update notification preferences of the current wiki.
     *
     * @param json a list of notification preferences represented as JSON
     * @throws NotificationException if an error occurs
     * @throws AccessDeniedException if the current user has not the right to administrate the current wiki
     */
    public void saveNotificationPreferencesForCurrentWiki(String json)
        throws NotificationException, AccessDeniedException
    {
        WikiReference currentWiki = documentAccessBridge.getCurrentDocumentReference().getWikiReference();
        authorizationManager.checkAccess(Right.ADMIN, currentWiki);

        saveNotificationPreferences(json, WikiNotificationPreferenceProvider.NAME, currentWiki,
                NotificationPreferenceCategory.SYSTEM);
    }

    /**
     * Set the start date for the current user.
     *
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    public void setStartDate(Date startDate) throws NotificationException
    {
        notificationPreferenceManager.setStartDateForUser(documentAccessBridge.getCurrentUserReference(), startDate);
    }

    /**
     * Set the start date for every notification preference of the given user.
     *
     * @param userId id of the user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    public void setStartDate(String userId, Date startDate) throws NotificationException
    {
        try {
            DocumentReference user = documentReferenceResolver.resolve(userId);
            this.authorizationManager.checkAccess(Right.EDIT, user);
            notificationPreferenceManager.setStartDateForUser(user, startDate);
        } catch (AccessDeniedException e) {
            throw new NotificationException(
                    String.format("Unable to save the start date of the notifications for the user [%s]", userId),
                    e);
        }
    }

    /**
     * @return if there is a least one preference enabled
     * @throws NotificationException if an error occurs
     * @since 9.9RC1
     */
    public boolean hasAnyEnabledNotificationPreferences() throws NotificationException
    {
        List<NotificationPreference> preferences
                = notificationPreferenceManager.getAllPreferences(documentAccessBridge.getCurrentUserReference());
        return preferences.isEmpty() || preferences.stream().anyMatch(NotificationPreference::isNotificationEnabled);
    }

    /**
     * @return the diff type for emails configured for the current user
     * @since 9.11RC1
     */
    public NotificationEmailDiffType getDiffType()
    {
        return emailUserPreferenceManager.getDiffType();
    }

    /**
     * @param userId id of a user
     * @return the diff type for emails configured for the given user
     * @since 9.11RC1
     */
    public NotificationEmailDiffType getDiffType(String userId)
    {
        return emailUserPreferenceManager.getDiffType(userReferenceResolver.resolve(userId));
    }

    /**
     * @param user reference of a user
     * @return the diff type for emails configured for the given user
     * @since 14.10
     */
    public NotificationEmailDiffType getDiffType(UserReference user)
    {
        return emailUserPreferenceManager.getDiffType(user);
    }

    /**
     * @return the email notification interval configured for the current user
     * @since 14.10
     */
    public NotificationEmailInterval getInterval()
    {
        return emailUserPreferenceManager.getInterval();
    }

    /**
     * @param user reference of a user
     * @return the notification email interval configured for the given user
     * @since 14.10
     */
    public NotificationEmailInterval getInterval(UserReference user)
    {
        return emailUserPreferenceManager.getInterval(user);
    }

    /**
     * @param eventType an event type
     * @param format a notification format
     * @param wiki id of the wiki
     * @return either or not the given event type is enabled by default on the given wiki in the given format
     * @throws NotificationException if an error happens
     * @throws AccessDeniedException if the current user has not the admin right on the wiki
     */
    public boolean isEventTypeEnabled(String eventType, NotificationFormat format, String wiki)
        throws NotificationException, AccessDeniedException
    {
        WikiReference wikiReference = new WikiReference(wiki);
        authorizationManager.checkAccess(Right.ADMIN, wikiReference);

        List<NotificationPreference> allPreferences = notificationPreferenceManager.getAllPreferences(wikiReference);
        return this.isEventTypeEnabled(eventType, format, allPreferences);
    }

    /**
     * @param eventType an event type
     * @param format a notification format
     * @return either or not the given event type is enabled for the current user in the given format
     * @throws NotificationException if an error happens
     */
    public boolean isEventTypeEnabled(String eventType, NotificationFormat format) throws NotificationException
    {
        return this.isEventTypeEnabledForUser(eventType, format, CurrentUserReference.INSTANCE);
    }

    /**
     * @param eventType an event type
     * @param format a notification format
     * @param userReference the reference of the user for which to check the event type
     * @return either or not the given event type is enabled for the given user in the given format
     * @throws NotificationException if an error happens
     * @since 13.2RC1
     */
    public boolean isEventTypeEnabledForUser(String eventType, NotificationFormat format, UserReference userReference)
        throws NotificationException
    {
        DocumentReference userDocumentReference;
        if (userReference == CurrentUserReference.INSTANCE) {
            userDocumentReference = documentAccessBridge.getCurrentUserReference();
        } else if (userReference instanceof DocumentUserReference) {
            userDocumentReference = ((DocumentUserReference) userReference).getReference();
        } else {
            throw new NotificationException(
                String.format("The method isEventTypeEnabledForUser should only be used with DocumentUserReference, "
                    + "the given reference was a [%s]", userReference.getClass().getSimpleName()));
        }
        List<NotificationPreference> allPreferences =
            notificationPreferenceManager.getAllPreferences(userDocumentReference);
        return this.isEventTypeEnabled(eventType, format, allPreferences);
    }

    private boolean isEventTypeEnabled(String eventType, NotificationFormat format,
        List<NotificationPreference> allPreferences)
    {
        for (NotificationPreference preference : allPreferences) {
            Object prefEventType = preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);
            if (prefEventType != null && StringUtils.equals((String) prefEventType, eventType)
                && preference.getFormat() == format) {
                return preference.isNotificationEnabled();
            }
        }
        return allPreferences.isEmpty();
    }
}
