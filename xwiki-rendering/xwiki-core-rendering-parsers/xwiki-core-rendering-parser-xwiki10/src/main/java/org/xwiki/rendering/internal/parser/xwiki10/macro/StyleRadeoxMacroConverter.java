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
package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;

/**
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class StyleRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    protected StyleRadeoxMacroConverter()
    {
        super("box");
    }

    @Override
    public String convert(String name, Map<String, String> parameters, String content)
    {
        StringBuffer result = new StringBuffer();

        appendParameters(result, convertFormatParameters(parameters));

        super.convert(name, parameters, content);

        return result.toString();
    }

    protected Map<String, String> convertFormatParameters(Map<String, String> parameters)
    {
        Map<String, String> boxParameters = new HashMap<String, String>();

        String id = parameters.get("id");
        String align = parameters.get("align");
        String name = parameters.get("name");
        String size = parameters.get("font-size");
        String font = parameters.get("font-family");
        String color = parameters.get("color");
        String bgcolor = parameters.get("background-color");
        String fl = parameters.get("float");
        String width = parameters.get("width");
        String height = parameters.get("height");
        String border = parameters.get("border");

        // add id support
        if ((!"none".equals(id)) && (id != null) && (!"".equals(id.trim()))) {
            boxParameters.put("id", id.trim());
        }

        // add name support
        if ((!"none".equals(name)) && (name != null) && (!"".equals(name.trim()))) {
            boxParameters.put("name", name.trim());
        }

        // add align support
        if ((!"none".equals(align)) && (align != null) && (!"".equals(align.trim()))) {
            boxParameters.put("align", align.trim());
        }

        // add style support
        StringBuffer styleStr = new StringBuffer();

        if ((!"none".equals(size)) && (size != null) && (!"".equals(size.trim()))) {
            styleStr.append("font-size:" + size.trim() + "; ");
        }
        if ((!"none".equals(font)) && (font != null) && (!"".equals(font.trim()))) {
            styleStr.append("font-family:" + font.trim() + "; ");
        }
        if ((!"none".equals(color)) && (color != null) && (!"".equals(color.trim()))) {
            styleStr.append("color:" + color.trim() + "; ");
        }
        if ((!"none".equals(bgcolor)) && (bgcolor != null) && (!"".equals(bgcolor.trim()))) {
            styleStr.append("background-color:" + bgcolor.trim() + "; ");
        }
        if ((!"none".equals(width)) && (width != null) && (!"".equals(width.trim()))) {
            styleStr.append("width:" + width.trim() + "; ");
        }
        if ((!"none".equals(fl)) && (fl != null) && (!"".equals(fl.trim()))) {
            styleStr.append("float:" + fl.trim() + "; ");
        }
        if ((!"none".equals(height)) && (height != null) && (!"".equals(height.trim()))) {
            styleStr.append("height:" + height.trim() + "; ");
        }
        if ((!"none".equals(border)) && (border != null) && (!"".equals(border.trim()))) {
            styleStr.append("border:" + border.trim() + "; ");
        }
        styleStr.append("\"");
        boxParameters.put("style", styleStr.toString());

        return boxParameters;
    }

    @Override
    protected Map<String, String> convertParameters(Map<String, String> parameters)
    {
        Map<String, String> boxParameters = new HashMap<String, String>();

        String classes = parameters.get("class");
        String document = parameters.get("document");
        String icon = parameters.get("icon");
        boolean hasIcon = false;

        if (null == document || document.indexOf("=") != -1) {
            document = null;
        }

        if ((!"none".equals(icon)) && (icon != null) && (!"".equals(icon.trim()))) {
            hasIcon = true;
        }

        // add icon support
        if (hasIcon) {
            if (document != null) {
                boxParameters.put("image", document + "@" + icon);
            } else {
                boxParameters.put("image", icon);
            }
        }

        // add class support
        if ((!"none".equals(classes)) && (classes != null) && (!"".equals(classes.trim()))) {
            boxParameters.put("class", classes.trim());
        } else if (hasIcon) {
            boxParameters.put("class", "stylemacro");
        }

        return boxParameters;
    }

    @Override
    public boolean protectResult()
    {
        return false;
    }

    public boolean supportContent()
    {
        return true;
    }
}
