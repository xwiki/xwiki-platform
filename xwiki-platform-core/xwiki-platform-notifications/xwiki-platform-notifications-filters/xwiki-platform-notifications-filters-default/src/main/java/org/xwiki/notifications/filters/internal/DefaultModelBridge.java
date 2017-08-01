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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.text.StringUtils;

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
public class DefaultModelBridge implements ModelBridge
{
    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
            new SpaceReference("Notifications",
                    new SpaceReference("XWiki", new WikiReference("xwiki"))
            )
    );

    private static final DocumentReference NOTIFICATION_PREFERENCE_SCOPE_CLASS = new DocumentReference(
            "NotificationPreferenceScopeClass", NOTIFICATION_CODE_SPACE
    );

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<NotificationPreferenceFilterScope> getNotificationPreferenceScopes(DocumentReference userReference,
            NotificationFormat format) throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesScopeClass
                = NOTIFICATION_PREFERENCE_SCOPE_CLASS.setWikiReference(userReference.getWikiReference());

        List<NotificationPreferenceFilterScope> preferences = new ArrayList<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            List<BaseObject> preferencesObj = doc.getXObjects(notificationPreferencesScopeClass);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null && isCompatibleFormat(obj.getStringValue("format"), format)) {
                        String scopeType = obj.getStringValue("scope");
                        NotificationPreferenceScopeFilterType scopeFilterType = this.extractScopeFilterType(obj);
                        EntityType type;
                        if (scopeType.equals("pageOnly")) {
                            type = EntityType.DOCUMENT;
                        } else if (scopeType.equals("pageAndChildren")) {
                            type = EntityType.SPACE;
                        } else if (scopeType.equals("wiki")) {
                            type = EntityType.WIKI;
                        } else {
                            logger.warn(
                                    "Scope [{}] is not supported as a NotificationPreferenceFilterScope (user [{}]).",
                                    scopeType, userReference);
                            continue;
                        }

                        preferences.add(new NotificationPreferenceFilterScope(
                                obj.getStringValue("eventType"),
                                entityReferenceResolver.resolve(obj.getStringValue("scopeReference"), type),
                                scopeFilterType
                        ));
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences scope for the user [%s].",
                            userReference), e);
        }

        return preferences;
    }

    /**
     * Extract the scopeFilterType parameter in the given {@link BaseObject}.
     * This is done in order to eliminate too much cyclomatic complexity in
     * {@link #getNotificationPreferenceScopes(DocumentReference, NotificationFormat)}.
     * If no scopeFilterType is defined, the default is {@link NotificationPreferenceScopeFilterType#INCLUSIVE}.
     *
     * @param object the related base object
     * @return the corresponding {@link NotificationPreferenceScopeFilterType}
     * @since 9.7RC1
     */
    private NotificationPreferenceScopeFilterType extractScopeFilterType(BaseObject object)
    {
        String rawScopeFilterType = object.getStringValue("scopeFilterType");
        return (rawScopeFilterType != null && StringUtils.isNotBlank(rawScopeFilterType))
                ? NotificationPreferenceScopeFilterType.valueOf(rawScopeFilterType.toUpperCase())
                : NotificationPreferenceScopeFilterType.INCLUSIVE;
    }

    private boolean isCompatibleFormat(String format, NotificationFormat expectedFormat)
    {
        return format != null && NotificationFormat.valueOf(format.toUpperCase()) == expectedFormat;
    }

    @Override
    public List<NotificationPreferenceFilterScope> getNotificationPreferenceScopes(DocumentReference userReference,
            NotificationFormat format, NotificationPreferenceScopeFilterType scopeFilterType)
            throws NotificationException
    {
        List<NotificationPreferenceFilterScope> preferences = new ArrayList<>();

        for (NotificationPreferenceFilterScope preference
                : this.getNotificationPreferenceScopes(userReference, format)) {
            if (preference.getScopeFilterType().equals(scopeFilterType)) {
                preferences.add(preference);
            }
        }

        return preferences;
    }
}
