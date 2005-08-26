/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 avr. 2005
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.graphviz;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class GraphVizPluginApi extends Api {
    private GraphVizPlugin plugin;

    public GraphVizPluginApi(GraphVizPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public GraphVizPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(GraphVizPlugin plugin) {
        this.plugin = plugin;
    }

    public byte[] getDotImage(String content, boolean dot) throws IOException {
        return plugin.getDotImage(content, "gif", dot);
    }

    public byte[] getDotImage(String content, String extension, boolean dot) throws IOException {
        return plugin.getDotImage(content, extension, dot);
    }

    public String getDotImageURL(String content, boolean dot) throws IOException {
        return plugin.getDotImageURL(content, dot, context);
    }

    public String writeDotImage(String content, boolean dot) throws IOException {
        return plugin.writeDotImage(content, "gif", dot);
    }

    public String writeDotImage(String content, String extension, boolean dot) throws IOException {
        return plugin.writeDotImage(content, extension, dot);
    }

    public void outputDotImage(String content, boolean dot) throws IOException {
        plugin.outputDotImage(content, "gif", dot, context);
    }

    public void outputDotImage(String content, String extension, boolean dot) throws IOException {
        plugin.outputDotImage(content, extension, dot, context);
    }

    public void flushCache() {
        plugin.flushCache();
    }
}
