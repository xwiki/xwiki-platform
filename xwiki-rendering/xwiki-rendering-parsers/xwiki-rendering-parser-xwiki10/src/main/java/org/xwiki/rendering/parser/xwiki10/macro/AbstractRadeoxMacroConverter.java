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
package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Base class for Radeox macros converters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public abstract class AbstractRadeoxMacroConverter implements RadeoxMacroConverter
{
    private String name;

    private List<Object[]> parametersNames = new ArrayList<Object[]>();

    protected AbstractRadeoxMacroConverter()
    {

    }

    protected AbstractRadeoxMacroConverter(String name)
    {
        this.name = name;
    }

    public String getParameterName(int parameterIndex)
    {
        return getParameterName(this.parametersNames.get(parameterIndex));
    }

    public int getParameterType(int parameterIndex)
    {
        return getParameterType(this.parametersNames.get(parameterIndex));
    }
    
    private String getParameterName(Object[] objects)
    {
        return objects == null ? null : (String) objects[0];
    }
    
    private int getParameterType(Object[] objects)
    {
        return objects == null ? PARAMETER_SIMPLE : (Integer) objects[1];
    }

    protected void registerParameter(String parameterName)
    {
        registerParameter(parameterName, PARAMETER_SIMPLE);
    }

    protected void registerParameter(String parameterName, int paramType)
    {
        this.parametersNames.add(new Object[] {parameterName, paramType});
    }

    public boolean protectResult()
    {
        return true;
    }

    public boolean isInline()
    {
        return true;
    }

    protected String convertName(String name)
    {
        return this.name == null ? name : this.name;
    }

    protected Map<String, String> convertParameters(RadeoxMacroParameters parameters)
    {
        Map<String, String> parameters20 = new LinkedHashMap<String, String>(parameters.size());

        for (RadeoxMacroParameter radeoxParameter : parameters.values()) {
            convertParameter(parameters20, radeoxParameter);
        }

        return parameters20;
    }

    protected void convertParameter(Map<String, String> parameters20, RadeoxMacroParameter radeoxParameter)
    {
        convertParameter(parameters20, radeoxParameter.getName(), radeoxParameter.getValue());
    }

    protected void convertParameter(Map<String, String> parameters20, String key, String value)
    {
        parameters20.put(key, value);
    }

    protected String convertContent(String content, RadeoxMacroParameters parameters, FilterContext filterContext)
    {
        return content;
    }

    public String convert(String name, RadeoxMacroParameters parameters, String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        result.append("{{");
        result.append(convertName(name));
        if (parameters.size() > 0) {
            result.append(' ');
            appendParameters(result, convertParameters(parameters));
        }

        if (content != null) {
            result.append("}}");

            String macroContent = convertContent(content, parameters, filterContext);
            if (macroContent.indexOf("\n") != -1) {
                result.append("\n" + macroContent + "\n");
            } else {
                result.append(macroContent);
            }

            result.append("{{/");
            result.append(convertName(name));
        } else {
            result.append("/");
        }

        result.append("}}");

        return result.toString();
    }

    protected void appendParameters(StringBuffer result, Map<String, String> parameters)
    {
        StringBuffer parametersSB = new StringBuffer();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (parametersSB.length() > 0) {
                parametersSB.append(" ");
            }
            parametersSB.append(parameter.getKey());
            parametersSB.append("=");
            parametersSB.append("\"" + parameter.getValue() + "\"");
        }

        result.append(parametersSB);
    }
}
