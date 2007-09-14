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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;
import java.io.Writer;

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