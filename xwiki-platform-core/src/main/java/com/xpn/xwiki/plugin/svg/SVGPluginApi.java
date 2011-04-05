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

package com.xpn.xwiki.plugin.svg;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import org.apache.batik.apps.rasterizer.SVGConverterException;

import java.io.IOException;

public class SVGPluginApi extends Api {
    private SVGPlugin plugin;

    public SVGPluginApi(SVGPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public SVGPlugin getPlugin() {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    public void setPlugin(SVGPlugin plugin) {
        this.plugin = plugin;
    }

    public byte[] getSVGImage(String content, int height, int width) throws IOException, SVGConverterException	 {
        return plugin.getSVGImage(content, "png", height, width);
    }

    public byte[] getSVGImage(String content, String extension, int height, int width) throws IOException, SVGConverterException {
        return plugin.getSVGImage(content, extension, height, width);
    }

    public String getSVGImageURL(String content, int height, int width) throws IOException, SVGConverterException {
        return plugin.getSVGImageURL(content, height, width, getXWikiContext());
    }

    public String writeSVGImage(String content, int height, int width) throws IOException, SVGConverterException {
        return plugin.writeSVGImage(content, "png", height, width);
    }

    public String writeSVGImage(String content, String extension, int height, int width) throws IOException, SVGConverterException {
        return plugin.writeSVGImage(content, extension, height, width);
    }

    public void outputSVGImage(String content, int height, int width) throws IOException, SVGConverterException {
        plugin.outputSVGImage(content, "gif", height, width, getXWikiContext());
    }

    public void outputSVGImage(String content, String extension, int height, int width) throws IOException, SVGConverterException {
        plugin.outputSVGImage(content, extension, height, width, getXWikiContext());
    }

    public void flushCache() {
        plugin.flushCache();
    }
}
