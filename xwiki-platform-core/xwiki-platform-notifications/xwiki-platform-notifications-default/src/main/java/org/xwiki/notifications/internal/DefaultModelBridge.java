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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesClass = new DocumentReference("NotificationPreferenceClass",
                new SpaceReference("Code", new SpaceReference("Notifications",
                        new SpaceReference("XWiki", userReference.getWikiReference()))));

        List<NotificationPreference> preferences = new ArrayList<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            for (BaseObject obj : doc.getXObjects(notificationPreferencesClass)) {
                if (obj != null) {
                    preferences.add(new NotificationPreference(
                            obj.getStringValue("eventType"),
                            obj.getStringValue("applicationId"),
                            obj.getIntValue("notificationEnabled", 0) == 1
                    ));
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences for the user [%s].", userReference), e);
        }

        return preferences;
    }
}
