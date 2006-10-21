/*
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 * 
  * Created on 27.01.2005
 *
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class EmailNotificationPluginApi extends Api
{
    private static final Logger     LOG = Logger.getLogger (EmailNotificationPluginApi.class);
    private EmailNotificationPlugin plugin;

    public EmailNotificationPluginApi (EmailNotificationPlugin plugin, XWikiContext context)
    {
        super (context);
        setPlugin (plugin);
    }

    /**
     * @param plugin
     */
    private void setPlugin (EmailNotificationPlugin plugin)
    {
        this.plugin = plugin;
    }

    public boolean isSubscribed (String type, Document doc, XWiki wiki)
    {
        return plugin.isSubscribed (type, doc, wiki, context);
    }

    /**
     * Toggle subscription
     * @param type: "page", "web", or "wiki"
     * @param doc
     * @param wiki
     */
    public void processSubscription (String type, Document doc, XWiki wiki)
    {
        plugin.processSubscription (type, doc, wiki, context);
    }
  
    /**
     * Set/unset subscription
     * @param enable: "true" to subscribe, "false" to unsubscribe
     * @param type: "page", "web", or "wiki"
     * @param doc
     * @param wiki
     */    
    public void processSubscription (String enable, String type, Document doc, XWiki wiki)
    {
    	if (enable.equals("true")) {
    		plugin.processSubscription(true, type, doc, wiki, context);
    	} else if (enable.equals("false")) { 
    		plugin.processSubscription(false, type, doc, wiki, context);
    	} else {
    		LOG.warn("Unknown value for enable: " + enable);
    	}

    }
    
}
