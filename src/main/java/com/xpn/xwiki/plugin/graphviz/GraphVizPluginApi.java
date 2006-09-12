/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 *
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.graphviz;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import java.io.IOException;

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
