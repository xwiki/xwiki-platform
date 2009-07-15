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

package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.AbstractDocumentEvent;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilder;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilderException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;

/**
 * An {@link EventListener} responsible for dynamically registering / unregistering / updating xwiki rendering macros
 * based on wiki macro create / delete / update actions.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component("wikimacrolistener")
public class WikiMacroEventListener extends AbstractLogEnabled implements EventListener
{
    /**
     * The {@link DocumentAccessBridge} component.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * The {@link WikiMacroBuilder} component.
     */
    @Requirement
    private WikiMacroBuilder macroBuilder;

    /**
     * The {@link WikiMacroManager} component.
     */
    @Requirement
    private WikiMacroManager wikiMacroManager;

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return "wikimacrolistener";
    }

    /**
     * {@inheritDoc}
     */
    public List<Event> getEvents()
    {
        List<Event> events = new ArrayList<Event>();
        events.add(new DocumentSaveEvent());
        events.add(new DocumentUpdateEvent());
        events.add(new DocumentDeleteEvent());
        return events;
    }

    /**
     * {@inheritDoc}
     */
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof AbstractDocumentEvent) {
            // TODO: This approach towards extracting the document name doesn't look right. But we cannot use
            // 'source' parameter without depending on xwiki-core. This must be fixed.
            String documentName = ((AbstractDocumentEvent) event).getEventFilter().getFilter();
            
            // TODO: This needs to be discussed.
            if (!documentName.startsWith("xwiki:")) {
                getLogger().error("Wiki macro registration from virtual wikis are not allowed");
                return;
            }

            if (event instanceof DocumentSaveEvent || event instanceof DocumentUpdateEvent) {
                // Unregister any existing macro registered under this document.
                if (wikiMacroManager.hasWikiMacro(documentName)) {
                    wikiMacroManager.unregisterWikiMacro(documentName);
                }

                // Check whether the given document has a wiki macro defined in it.
                String macroName =
                    (String) docBridge.getProperty(documentName, WikiMacroBuilder.WIKI_MACRO_CLASS, 0,
                        WikiMacroBuilder.MACRO_NAME_PROPERTY);

                if (null != macroName) {
                    // Attempt to build a wiki macro.
                    WikiMacro wikiMacro = null;
                    try {
                        wikiMacro = macroBuilder.buildMacro(documentName);
                    } catch (WikiMacroBuilderException ex) {
                        getLogger().error(ex.getMessage());
                        return;
                    }

                    // Check if the user has programming rights before continuing further.
                    if (!docBridge.hasProgrammingRights()) {
                        String errorMessage = "Unable to register macro [%s] due to insufficient privileges";
                        getLogger().error(String.format(errorMessage, wikiMacro.getName()));
                        return;
                    }

                    // Register macro.
                    wikiMacroManager.registerWikiMacro(documentName, wikiMacro);
                }
            } else if (event instanceof DocumentDeleteEvent && wikiMacroManager.hasWikiMacro(documentName)) {
                wikiMacroManager.unregisterWikiMacro(documentName);
            }
        }
    }
}
