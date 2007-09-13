/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.plugin.exoplatform;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import javax.jcr.Session;

/**
 * Allows getting an Exoplatform JCR Session so that it becomes possible to get data out of JCR
 * and display it inside XWiki or store XWiki data in Exo's JCR.
 *
 * @version $Id: $
 */
public class ExoPlatformPluginApi extends Api
{
    private ExoPlatformPlugin plugin;

    public ExoPlatformPluginApi(ExoPlatformPlugin plugin, XWikiContext context)
    {
        super(context);
        this.plugin = plugin; 
    }

    public Session getRepositorySession() throws XWikiException
    {
        return this.plugin.getRepositorySession(getXWikiContext());
    }
}
