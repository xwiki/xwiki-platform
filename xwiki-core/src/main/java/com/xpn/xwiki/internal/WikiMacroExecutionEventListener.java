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
package com.xpn.xwiki.internal;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionFinishedEvent;
import org.xwiki.rendering.macro.wikibridge.WikiMacroExecutionStartsEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Make sure to execute wiki macro with a properly configured context and especially which user programming right is
 * tested on.
 * 
 * @version $Id$
 */
@Component("WikiMacroExecutionEventListener")
public class WikiMacroExecutionEventListener extends AbstractLogEnabled implements EventListener
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
    @Requirement
    private Execution execution;

    /**
     * Used to get wiki macro document and context document.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Temporarily used to resolve the user as a valid document reference.
     */
    @Requirement
    private DocumentReferenceResolver<String> resolver;

    /**
     * Temporarily used to serialize a document reference pointing to a user as a String.
     */
    @Requirement
    private EntityReferenceSerializer<String> serializer;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext xwikiContext = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument contextDoc = xwikiContext.getDoc();

        if (event instanceof WikiMacroExecutionStartsEvent) {
            // Set context document content author as macro author so that programming right is tested on the right
            // user
            WikiMacro wikiMacro = (WikiMacro) source;
            XWikiDocument wikiMacroDocument;
            try {
                wikiMacroDocument =
                    (XWikiDocument) this.documentAccessBridge.getDocument(wikiMacro.getDocumentReference());

                // Set context document content author as macro author so that programming right is tested on the right
                // user. It's cloned to make sure it not really modifying the real document but only do that for the
                // current context.
                contextDoc = contextDoc.clone();
                contextDoc.setContentAuthor(this.serializer.serialize(this.resolver.resolve(
                    wikiMacroDocument.getContentAuthor(), wikiMacroDocument.getDocumentReference())));

                xwikiContext.setDoc(contextDoc);
            } catch (Exception e) {
                Log.error("Failed to setup context before wiki macro execution");
            }
        } else {
            // Restore context document
            try {
                contextDoc = (XWikiDocument) this.documentAccessBridge.getDocument(contextDoc.getDocumentReference());

                xwikiContext.setDoc(contextDoc);
            } catch (Exception e) {
                Log.error("Failed to setup context after wiki macro execution");
            }
        }
    }
}
