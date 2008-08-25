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
package com.xpn.xwiki.plugin.wysiwyg;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.wysiwyg.server.converter.HTMLConverter;

/**
 * Api for the WysiwygPlugin.
 */
public class WysiwygPluginApi extends Api
{
    /**
     * the plugin instance
     */
    private WysiwygPlugin plugin;

    public WysiwygPluginApi(WysiwygPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * @see #plugin
     */
    public WysiwygPlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    /**
     * @see #plugin
     */
    public void setPlugin(WysiwygPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @return true if the editor is enabled.
     */
    public boolean isEnabled()
    {
        return getXWikiContext().getWiki().Param("xwiki.wysiwygnew", "1").equals("1");
    }

    /**
     * @param syntaxId The syntax identifier, like "xwiki/2.0".
     * @return true if the specified syntax is currently supported by the editor.
     */
    public boolean isSyntaxSupported(String syntaxId)
    {
        try {
            Utils.getComponent(HTMLConverter.ROLE, syntaxId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
