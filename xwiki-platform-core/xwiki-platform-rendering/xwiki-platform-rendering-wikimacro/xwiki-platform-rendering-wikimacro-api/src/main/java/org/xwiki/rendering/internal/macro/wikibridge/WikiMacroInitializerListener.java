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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Called at startup to initialize existing wiki macros.
 * 
 * @version $Id$
 * @since 4.2M1
 */
@Component
@Singleton
@Named(WikiMacroInitializerListener.NAME)
public class WikiMacroInitializerListener implements EventListener
{
    static final String NAME = "WikiMacroInitializerListener";

    /**
     * The events observed by this event listener.
     */
    private static final List<Event> EVENTS = Arrays.asList(new ApplicationReadyEvent(), new WikiReadyEvent());

    /**
     * The macro initializer used to register the wiki macros.
     */
    @Inject
    private Provider<WikiMacroInitializer> macroInitializer;

    @Inject
    private WikiDescriptorManager wikiManager;

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
        WikiMacroInitializer initializer = this.macroInitializer.get();

        if (event instanceof ApplicationReadyEvent) {
            try {
                initializer.registerExistingWikiMacros(wikiManager.getCurrentWikiId());
            } catch (Exception e) {
                this.logger.error("Error while registering wiki macros.", e);
            }
        } else if (event instanceof WikiReadyEvent) {
            try {
                initializer.registerExistingWikiMacros(((WikiReadyEvent) event).getWikiId());
            } catch (Exception e) {
                this.logger.error("Error while initializing wiki macro classes.", e);
            }
        }
    }
}
