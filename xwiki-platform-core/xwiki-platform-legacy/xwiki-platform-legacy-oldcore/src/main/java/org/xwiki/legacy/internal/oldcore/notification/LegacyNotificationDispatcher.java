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
package org.xwiki.legacy.internal.oldcore.notification;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;

/**
 * An event listener that forwards received events to their corresponding legacy events. This allows deprecated events
 * to continue to be supported.
 * 
 * @version $Id$
 * @deprecated since old events shouldn't be used anymore (they're deprecated themselves)
 */
@Component
@Named("LegacyNotificationDispatcher")
@Singleton
@Deprecated
public class LegacyNotificationDispatcher implements EventListener
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "LegacyNotificationDispatcher";
    }

    @Override
    public List<Event> getEvents()
    {
        return new ArrayList<Event>()
        {
            {
                add(new DocumentDeletedEvent());
                add(new DocumentCreatedEvent());
                add(new DocumentUpdatedEvent());
                add(new DocumentDeletingEvent());
                add(new DocumentCreatingEvent());
                add(new DocumentUpdatingEvent());
                add(new ActionExecutedEvent());
                add(new ActionExecutingEvent());
            }
        };
    }

    private XWikiNotificationManager getNotificationManager(XWikiContext context)
    {
        try {
            return (XWikiNotificationManager) XWiki.class.getMethod("getNotificationManager").invoke(context.getWiki());
        } catch (Exception e) {
            this.logger.error("Failed to get [XWikiNotificationManager]", e);
        }

        return null;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiDocument originalDocument = document.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;
        
        XWikiNotificationManager manager = getNotificationManager((XWikiContext) data);

        if (manager != null) {
            if (event instanceof DocumentCreatedEvent) {
                manager.verify(document, originalDocument, XWikiDocChangeNotificationInterface.EVENT_NEW, context);
            } else if (event instanceof DocumentUpdatedEvent) {
                manager.verify(document, originalDocument, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
            } else if (event instanceof DocumentCreatingEvent) {
                manager.preverify(document, originalDocument, XWikiDocChangeNotificationInterface.EVENT_NEW, context);
            } else if (event instanceof DocumentUpdatingEvent) {
                manager
                    .preverify(document, originalDocument, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
            } else if (event instanceof DocumentDeletedEvent) {
                manager.verify(new XWikiDocument(document.getDocumentReference()), document,
                    XWikiDocChangeNotificationInterface.EVENT_DELETE, context);
            } else if (event instanceof DocumentDeletingEvent) {
                manager.preverify(document, new XWikiDocument(document.getDocumentReference()),
                    XWikiDocChangeNotificationInterface.EVENT_DELETE, context);
            } else if (event instanceof ActionExecutedEvent) {
                manager.verify(document, ((ActionExecutedEvent) event).getActionName(), context);
            } else if (event instanceof ActionExecutingEvent) {
                manager.preverify(document, ((ActionExecutingEvent) event).getActionName(), context);
            }
        } else {
            this.logger.error("Can't find old [XWikiNotificationManager] system");
        }
    }
}
