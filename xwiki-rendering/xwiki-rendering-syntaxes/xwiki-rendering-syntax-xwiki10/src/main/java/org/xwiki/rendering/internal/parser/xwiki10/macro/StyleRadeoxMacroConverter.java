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

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("style")
public class StyleRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    public StyleRadeoxMacroConverter()
    {
        registerParameter("type");
        registerParameter("id", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("class", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("align", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("name", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("font-size", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("font-family", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("color", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("background-color", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("float", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("width", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("height", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("border", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("document");
        registerParameter("icon", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
    }

    @Override
    public String convert(String name, RadeoxMacroParameters parameters, String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        boolean inline = parameters.get("type") != null && parameters.get("type").getValue().equals("span");

        // Print parameters
        StringBuffer parametersOpen = new StringBuffer();
        Map<String, String> styleParameters = convertParameters(parameters);
        if (styleParameters.size() > 0) {
            parametersOpen.append("(% ");
            appendParameters(parametersOpen, styleParameters);
            parametersOpen.append(" %)");
            result.append(filterContext.addProtectedContent(parametersOpen.toString(), inline));
        }

        if (!inline) {
            // Open standalone group
            result.append(filterContext.addProtectedContent("(((", false));
        }

        // Print content
        result.append(convertContent(content.trim(), parameters, filterContext));

        // Print group close
        if (inline) {
            // Close inline group
            result.append(filterContext.addProtectedContent("(%%)", true));
        } else {
            // Close standalone group
            result.append(filterContext.addProtectedContent(")))", false));
        }

        return result.toString();
    }

    @Override
    protected String convertContent(String content, RadeoxMacroParameters parameters, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        // Print icon
        appendIcon(result, parameters, filterContext);

        if (content.length() > 0) {
            result.append(' ');
        }

        // Print content
        result.append(content);

        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter#convertParameters(org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters)
     */
    @Override
    protected Map<String, String> convertParameters(RadeoxMacroParameters parameters)
    {
        Map<String, String> boxParameters = new LinkedHashMap<String, String>();

        RadeoxMacroParameter classes = parameters.get("class");
        RadeoxMacroParameter icon = parameters.get("icon");
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

        // add class support
        if (classes != null) {
            boxParameters.put("class", classes.getValue().trim());
        } else if (icon != null) {
            boxParameters.put("class", "stylemacro");
        }

        // add id support
        if (id != null) {
            boxParameters.put("id", id.getValue().trim());
        }

        // add name support
        if (name != null) {
            boxParameters.put("name", name.getValue().trim());
        }

        // add align support
        if (align != null) {
            boxParameters.put("align", align.getValue().trim());
        }

        // add style support
        StringBuffer styleStr = new StringBuffer();

        if (size != null) {
            styleStr.append("font-size:" + size.getValue().trim() + "; ");
        }
        if (font != null) {
            styleStr.append("font-family:" + font.getValue().trim() + "; ");
        }
        if (color != null) {
            styleStr.append("color:" + color.getValue().trim() + "; ");
        }
        if (bgcolor != null) {
            styleStr.append("background-color:" + bgcolor.getValue().trim() + "; ");
        }
        if (width != null) {
            styleStr.append("width:" + width.getValue().trim() + "; ");
        }
        if (fl != null) {
            styleStr.append("float:" + fl.getValue().trim() + "; ");
        }
        if (height != null) {
            styleStr.append("height:" + height.getValue().trim() + "; ");
        }
        if (border != null) {
            styleStr.append("border:" + border.getValue().trim() + "; ");
        }

        if (styleStr.length() > 0) {
            boxParameters.put("style", styleStr.toString());
        }

        return boxParameters;
    }

    private void appendIcon(StringBuffer result, RadeoxMacroParameters parameters, FilterContext filterContext)
    {
        RadeoxMacroParameter document = parameters.get("document");
        RadeoxMacroParameter icon = parameters.get("icon");
        boolean hasIcon = false;

        if (document != null && document.getValue().contains("=")) {
            document = null;
        }

        if (icon != null) {
            hasIcon = true;
        }

        // add icon support
        if (hasIcon) {
            result.append(filterContext.addProtectedContent("image:", true));
            if (document != null) {
                result.append(document + "@" + icon);
            } else {
                result.append(icon.getValue().trim());
            }
        }
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
