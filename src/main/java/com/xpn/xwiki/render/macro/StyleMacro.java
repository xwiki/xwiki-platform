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
 * @author Phung Nam (phunghainam@xwiki.com)
 */

package com.xpn.xwiki.render.macro;

import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.api.engine.RenderEngine;

import java.io.Writer;
import java.io.IOException;

public class StyleMacro extends BaseLocaleMacro {
    public String getLocaleKey() {
        return "macro.style";
    }

    public void execute(Writer writer, MacroParameter params)
            throws IllegalArgumentException, IOException {
        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();

        String text = params.getContent();
        String type = params.get("type");
        String id = params.get("id");
        String classes = params.get("class");
        String size = params.get("font-size");
        String font = params.get("font-family");
        String color = params.get("color");
        String bgcolor = params.get("background-color");

        if (("none".equals(type)) || (type == null) || ("".equals(type.trim()))) {
            type = "span";
        }
        StringBuffer str = new StringBuffer();
        str.append("<" + type + " ");

        if ((!"none".equals(id)) && (id != null) && (!"".equals(id.trim()))) {
            str.append("id=\"" + id.trim() + "\" ");
        }
        if ((!"none".equals(classes)) && (classes != null) && (!"".equals(classes.trim()))) {
            str.append("class=\"" + classes.trim() + "\" ");
        }

        str.append("style=\"");

        if ((!"none".equals(size)) && (size != null) && (!"".equals(size.trim()))) {
            str.append("font-size:" + size.trim() + "; ");
        }
        if ((!"none".equals(font)) && (font != null) && (!"".equals(font.trim()))) {
            str.append("font-family:" + font.trim() + "; ");
        }
        if ((!"none".equals(color)) && (color != null) && (!"".equals(color.trim()))) {
            str.append("color:" + color.trim() + "; ");
        }
        if ((!"none".equals(bgcolor)) && (bgcolor != null) && (!"".equals(bgcolor.trim()))) {
            str.append("background-color:" + bgcolor.trim() + "; ");
        }
        str.append("\" >");
        str.append(text);
        str.append("</" + type + ">");

        writer.write(str.toString());
    }
}
