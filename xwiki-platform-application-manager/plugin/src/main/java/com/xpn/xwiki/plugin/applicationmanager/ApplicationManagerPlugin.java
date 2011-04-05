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

package com.xpn.xwiki.plugin.applicationmanager;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.applicationmanager.doc.XWikiApplicationClass;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Entry point of the Application Manager plugin.
 * 
 * @version $Id$
 */
public class ApplicationManagerPlugin extends XWikiDefaultPlugin implements EventListener
{
    /**
     * Identifier of Application Manager plugin.
     */
    public static final String PLUGIN_NAME = "applicationmanager";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The logging tool.
     */
    protected static final Log LOG = LogFactory.getLog(ApplicationManagerPlugin.class);

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

    /**
     * Protected API for managing applications.
     */
    private ApplicationManager applicationManager;

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Construction the entry point of the Application Manager plugin.
     * 
     * @param name the identifier of the plugin.
     * @param className the class name of the entry point of the plugin.
     * @param context the XWiki context.
     */
    public ApplicationManagerPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

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
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        this.applicationManager = new ApplicationManager(ApplicationManagerMessageTool.getDefault(context));

        // register for documents modifications events
        Utils.getComponent(ObservationManager.class).addListener(this);

        String database = context.getDatabase();
        try {
            XWikiURLFactory urlf =
                context.getWiki().getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);
            context.setDatabase(context.getMainXWiki());
            this.applicationManager.updateAllApplicationTranslation(context);
        } catch (XWikiException e) {
            LOG.error(ApplicationManagerMessageTool.getDefault(context).get(
                ApplicationManagerMessageTool.LOG_REFRESHALLTRANSLATIONS), e);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        try {
            if (XWikiApplicationClass.isApplication(document)) {
                this.applicationManager.updateApplicationsTranslation(document, context);
            }
        } catch (XWikiException e) {
            LOG.error(ApplicationManagerMessageTool.getDefault(context).get(
                ApplicationManagerMessageTool.LOG_AUTOUPDATETRANSLATIONS, document.getFullName()), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ApplicationManagerPluginApi((ApplicationManagerPlugin) plugin, context);
    }
}
