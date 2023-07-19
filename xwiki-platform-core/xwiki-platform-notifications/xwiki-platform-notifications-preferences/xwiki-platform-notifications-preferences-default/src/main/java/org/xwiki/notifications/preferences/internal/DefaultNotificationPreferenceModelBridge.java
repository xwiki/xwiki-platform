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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link NotificationPreferenceModelBridge}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultNotificationPreferenceModelBridge implements NotificationPreferenceModelBridge
{
    private static final String EVENT_TYPE_FIELD = "eventType";

    private static final String START_DATE_FIELD = "startDate";

    private static final String NOTIFICATION_ENABLED_FIELD = "notificationEnabled";

    private static final String FORMAT_FIELD = "format";

    private static final String WIKI_SPACE = "XWiki";

    private static final String NOTIFICATIONS = "Notifications";

    private static final String CODE = "Code";

    private static final String NOTIFICATION_PREFERENCE_CLASS_NAME = "NotificationPreferenceClass";

    /**
     * @since 10.11.4
     * @since 11.2RC1
     */
    public static final String NOTIFICATION_PREFERENCE_CLASS_STRING =
        WIKI_SPACE + '.' + NOTIFICATIONS + '.' + CODE + '.' + NOTIFICATION_PREFERENCE_CLASS_NAME;

    /**
     * @since 13.4.4
     * @since 12.10.10
     * @since 13.8RC1
     */
    public static final LocalDocumentReference NOTIFICATION_PREFERENCE_CLASS =
        new LocalDocumentReference(Arrays.asList(WIKI_SPACE, NOTIFICATIONS, CODE), NOTIFICATION_PREFERENCE_CLASS_NAME);

    /**
     * @since 13.4.4
     * @since 12.10.10
     * @since 13.8RC1
     */
    public static final LocalDocumentReference GLOBAL_PREFERENCES =
        new LocalDocumentReference(Arrays.asList(WIKI_SPACE, NOTIFICATIONS, CODE), "NotificationAdministration");

    private static final String WIKI_PROVIDER = "wiki";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<TargetableNotificationPreferenceBuilder> notificationPreferenceBuilderProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<NotificationPreferenceManager> notificationPreferenceManager;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
            throws NotificationException
    {
        return getNotificationPreferences(userReference, "userProfile");
    }

    @Override
    public List<NotificationPreference> getNotificationsPreferences(WikiReference wikiReference)
            throws NotificationException
    {
        List<NotificationPreference> results = getNotificationPreferences(
                new DocumentReference(GLOBAL_PREFERENCES, wikiReference), WIKI_PROVIDER);
        // Inherit preferences from the main wiki
        if (!wikiReference.getName().equals(wikiDescriptorManager.getMainWikiId())) {
            results.addAll(getNotificationsPreferences(new WikiReference(wikiDescriptorManager.getMainWikiId())));
        }

        return results;
    }

    private List<NotificationPreference> getNotificationPreferences(DocumentReference document,
            String providerHint) throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        List<NotificationPreference> preferences = new ArrayList<>();
        // Get a new instance of the thread-unsafe TargetableNotificationPreferenceBuilder
        TargetableNotificationPreferenceBuilder notificationPreferenceBuilder
                = notificationPreferenceBuilderProvider.get();

        try {
            XWikiDocument doc = xwiki.getDocument(document, context);
            List<BaseObject> preferencesObj = doc.getXObjects(NOTIFICATION_PREFERENCE_CLASS);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        String objFormat = obj.getStringValue(FORMAT_FIELD);
                        Date objStartDate = obj.getDateValue(START_DATE_FIELD);

                        Map<NotificationPreferenceProperty, Object> properties =
                                extractNotificationPreferenceProperties(obj);

                        preferences.add(notificationPreferenceBuilder.prepare()
                            .setProperties(properties)
                            .setStartDate((objStartDate != null) ? objStartDate : doc.getCreationDate())
                            .setFormat(StringUtils.isNotBlank(objFormat)
                                ? NotificationFormat.valueOf(objFormat.toUpperCase())
                                : NotificationFormat.ALERT)
                            .setTarget(document)
                            .setProviderHint(providerHint)
                            .setEnabled(obj.getIntValue(NOTIFICATION_ENABLED_FIELD, 0) != 0)
                            .setCategory(NotificationPreferenceCategory.DEFAULT).build());
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences from the document [%s].",
                            document), e);
        }

        return preferences;
    }

    private Map<NotificationPreferenceProperty, Object> extractNotificationPreferenceProperties(BaseObject object)
    {
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();

        String eventType = object.getStringValue(EVENT_TYPE_FIELD);
        if (StringUtils.isNotBlank(eventType)) {
            properties.put(NotificationPreferenceProperty.EVENT_TYPE, eventType);
        }

        return properties;
    }

    @Override
    public void setStartDateForUser(DocumentReference userReference, Date startDate)
            throws NotificationException
    {
        // Here, we used to update the "startingDate" of all the NotificationPreferenceClass objects that the user page
        // has. But this does not affect the preferences coming from other providers! For example, this does not
        // update the notification settings stored in the wiki's administration page. As a result, most users were
        // unable to clean their notifications when some preferences were inherited from the wiki.
        // To fix this problem, we need to get all preferences and to store them in the user profile, duplicating
        // inherited preferences, but with the new starting date.

        List<NotificationPreference> preferences = notificationPreferenceManager.get().getAllPreferences(userReference);

        // Get a new instance of the thread-unsafe TargetableNotificationPreferenceBuilder
        TargetableNotificationPreferenceBuilder notificationPreferenceBuilder
                = notificationPreferenceBuilderProvider.get();

        List<NotificationPreference> toSave = new ArrayList<>(preferences.size());
        for (NotificationPreference preference : preferences) {
            // We do not handle preferences for the emails, because they have their own starting date mechanism
            // We also ignore preferences that are not enabled.
            if (preference.getFormat() == NotificationFormat.EMAIL || !preference.isNotificationEnabled()) {
                continue;
            }

            // Duplicate the preference to be able to change its content and Save it
            toSave.add(notificationPreferenceBuilder.prepare()
                .setCategory(preference.getCategory())
                .setEnabled(preference.isNotificationEnabled())
                .setFormat(NotificationFormat.ALERT)
                .setProperties(preference.getProperties())
                .setProviderHint(preference.getProviderHint())
                .setTarget(userReference)
                // Change the field we are interested in
                .setStartDate(startDate)
                .build());
        }

        // Now that we have a list of updated preferences to save, we need to save them!
        this.saveNotificationsPreferences(userReference, toSave);
    }

    /**
     * Search for the XObject corresponding to the given notification preference in the given document. If no object
     * is found, returns null.
     *
     * @param xWikiDocument the document to search in
     * @param notificationPreference the notification preference
     * @return the corresponding NotificationPreferences XObject
     * @throws NotificationException if the base object could not be found
     * @since 9.7RC1
     */
    private BaseObject findNotificationPreference(XWikiDocument xWikiDocument,
            NotificationPreference notificationPreference) throws NotificationException
    {
        List<BaseObject> objects = xWikiDocument.getXObjects(NOTIFICATION_PREFERENCE_CLASS);

        if (objects != null) {
            for (BaseObject object : objects) {
                if (object != null
                        && notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                        && notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)
                            .equals(object.getStringValue(EVENT_TYPE_FIELD))) {
                    String format = object.getStringValue(FORMAT_FIELD);

                    // Ensure that we have the correct notification format
                    if ((StringUtils.isBlank(format) && notificationPreference.getFormat().equals(
                            NotificationFormat.ALERT))
                            || notificationPreference.getFormat().equals(NotificationFormat.valueOf(
                                    format.toUpperCase()))) {
                        return object;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void saveNotificationsPreferences(DocumentReference targetDocument,
            List<NotificationPreference> notificationPreferences) throws NotificationException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument document =  xwiki.getDocument(targetDocument, context);

            // Avoid messing with the cache before the document is saved
            document = document.clone();

            for (NotificationPreference notificationPreference : notificationPreferences) {

                // Ensure that the notification preference has an event type to save
                if (!notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)) {
                    continue;
                }

                // Try to find the corresponding XObject for the notification preference
                BaseObject preferenceObject = this.findNotificationPreference(document, notificationPreference);

                // If no preference exist, then create one
                if (preferenceObject == null) {
                    preferenceObject = new BaseObject();
                    preferenceObject.setXClassReference(NOTIFICATION_PREFERENCE_CLASS);
                    document.addXObject(preferenceObject);
                }

                preferenceObject.set(EVENT_TYPE_FIELD,
                        notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE), context);
                preferenceObject.set(FORMAT_FIELD, notificationPreference.getFormat().name().toLowerCase(),
                        context);
                preferenceObject.set(NOTIFICATION_ENABLED_FIELD,
                        (notificationPreference.isNotificationEnabled() ? 1 : 0), context);

                Date startDate = null;
                if (notificationPreference.isNotificationEnabled()) {
                    startDate = notificationPreference.getStartDate();
                    if (startDate == null) {
                        // Fallback to the previous value if date is empty
                        startDate = preferenceObject.getDateValue(START_DATE_FIELD);
                        if (startDate == null) {
                            // Fallback to now
                            startDate = new Date();
                        }
                    }
                }
                preferenceObject.set(START_DATE_FIELD, startDate, context);
            }

            // Make this change a minor edit so it's not displayed, by default, in notifications
            xwiki.saveDocument(document, "Update notification preferences.", true, context);

        } catch (XWikiException e) {
            throw new NotificationException(
                String.format("Failed to save the notification preference into [%s]", targetDocument), e);
        }
    }

    @Override
    public String getEventGroupingStrategyHint(DocumentReference userDocReference, String target)
        throws NotificationException
    {
        String result = null;
        XWikiContext context = contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(userDocReference, context);
            result = getEventGroupingStrategyHint(document, target);
            if (result == null) {
                DocumentReference xwikiPrefReference =
                    new DocumentReference(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE,
                        userDocReference.getWikiReference());
                document = context.getWiki().getDocument(xwikiPrefReference, context);
                result = getEventGroupingStrategyHint(document, target);
            }
        } catch (XWikiException e) {
            throw new NotificationException(
                String.format("Error when trying to load the event grouping strategy preference for "
                + "user [%s]", userDocReference), e);
        }
        if (result == null) {
            result = "default";
        }
        return result;
    }

    private String getEventGroupingStrategyHint(XWikiDocument document, String target)
    {
        List<BaseObject> objects =
            document.getXObjects(NotificationEventGroupingStrategyPreferenceDocumentInitializer.REFERENCE);
        for (BaseObject object : objects) {
            if (object != null && StringUtils.equalsIgnoreCase(target,
                object.getStringValue(NotificationEventGroupingStrategyPreferenceDocumentInitializer.FIELD_TARGET)))
            {
                return object.getStringValue(
                    NotificationEventGroupingStrategyPreferenceDocumentInitializer.FIELD_STRATEGY);
            }
        }
        return null;
    }
}
