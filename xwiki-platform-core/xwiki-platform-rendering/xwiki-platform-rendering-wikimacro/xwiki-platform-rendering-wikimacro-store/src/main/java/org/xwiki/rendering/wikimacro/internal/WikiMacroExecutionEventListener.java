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
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

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
     * Used to extract the {@link XWikiContext}.
     */
    @Inject
    private Execution execution;

    /**
     * Used to get wiki macro document and context document.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

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
        ExecutionContext context = this.execution.getContext();
        XWikiContext xwikiContext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        XWikiDocument contextDoc = xwikiContext.getDoc();

        // Set context document content author as macro author so that programming right is tested on the right
        // user
        XWikiDocument wikiMacroDocument;
        try {
            wikiMacroDocument = (XWikiDocument) this.documentAccessBridge.getDocument(wikiMacro.getDocumentReference());

            // Set context document content author as macro author so that programming right is tested on the right
            // user. It's cloned to make sure it not really modifying the real document but only do that for the
            // current context.
            XWikiDocument contextDocClone = contextDoc.clone();
            contextDocClone.setContentAuthorReference(wikiMacroDocument.getContentAuthorReference()); 
            contextDocClone.setMetaDataDirty(contextDoc.isMetaDataDirty());
            xwikiContext.setDoc(contextDocClone);
        } catch (Exception e) {
            Log.error("Failed to setup context before wiki macro execution");
        }
    }

    /**
     * Called when receiving a {@link WikiMacroExecutionFinishedEvent} event.
     */
    public void onWikiMacroExecutionFinishedEvent()
    {
        ExecutionContext context = this.execution.getContext();
        XWikiContext xwikiContext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        XWikiDocument contextDoc = xwikiContext.getDoc();

        // Restore context document
        try {
            contextDoc = (XWikiDocument) this.documentAccessBridge.getDocument(contextDoc.getDocumentReference());

            xwikiContext.setDoc(contextDoc);
        } catch (Exception e) {
            Log.error("Failed to setup context after wiki macro execution");
        }

    }
}
