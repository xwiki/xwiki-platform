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
 *
 */

package com.xpn.xwiki.plugin.graphviz;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class GraphVizPluginApi extends Api
{
    private GraphVizPlugin plugin;

    public GraphVizPluginApi(GraphVizPlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    public GraphVizPlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return this.plugin;
        }
        return null;
    }

    public void setPlugin(GraphVizPlugin plugin)
    {
        this.plugin = plugin;
    }

    public byte[] getDotImage(String content, boolean dot) throws IOException
    {
        return this.plugin.getDotImage(content, "png", dot);
    }

    public byte[] getDotImage(String content, String extension, boolean dot) throws IOException
    {
        return this.plugin.getDotImage(content, extension, dot);
    }

    public String getDotImageURL(String content, boolean dot) throws IOException
    {
        return this.plugin.getDotImageURL(content, dot, getXWikiContext());
    }

    public String writeDotImage(String content, boolean dot) throws IOException
    {
        return this.plugin.writeDotImage(content, "png", dot);
    }

    public String writeDotImage(String content, String extension, boolean dot) throws IOException
    {
        return this.plugin.writeDotImage(content, extension, dot);
    }

    public void outputDotImage(String content, boolean dot) throws IOException
    {
        this.plugin.outputDotImage(content, "png", dot, getXWikiContext());
    }

    public void outputDotImage(String content, String extension, boolean dot) throws IOException
    {
        this.plugin.outputDotImage(content, extension, dot, getXWikiContext());
    }

    public void flushCache()
    {
        this.plugin.flushCache();
    }
}
