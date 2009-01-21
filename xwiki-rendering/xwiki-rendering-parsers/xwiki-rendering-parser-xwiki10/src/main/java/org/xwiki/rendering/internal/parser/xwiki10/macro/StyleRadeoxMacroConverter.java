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

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;

/**
 * @version $Id$
 * @since 1.8M1
 */
public class StyleRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    protected StyleRadeoxMacroConverter()
    {
        super("box");

        registerParameter("type");
        registerParameter("id");
        registerParameter("class");
        registerParameter("align");
        registerParameter("font-size");
        registerParameter("color");
        registerParameter("font-family");
        registerParameter("width");
        registerParameter("height");
        registerParameter("text");
    }

    @Override
    public String convert(String name, RadeoxMacroParameters parameters, String content)
    {
        StringBuffer result = new StringBuffer();

        appendParameters(result, convertFormatParameters(parameters));

        super.convert(name, parameters, content);

        return result.toString();
    }

    protected Map<String, String> convertFormatParameters(RadeoxMacroParameters parameters)
    {
        Map<String, String> boxParameters = new HashMap<String, String>();

        RadeoxMacroParameter id = parameters.get("id");
        RadeoxMacroParameter align = parameters.get("align");
        RadeoxMacroParameter name = parameters.get("name");
        RadeoxMacroParameter size = parameters.get("font-size");
        RadeoxMacroParameter font = parameters.get("font-family");
        RadeoxMacroParameter color = parameters.get("color");
        RadeoxMacroParameter bgcolor = parameters.get("background-color");
        RadeoxMacroParameter fl = parameters.get("float");
        RadeoxMacroParameter width = parameters.get("width");
        RadeoxMacroParameter height = parameters.get("height");
        RadeoxMacroParameter border = parameters.get("border");

        // add id support
        if ((!"none".equals(id)) && (id != null) && !StringUtils.isEmpty(id.getValue())) {
            boxParameters.put("id", id.getValue().trim());
        }

        // add name support
        if ((!"none".equals(name)) && (name != null) && !StringUtils.isEmpty(name.getValue())) {
            boxParameters.put("name", name.getValue().trim());
        }

        // add align support
        if ((!"none".equals(align)) && (align != null) && !StringUtils.isEmpty(align.getValue())) {
            boxParameters.put("align", align.getValue().trim());
        }

        // add style support
        StringBuffer styleStr = new StringBuffer();

        if ((!"none".equals(size)) && (size != null) && !StringUtils.isEmpty(size.getValue())) {
            styleStr.append("font-size:" + size.getValue().trim() + "; ");
        }
        if ((!"none".equals(font)) && (font != null) && !StringUtils.isEmpty(font.getValue().trim())) {
            styleStr.append("font-family:" + font.getValue().trim() + "; ");
        }
        if ((!"none".equals(color)) && (color != null) && !StringUtils.isEmpty(color.getValue().trim())) {
            styleStr.append("color:" + color.getValue().trim() + "; ");
        }
        if ((!"none".equals(bgcolor)) && (bgcolor != null) && !StringUtils.isEmpty(bgcolor.getValue().trim())) {
            styleStr.append("background-color:" + bgcolor.getValue().trim() + "; ");
        }
        if ((!"none".equals(width)) && (width != null) && !StringUtils.isEmpty(width.getValue().trim())) {
            styleStr.append("width:" + width.getValue().trim() + "; ");
        }
        if ((!"none".equals(fl)) && (fl != null) && !StringUtils.isEmpty(fl.getValue().trim())) {
            styleStr.append("float:" + fl.getValue().trim() + "; ");
        }
        if ((!"none".equals(height)) && (height != null) && !StringUtils.isEmpty(height.getValue().trim())) {
            styleStr.append("height:" + height.getValue().trim() + "; ");
        }
        if ((!"none".equals(border)) && (border != null) && !StringUtils.isEmpty(border.getValue().trim())) {
            styleStr.append("border:" + border.getValue().trim() + "; ");
        }
        styleStr.append("\"");
        boxParameters.put("style", styleStr.toString());

        return boxParameters;
    }

    @Override
    protected Map<String, String> convertParameters(RadeoxMacroParameters parameters)
    {
        Map<String, String> boxParameters = new HashMap<String, String>();

        RadeoxMacroParameter classes = parameters.get("class");
        RadeoxMacroParameter document = parameters.get("document");
        RadeoxMacroParameter icon = parameters.get("icon");
        boolean hasIcon = false;

        if (document != null && document.getValue().contains("=")) {
            document = null;
        }

        if ((!"none".equals(icon)) && (icon != null) && !StringUtils.isEmpty(icon.getValue().trim())) {
            hasIcon = true;
        }

        // add icon support
        if (hasIcon) {
            if (document != null) {
                boxParameters.put("image", document + "@" + icon);
            } else {
                boxParameters.put("image", icon.getValue().trim());
            }
        }

        // add class support
        if ((!"none".equals(classes)) && (classes != null) && !StringUtils.isEmpty(classes.getValue().trim())) {
            boxParameters.put("class", classes.getValue().trim());
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
