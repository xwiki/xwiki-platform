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
 */
package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.Writer;

import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;

public class StyleMacro extends BaseLocaleMacro
{
    @Override
    public String getLocaleKey()
    {
        return "macro.style";
    }

    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {
        RenderContext context = params.getContext();
        RenderEngine engine = context.getRenderEngine();
        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) engine).getXWikiContext();

        String text = params.getContent();
        String type = params.get("type");
        String id = params.get("id");
        String classes = params.get("class");
        String align = params.get("align");
        String name = params.get("name");
        String size = params.get("font-size");
        String font = params.get("font-family");
        String color = params.get("color");
        String bgcolor = params.get("background-color");
        String fl = params.get("float");
        String width = params.get("width");
        String height = params.get("height");
        String border = params.get("border");
        String document = params.get("document");
        String icon = params.get("icon");
        boolean hasIcon = false;

        if (null == document || document.indexOf("=") != -1) {
            document = null;
        }

        if ((!"none".equals(icon)) && (icon != null) && (!"".equals(icon.trim()))) {
            hasIcon = true;
        }
        // Get the target document
        XWikiDocument doc = null;

        if (document != null && !("".equals(document))) {
            String space = "";
            if (document.indexOf(".") >= 0) {
                space = document.substring(0, document.indexOf(".")).trim();
                document = document.substring(document.indexOf(".") + 1, document.length()).trim();
            }
            try {
                if (space.equals("")) {
                    space = xcontext.getDoc().getSpace();
                }
                doc = xcontext.getWiki().getDocument(space, document, xcontext);
            } catch (XWikiException e) {
                // NullPointer or ClassCast
            }
        } else {
            doc = xcontext.getDoc();
        }

        String path = "";
        if (icon != null) {
            XWikiAttachment image = doc.getAttachment(icon);
            if (image != null) {
                path = doc.getAttachmentURL(icon, "download", xcontext);
            } else {
                icon = "icons/" + icon; // icons default directory that contain icon image.
                path = xcontext.getWiki().getSkinFile(icon, xcontext);
            }
        }

        if (("none".equals(type)) || (type == null) || ("".equals(type.trim()))) {
            type = "span";
        }
        StringBuilder str = new StringBuilder();
        str.append("<" + type + " ");

        if ((!"none".equals(id)) && (id != null) && (!"".equals(id.trim()))) {
            str.append("id=\"" + id.trim() + "\" ");
        }
        if ((!"none".equals(classes)) && (classes != null) && (!"".equals(classes.trim()))) {
            str.append("class=\"" + classes.trim() + "\" ");
        } else if (hasIcon) {
            str.append("class=\"stylemacro\" ");
        }
        if ((!"none".equals(name)) && (name != null) && (!"".equals(name.trim()))) {
            str.append("name=\"" + name.trim() + "\" ");
        }
        if ((!"none".equals(align)) && (align != null) && (!"".equals(align.trim()))) {
            str.append("align=\"" + align.trim() + "\" ");
        }

        str.append("style=\"");

        if (hasIcon) {
            str.append("background-image: url(" + path + "); ");
        }
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
        if ((!"none".equals(width)) && (width != null) && (!"".equals(width.trim()))) {
            str.append("width:" + width.trim() + "; ");
        }
        if ((!"none".equals(fl)) && (fl != null) && (!"".equals(fl.trim()))) {
            str.append("float:" + fl.trim() + "; ");
        }
        if ((!"none".equals(height)) && (height != null) && (!"".equals(height.trim()))) {
            str.append("height:" + height.trim() + "; ");
        }
        if ((!"none".equals(border)) && (border != null) && (!"".equals(border.trim()))) {
            str.append("border:" + border.trim() + "; ");
        }
        str.append("\" >");
        if ((!"none".equals(text)) && (text != null) && (!"".equals(text.trim()))) {
            str.append(text);
        }
        str.append("</" + type + ">");

        writer.write(str.toString());
    }
}
