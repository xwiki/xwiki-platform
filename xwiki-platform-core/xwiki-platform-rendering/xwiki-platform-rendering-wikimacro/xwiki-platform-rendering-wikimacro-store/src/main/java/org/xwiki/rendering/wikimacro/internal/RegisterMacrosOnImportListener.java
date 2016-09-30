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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.event.XARImportedEvent;

/**
 * Import action event listener to handle registering of the wiki macros after import. The problem is with the wiki
 * macros imported in the empty wiki, which cannot be registered at the save of the document on import time due to lack
 * of rights (initial import is done with XWikiGuest). <br>
 * FIXME: remove this when the initial import will be done with the appropriate user
 * 
 * @version $Id$
 */
@Component
@Singleton
@Named("register-macros-on-import")
public class RegisterMacrosOnImportListener implements EventListener
{
    /**
     * The macro initializer used to register the wiki macros.
     */
    @Inject
    private WikiMacroInitializer macroInitializer;

    /**
     * The execution used to get the xwiki context, to reset the context grouplist cache.
     */
    @Inject
    private Execution execution;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The events observed by this event listener.
     */
    private final List<Event> eventsList = new ArrayList<Event>(Arrays.asList(new XARImportedEvent()));

    @Override
    public List<Event> getEvents()
    {
        return eventsList;
    }

    @Override
    public String getName()
    {
        return "RegisterMacrosOnImportListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // when import is done, re-register macros in the current wiki
        try {
            // clean the grouplist cache from the context since now, after import, the groups have changed. All
            // cache saved on the context during the import (save listeners) should be cleaned since it might not be
            // accurate.
            // FIXME: somehow, it's a bit of time loss to clean the context grouplist, especially when this is done
            // for a subsequent import (not the initial import of the .xar), when macros are registered anyway, there
            // are no rights issues on registering macros on documents save.
            XWikiContext xcontext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
            xcontext.remove("grouplist");
            // get the current wiki to register macros for
            String currentWiki = xcontext.getWikiId();
            macroInitializer.registerExistingWikiMacros(currentWiki);
        } catch (Exception e) {
            this.logger.warn("Could not register existing macros on import", e);
        }
    }
}
