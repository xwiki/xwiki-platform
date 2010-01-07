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
@Component("image")
public class ImageRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    public ImageRadeoxMacroConverter()
    {
        registerParameter("");
        registerParameter("height", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("width", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("align", RadeoxMacroConverter.PARAMETER_NOTEMPTYNONE);
        registerParameter("halign", RadeoxMacroConverter.PARAMETER_NOTNONE);
        registerParameter("document", RadeoxMacroConverter.PARAMETER_NOTEMPTY);
        registerParameter("alt");
        registerParameter("title");
        registerParameter("link");
        registerParameter("fromIncludingDoc");
    }

    @Override
    public String convert(String name, RadeoxMacroParameters parameters, String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Map<String, String> parametersClone = convertParameters(parameters);

        // Read the parameters
        RadeoxMacroParameter img = parameters.get("");
        // Allow the image to contain '=' when it is an URL.
        if (null == img || (img.getValue().indexOf("=") != -1 && img.getValue().indexOf("://") == -1)) {
            return "";
        }

        // Fix missing alt or title parameters
        RadeoxMacroParameter alt = parameters.get("alt");
        RadeoxMacroParameter title = parameters.get("title");
        if (alt == null && title != null) {
            alt = title;
            parametersClone.put(title.getName(), title.getValue());
        } else if (alt != null && title == null) {
            title = alt;
            parametersClone.put(alt.getName(), alt.getValue());
        } else {
            alt = title = img;
        }
        parametersClone.put(alt.getName(), alt.getValue());
        parametersClone.put(title.getName(), title.getValue());

        // print halign begin
        RadeoxMacroParameter halign = parameters.get("halign");
        if (halign != null) {
            result.append("(% class=\"img" + halign.getValue().trim() + "\" %)");
            result.append("(((");
            parametersClone.remove("halign");
        }

        // print link begin
        RadeoxMacroParameter link = parameters.get("link");
        if (link != null && link.getValue().indexOf("=") == -1 && !link.getValue().toLowerCase().startsWith("f")) {
            result.append("[[");
            parametersClone.remove("link");
        }

        // print the image itself
        result.append("[[");
        if (parameters.size() == 1 || (parameters.size() == 2 && parameters.containsKey("document"))) {
            appendSimpleImage(result, parameters);
        } else {
            appendSimpleImage(result, parameters);
            parametersClone.remove("");
            parametersClone.remove("document");
            result.append("||");
            appendParameters(result, parametersClone);
        }
        result.append("]]");

        // print link end
        if (link != null && link.getValue().indexOf("=") == -1 && !link.getValue().toLowerCase().startsWith("f")) {
            result.append(">>attach:");
            appendAttachmentReference(result, parameters);
            result.append("]]");
        }

        // print halign end
        if (halign != null) {
            result.append(")))");
        }

        return result.toString();
    }

    private void appendSimpleImage(StringBuffer result, RadeoxMacroParameters parameters)
    {
        result.append("image:");

        appendAttachmentReference(result, parameters);
    }

    private void appendAttachmentReference(StringBuffer result, RadeoxMacroParameters parameters)
    {
        RadeoxMacroParameter document = parameters.get("document");
        if (document != null) {
            result.append(document.getValue());
            result.append("@");
        }

        result.append(parameters.get(""));
    }

    public boolean supportContent()
    {
        return false;
    }
}
