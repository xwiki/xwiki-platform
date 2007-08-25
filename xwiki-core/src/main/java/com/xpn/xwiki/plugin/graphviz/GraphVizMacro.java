/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.plugin.graphviz;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import java.io.IOException;
import java.io.Writer;

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

        boolean dot = !("neato").equals(params.get("type"));
        StringBuffer str = new StringBuffer();
        String img = params.get("text", 0);
        String height = params.get("height", 1);
        String width = params.get("width", 2);

        String dottext = params.getContent();
        str.append("<img src=\"");
        str.append(plugin.getDotImageURL(dottext, dot, xcontext));
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