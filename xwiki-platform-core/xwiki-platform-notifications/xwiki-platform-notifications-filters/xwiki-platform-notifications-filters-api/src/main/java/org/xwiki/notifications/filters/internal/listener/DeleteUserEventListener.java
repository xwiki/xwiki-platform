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
package org.xwiki.notifications.filters.internal.listener;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Delete the user preferences of the deleted users.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@Component
@Singleton
@Named("org.xwiki.notifications.filters.internal.listener.DeleteUserEventListener")
public class DeleteUserEventListener extends AbstractEventListener
{
    /**
     * We use a provider to allow the api to be loaded without an implementation, which is useful on some test context
     * where the listeners are not used.
     */
    @Named("cached")
    @Inject
    private Provider<FilterPreferencesModelBridge> modelBridgeProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DeleteUserEventListener()
    {
        super(DeleteUserEventListener.class.getName(),
            new XObjectDeletedEvent(BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference user = ((XWikiDocument) source).getDocumentReference();
        try {
            this.modelBridgeProvider.get().deleteFilterPreferences(user);
        } catch (NotificationException e) {
            this.logger.warn("Failed to delete notification preferences for user [{}]. Cause: [{}].", user,
                getRootCauseMessage(e));
        }
    }
}
