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
 * Date: 24 août 2004
 * Time: 20:03:03
 */
package com.xpn.xwiki.plugin.svg;

import java.io.IOException;
import java.io.Writer;

import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

public class SVGMacro  extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.svg";
  }

    public void execute(Writer writer, MacroParameter params)
            throws IllegalArgumentException, IOException {

        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
        XWiki xwiki = xcontext.getWiki();

        SVGPlugin plugin = (SVGPlugin) xwiki.getPlugin("svg", xcontext);
        // If the plugin is not loaded
        if (plugin==null) {
            writer.write("Plugin not loaded");
            return;
        }
        /* {svg:image|height|width} */
        StringBuffer str = new StringBuffer();
        String text = params.get("text", 0);
        String height = params.get("height", 1);
        if ((height==null)||("none".equals(height))) {
        	height = "400";
        }
        String width = params.get("width", 2);
        if ((width==null)||("none".equals(width))) {
        	width = "400";
        }
        try {
	        int intHeight = Integer.parseInt(height);
	        int intWidth = Integer.parseInt(width);
	        String svgtext = params.getContent();
	        str.append("<img src=\"");
	        str.append(plugin.getSVGImageURL(svgtext, intHeight, intWidth, xcontext));
	        str.append("\" ");
	        str.append("height=\"" + height + "\" ");
	        str.append("width=\"" + width + "\" ");
	        str.append("alt=\"");
	        str.append(text);
	        str.append("\" />");
	        writer.write(str.toString());
	    } catch (Throwable t) {
	    	XWikiException e = new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "SVG Issue", t);
	    	writer.write("Exception converting SVG: " + e.getFullMessage());
	    }
    }
}