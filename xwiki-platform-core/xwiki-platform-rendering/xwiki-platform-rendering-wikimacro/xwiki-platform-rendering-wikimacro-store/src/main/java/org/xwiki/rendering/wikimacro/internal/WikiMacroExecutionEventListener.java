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
package org.xwiki.rendering.wikimacro.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;
import org.xwiki.security.authorization.ContentDocumentController;

/**
 * Make sure to execute wiki macro with a properly configured context and especially which user programming right is
 * tested on.
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("WikiMacroExecutionEventListener")
public class WikiMacroExecutionEventListener implements EventListener
{
    /**
     * The name of the listener.
     */
    private static final String NAME = "WikiMacroExecutionEventListener";

    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new WikiMacroExecutionStartsEvent());
            add(new WikiMacroExecutionFinishedEvent());
        }
    };

    /**
     * Used to get wiki macro document and context document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The content author controller.
     */
    @Inject
    private ContentDocumentController contentDocumentController;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;


    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiMacroExecutionStartsEvent) {
            onWikiMacroExecutionStartsEvent((WikiMacro) source);
        } else {
            onWikiMacroExecutionFinishedEvent();
        }
    }

    /**
     * Called when receiving a {@link WikiMacroExecutionStartsEvent} event.
     * 
     * @param wikiMacro the wiki macro sending the event
     */
    public void onWikiMacroExecutionStartsEvent(WikiMacro wikiMacro)
    {
        try {
            DocumentModelBridge wikiMacroDocument
                = this.documentAccessBridge.getDocument(wikiMacro.getDocumentReference());
            contentDocumentController.pushContentDocument(wikiMacroDocument);
        } catch (Exception e) {
            logger.error("Failed to set the wiki macro document as content document before macro exeuction.");
            contentDocumentController.pushContentDocument(null);
        }
    }

    /**
     * Called when receiving a {@link WikiMacroExecutionFinishedEvent} event.
     */
    public void onWikiMacroExecutionFinishedEvent()
    {
        contentDocumentController.popContentDocument();
    }
}
