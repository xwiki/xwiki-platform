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
package com.xpn.xwiki.plugin.graphviz;

import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.api.engine.RenderEngine;

import java.io.Writer;
import java.io.IOException;

import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;

public class GraphVizMacro  extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.graphviz";
  }

    public void execute(Writer writer, MacroParameter params)
            throws IllegalArgumentException, IOException {

        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
        XWiki xwiki = xcontext.getWiki();

        GraphVizPlugin plugin = (GraphVizPlugin) xwiki.getPlugin("graphviz", xcontext);
        // If the plugin is not loaded
        if (plugin==null) {
            writer.write("Plugin not loaded");
            return;
        }

        StringBuffer str = new StringBuffer();
        String img = params.get("text", 0);
        String height = params.get("height", 1);
        String width = params.get("width", 2);

        String dottext = params.getContent();
        str.append("<img src=\"");
        str.append(plugin.getDotImageURL(dottext, xcontext));
        str.append("\" ");
        if ((!"none".equals(height))&&(height!=null))
            str.append("height=\"" + height + "\" ");
        if ((!"none".equals(width))&&(width!=null))
            str.append("width=\"" + width + "\" ");
        str.append("alt=\"");
        str.append(img);
        str.append("\" />");
        writer.write(str.toString());
    }
}