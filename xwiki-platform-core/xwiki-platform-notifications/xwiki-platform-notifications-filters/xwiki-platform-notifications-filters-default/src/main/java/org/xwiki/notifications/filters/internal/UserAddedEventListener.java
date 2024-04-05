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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Automatically copy the wiki filter preferences to newly created users.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component
@Singleton
@Named(UserAddedEventListener.NAME)
public class UserAddedEventListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "org.xwiki.notifications.filters.internal.UserAddedEventListener";

    private static final List<String> NOTIFICATION_CODE_SPACE = Arrays.asList("XWiki", "Notifications", "Code");
    protected static final LocalDocumentReference NOTIFICATION_CONFIGURATION =
        new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "NotificationAdministration");
    protected static final LocalDocumentReference TOGGLEABLE_FILTER_PREFERENCE_CLASS =
        new LocalDocumentReference(NOTIFICATION_CODE_SPACE, "ToggleableFilterPreferenceClass");

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public UserAddedEventListener()
    {
        // We're listening on a DocumentCreatingEvent because we cannot listen properly on an XObjectAddedEvent
        // without avoiding some recursivity issues. This should be improved with a XObjectAddingEvent or even better
        // with a UserCreatingEvent.
        super(NAME, new DocumentCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument userDoc = (XWikiDocument) source;
        if (userDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE) != null) {
            DocumentReference userDocReference = userDoc.getDocumentReference();
            try {
                this.copyFilterPreferences(userDocReference);
                this.copyToggleableFilterPreferences(userDoc);
            } catch (Exception e) {
                logger.error("Error while trying to copy filter preferences to new user", e);
            }
        }
    }

    private void copyFilterPreferences(DocumentReference userDocReference) throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences = this.notificationFilterPreferenceManager
            .getFilterPreferences(userDocReference.getWikiReference());
        Set<NotificationFilterPreference> userFilterPreferences = new HashSet<>();

        for (NotificationFilterPreference filterPreference : filterPreferences) {
            DefaultNotificationFilterPreference preference =
                new DefaultNotificationFilterPreference(filterPreference, false);
            preference.setProviderHint(UserProfileNotificationFilterPreferenceProvider.HINT);
            userFilterPreferences.add(preference);
        }
        this.notificationFilterPreferenceManager.saveFilterPreferences(userDocReference, userFilterPreferences);
    }

    private void copyToggleableFilterPreferences(XWikiDocument userDocument) throws Exception
    {
        XWikiContext xWikiContext = this.contextProvider.get();
        WikiReference currentWikiReference = xWikiContext.getWikiReference();
        try {
            xWikiContext.setWikiReference(userDocument.getDocumentReference().getWikiReference());

            // We check if the userDocument contains the toggleable filter preferences xobjects before adding them
            // to avoid a recursive loop when calling the save.
            if (userDocument.getXObjects(TOGGLEABLE_FILTER_PREFERENCE_CLASS).isEmpty()) {
                XWikiDocument notificationConfiguration =
                    xWikiContext.getWiki().getDocument(NOTIFICATION_CONFIGURATION, xWikiContext);
                List<BaseObject> xObjects = notificationConfiguration.getXObjects(TOGGLEABLE_FILTER_PREFERENCE_CLASS);

                for (BaseObject xObject : xObjects) {
                    if (xObject != null) {
                        userDocument.addXObject(xObject.duplicate());
                    }
                }
            }
        } finally {
            xWikiContext.setWikiReference(currentWikiReference);
        }
    }
}
