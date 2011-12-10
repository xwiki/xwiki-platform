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
package com.xpn.xwiki.plugin.applicationmanager.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManager;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerMessageTool;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerPlugin;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;

/**
 * Listener do to application descriptors modifications.
 * 
 * @version $Id$
 */
@Component
@Named("applicationmanager")
public class ApplicationManagerListener implements EventListener
{
    /**
     * The events matchers.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentUpdatedEvent());
            add(new DocumentCreatedEvent());
        }
    };

    @Inject
    private Logger logger;

    /**
     * Protected API for managing applications.
     */
    private ApplicationManager applicationManager;

    private ApplicationManagerMessageTool messageTool;

    private ApplicationManagerMessageTool getApplicationManagerMessageTool(XWikiContext xcontext)
    {
        if (this.messageTool == null) {
            this.messageTool = ApplicationManagerMessageTool.getDefault(xcontext);
        }

        return this.messageTool;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return ApplicationManagerPlugin.PLUGIN_NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext xcontext = (XWikiContext) data;

        try {
            if (XWikiApplicationClass.isApplication(document)) {
                getApplicationManager(xcontext).updateApplicationsTranslation(document, xcontext);
            }
        } catch (XWikiException e) {
            this.logger.error(
                getApplicationManagerMessageTool(xcontext).get(
                    ApplicationManagerMessageTool.LOG_AUTOUPDATETRANSLATIONS, document.getFullName()), e);
        }
    }

    private ApplicationManager getApplicationManager(XWikiContext xcontext)
    {
        if (this.applicationManager == null) {
            this.applicationManager = new ApplicationManager(getApplicationManagerMessageTool(xcontext));
        }

        return this.applicationManager;
    }
}
