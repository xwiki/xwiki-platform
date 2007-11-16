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

package com.xpn.xwiki.plugin.jodatime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin for manipulating dates from velocity scripts inside xwiki documents. It is based on the
 * <a href="http://joda-time.sourceforge.net/">JodaTime framework</a>, a quality replacement for the
 * Java date and time classes.
 * 
 * @see JodaTimePluginApi
 */
public class JodaTimePlugin extends XWikiDefaultPlugin
{
    /**
     * {@inheritDoc}
     *
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public JodaTimePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "jodatime";
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new JodaTimePluginApi((JodaTimePlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
    public void init(XWikiContext context)
    {
        super.init(context);
    }
}
