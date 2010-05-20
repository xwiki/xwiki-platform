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

import java.util.Arrays;
import java.util.List;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
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
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
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
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroFactory} component.
     */
    @Requirement
    private WikiMacroFactory macroFactory;

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
        return Arrays.<Event>asList(
            new DocumentSaveEvent(),
            new DocumentUpdateEvent(),
            new DocumentDeleteEvent());
    }

    /**
     * {@inheritDoc}
     */
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof AbstractDocumentEvent) {
            DocumentModelBridge document = (DocumentModelBridge) source;
            DocumentReference documentReference = document.getDocumentReference();

            // We've decided not to log any exception raised in the XWiki logs since a failure to register or
            // unregister a macro isn't a failure of the XWiki software. It's something normal, same as, for example,
            // not being able to edit a page if the user doesn't have edit rights.
            // The problem here is that since Macros are registered by an Event Listener there's no way defined
            // to let the user know about the status. We'd need to:
            // - create a status page listing the state of all macros with the last error messages
            // - don't use a listener to register macros and instead have a page listing all macros with a
            //   register/unregister button (and thus provide visual feedback when the action fails).

            if (event instanceof DocumentSaveEvent || event instanceof DocumentUpdateEvent) {
                // Unregister any existing macro registered under this document.
                if (unregisterMacroInternal(documentReference)) {

                    // Check whether the given document has a wiki macro defined in it.
                    if (macroFactory.containsWikiMacro(documentReference)) {

                        // Attempt to create a wiki macro.
                        WikiMacro wikiMacro;
                        try {
                            wikiMacro = macroFactory.createWikiMacro(documentReference);
                        } catch (WikiMacroException e) {
                            getLogger().debug(String.format("Failed to create wiki macro [%s]", documentReference), e);
                            return;
                        }

                        // Register the macro.
                        registerMacroInternal(documentReference, wikiMacro);
                    }
                }
            } else if (event instanceof DocumentDeleteEvent) {
                unregisterMacroInternal(documentReference);
            }
        }
    }

    /**
     * @since 2.2M1
     */
    private void registerMacroInternal(DocumentReference documentReference, WikiMacro wikiMacro)
    {
        try {
            this.wikiMacroManager.registerWikiMacro(documentReference, wikiMacro);
        } catch (WikiMacroException e) {
            getLogger().debug(String.format("Unable to register macro [%s] in document [%s]",
                wikiMacro.getDescriptor().getId().getId(), documentReference), e);
        }
    }

    /**
     * @since 2.2M1
     */
    private boolean unregisterMacroInternal(DocumentReference documentReference)
    {
        boolean result = true;
        if (this.wikiMacroManager.hasWikiMacro(documentReference)) {
            try {
                this.wikiMacroManager.unregisterWikiMacro(documentReference);
            } catch (WikiMacroException e) {
                getLogger().debug(String.format("Unable to unregister macro in document [%s]", documentReference), e);
                result = false;
            }
        }
        return result;
    }
}
