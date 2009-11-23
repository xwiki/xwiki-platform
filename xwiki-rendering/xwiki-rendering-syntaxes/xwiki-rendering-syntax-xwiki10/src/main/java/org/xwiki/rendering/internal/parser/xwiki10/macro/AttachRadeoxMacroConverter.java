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
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("attach")
public class AttachRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    public AttachRadeoxMacroConverter()
    {
        registerParameter("");
        registerParameter("file");
        registerParameter("document");
        registerParameter("title");
        registerParameter("rel");
        registerParameter("id");
        registerParameter("fromIncludingDoc");
    }

    @Override
    public String convert(String name, RadeoxMacroParameters parameters, String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        if (parameters.size() == 1 || (parameters.size() == 2 && parameters.containsKey("document"))) {
            appendSimpleAttach(result, parameters);
        } else {
            result.append("[[");
            if (parameters.containsKey("file")) {
                result.append(parameters.get(""));
                result.append(">>");
            }
            appendSimpleAttach(result, parameters);
            result.append("||");
            Map<String, String> parametersClone = convertParameters(parameters);
            parametersClone.remove("");
            parametersClone.remove("document");
            parametersClone.remove("file");
            appendParameters(result, parametersClone);
            result.append("]]");
        }

        return result.toString();
    }

    private void appendSimpleAttach(StringBuffer result, RadeoxMacroParameters parameters)
    {
        result.append("attach:");

        RadeoxMacroParameter document = parameters.get("document");
        if (document != null) {
            result.append(document);
            result.append("@");
        }

        result.append(parameters.containsKey("file") ? parameters.get("file") : parameters.get(""));
    }

    public boolean supportContent()
    {
        return false;
    }
}
