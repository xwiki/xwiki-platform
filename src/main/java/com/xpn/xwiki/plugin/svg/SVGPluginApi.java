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
package com.xpn.xwiki.plugin.svg;

import java.io.IOException;

import org.apache.batik.apps.rasterizer.SVGConverterException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

public class SVGPluginApi extends Api {
    private SVGPlugin plugin;

    public SVGPluginApi(SVGPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public SVGPlugin getPlugin() {
        return plugin;
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
        return plugin.getSVGImageURL(content, height, width, context);
    }

    public String writeSVGImage(String content, int height, int width) throws IOException, SVGConverterException {
        return plugin.writeSVGImage(content, "png", height, width);
    }

    public String writeSVGImage(String content, String extension, int height, int width) throws IOException, SVGConverterException {
        return plugin.writeSVGImage(content, extension, height, width);
    }

    public void outputSVGImage(String content, int height, int width) throws IOException, SVGConverterException {
        plugin.outputSVGImage(content, "gif", height, width, context);
    }

    public void outputSVGImage(String content, String extension, int height, int width) throws IOException, SVGConverterException {
        plugin.outputSVGImage(content, extension, height, width, context);
    }

    public void flushCache() {
        plugin.flushCache();
    }
}
