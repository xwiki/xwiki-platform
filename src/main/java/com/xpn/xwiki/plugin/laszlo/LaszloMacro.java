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

package com.xpn.xwiki.plugin.laszlo;

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

public class LaszloMacro  extends BaseLocaleMacro {
  public String getLocaleKey() {
    return "macro.laszlo";
  }

    public void execute(Writer writer, MacroParameter params)
            throws IllegalArgumentException, IOException {

        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
        XWiki xwiki = xcontext.getWiki();

        LaszloPlugin plugin = (LaszloPlugin) xwiki.getPlugin("laszlo", xcontext);
        // If the plugin is not loaded
        if (plugin==null) {
            writer.write("Plugin not loaded");
            return;
        }

        StringBuffer str = new StringBuffer();
        String name = params.get("name", 0);
        String height = params.get("height", 1);
        String width = params.get("width", 2);

        String laszlocode = params.getContent();
        try {
         str.append(plugin.getLaszloFlash(name, width, height, laszlocode, xcontext));
        } catch (XWikiException e) {
            str.append("Laszlo Error");
        }
        writer.write(str.toString());
    }
}