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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Cleanup the notifications when a wiki is deleted.
 *
 * @version $Id$
 * @since 14.5RC1
 * @since 14.4.1
 * @since 13.10.7
 */
@Component
@Singleton
@Named("org.xwiki.notifications.filters.internal.DeletedWikiEventListener")
public class DeletedWikiEventListener extends AbstractEventListener
{
    @Named("cached")
    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DeletedWikiEventListener()
    {
        super(DeletedWikiEventListener.class.getName(), new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        String wikiId = (String) source;
        try {
            this.modelBridge.deleteFilterPreferences(new WikiReference(wikiId));
        } catch (NotificationException e) {
            this.logger.warn("Failed to delete notification preferences for wiki [{}]. Cause: [{}].", wikiId,
                getRootCauseMessage(e));
        }
    }
}
