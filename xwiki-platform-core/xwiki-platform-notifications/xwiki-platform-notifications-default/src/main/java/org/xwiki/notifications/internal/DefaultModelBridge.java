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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;

import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link ModelBridge}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    private static final List<String> NOTIFICATION_CODE_SPACE =
            Arrays.asList(XWiki.SYSTEM_SPACE, "Notifications", "Code");

    private static final LocalDocumentReference NOTIFICATION_PREFERENCE_CLASS =
            new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "NotificationPreferenceClass");

    private static final LocalDocumentReference NOTIFICATION_PREFERENCE_SCOPE_CLASS =
            new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "NotificationPreferenceScopeClass");

    private static final LocalDocumentReference NOTIFICATION_START_DATE_CLASS =
            new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "NotificationsStartDateClass");

    private static final LocalDocumentReference PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS =
            new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "PageNotificationEventDescriptorClass");

    private static final String APPLICATION_NAME = "applicationName";

    private static final String EVENT_TYPE = "eventType";

    private static final String EVENT_PRETTY_NAME = "eventPrettyName";

    private static final String EVENT_ICON = "eventIcon";

    private static final String LISTEN_TO = "listenTo";

    private static final String OBJECT_TYPE = "objectType";

    private static final String VALIDATION_EXPRESSION = "validationExpression";

    private static final String NOTIFICATION_TEMPLATE = "notificationTemplate";

    private static final String START_DATE = "startDate";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        List<NotificationPreference> preferences = new ArrayList<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            List<BaseObject> preferencesObj = doc.getXObjects(NOTIFICATION_PREFERENCE_CLASS);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        preferences.add(new NotificationPreference(
                                obj.getStringValue(EVENT_TYPE),
                                obj.getStringValue("applicationId"),
                                obj.getIntValue("notificationEnabled", 0) == 1
                        ));
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences for the user [%s].", userReference), e);
        }

        return preferences;
    }

    @Override
    public Date getUserStartDate(DocumentReference userReference) throws NotificationException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument document =  xwiki.getDocument(userReference, context);

            BaseObject obj = document.getXObject(NOTIFICATION_START_DATE_CLASS);
            if (obj != null) {
                Date date = obj.getDateValue(START_DATE);
                if (date != null) {
                    return date;
                }
            }

            // Fallback to the creation date of the user
            return document.getCreationDate();
        } catch (XWikiException e) {
            throw new NotificationException(
                    String.format("Failed to get the document [%s].", userReference), e);
        }
    }

    @Override
    public void setStartDateForUser(DocumentReference userReference, Date startDate)
            throws NotificationException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument document =  xwiki.getDocument(userReference, context);

            BaseObject obj = document.getXObject(NOTIFICATION_START_DATE_CLASS, true, context);
            obj.setDateValue(START_DATE, startDate);

            xwiki.saveDocument(document, "Update start date for the notifications.", context);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to set the user start date for [%s].", userReference), e);
        }
    }

    @Override
    public PageNotificationEventDescriptor getPageNotificationEventDescriptor(
            DocumentReference documentReference) throws NotificationException

    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        PageNotificationEventDescriptor newDescriptor = null;

        try {
            XWikiDocument doc = xwiki.getDocument(PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS, context);
            BaseObject eventDescriptorObj = doc.getXObject(PAGE_NOTIFICATION_EVENT_DESCRIPTOR_CLASS);
            if (eventDescriptorObj != null) {
                newDescriptor = new PageNotificationEventDescriptor(ImmutableMap.<String, String>builder()
                        .put(APPLICATION_NAME, eventDescriptorObj.getStringValue(APPLICATION_NAME))
                        .put(EVENT_TYPE, eventDescriptorObj.getStringValue(EVENT_TYPE))
                        .put(EVENT_PRETTY_NAME, eventDescriptorObj.getStringValue(EVENT_PRETTY_NAME))
                        .put(EVENT_ICON, eventDescriptorObj.getStringValue(EVENT_ICON))
                        .put(OBJECT_TYPE, eventDescriptorObj.getStringValue(OBJECT_TYPE))
                        .put(VALIDATION_EXPRESSION, eventDescriptorObj.getStringValue(VALIDATION_EXPRESSION))
                        .put(NOTIFICATION_TEMPLATE, eventDescriptorObj.getStringValue(NOTIFICATION_TEMPLATE))
                        .build(),
                        eventDescriptorObj.getListValue(LISTEN_TO),
                        doc.getAuthorReference());
            }

            return newDescriptor;
        } catch (XWikiException e) {
            throw new NotificationException(
                    String.format("Failed to get the event descriptors for the user [%s].", documentReference), e);
        }
    }

    @Override
    public List<NotificationPreferenceScope> getNotificationPreferenceScopes(DocumentReference userReference)
        throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        List<NotificationPreferenceScope> preferences = new ArrayList<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            List<BaseObject> preferencesObj = doc.getXObjects(NOTIFICATION_PREFERENCE_SCOPE_CLASS);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        String scopeType = obj.getStringValue("scope");
                        EntityType type;
                        if (scopeType.equals("pageOnly")) {
                            type = EntityType.DOCUMENT;
                        } else if (scopeType.equals("pageAndChildren")) {
                            type = EntityType.SPACE;
                        } else if (scopeType.equals("wiki")) {
                            type = EntityType.WIKI;
                        } else {
                            logger.warn("Scope [{}] is not supported as a NotificationPreferenceScope (user [{}]).",
                                    scopeType, userReference);
                            continue;
                        }

                        preferences.add(new NotificationPreferenceScope(
                                obj.getStringValue(EVENT_TYPE),
                                entityReferenceResolver.resolve(obj.getStringValue("scopeReference"), type)
                        ));
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences scope for the user [%s].", userReference),
                    e);
        }

        return preferences;
    }
}
