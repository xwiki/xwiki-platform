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
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
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
     * The {@link DocumentNameSerializer} component.
     */
    @Requirement
    private DocumentNameSerializer docNameSerializer;

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
            DocumentModelBridge document = (DocumentModelBridge) source;
            DocumentName documentName = docBridge.getDocumentName(document.getFullName());
            String fullDocumentName = docNameSerializer.serialize(documentName);

            if (event instanceof DocumentSaveEvent || event instanceof DocumentUpdateEvent) {
                // Unregister any existing macro registered under this document.
                if (wikiMacroManager.hasWikiMacro(fullDocumentName)) {
                    wikiMacroManager.unregisterWikiMacro(fullDocumentName);
                }

                // Check whether the given document has a wiki macro defined in it.
                if (macroBuilder.containsMacro(fullDocumentName)) {
                    // Make sure the wiki macro is defined on the main wiki.
                    if (!fullDocumentName.startsWith("xwiki:")) {
                        getLogger().error("Wiki macro registration from virtual wikis are not allowed");
                        return;
                    }

                    // Attempt to build a wiki macro.
                    WikiMacro wikiMacro = null;
                    try {
                        wikiMacro = macroBuilder.buildMacro(fullDocumentName);
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
                    wikiMacroManager.registerWikiMacro(fullDocumentName, wikiMacro);
                }
            } else if (event instanceof DocumentDeleteEvent && wikiMacroManager.hasWikiMacro(fullDocumentName)) {
                wikiMacroManager.unregisterWikiMacro(fullDocumentName);
            }
        }
    }
}
