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

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Base class for Velocity macros converters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public abstract class AbstractVelocityMacroConverter implements VelocityMacroConverter
{
    private String name;

    private List<String> parameterNameList = new ArrayList<String>();

    protected AbstractVelocityMacroConverter()
    {

    }

    protected AbstractVelocityMacroConverter(String name)
    {
        this.name = name;
    }

    protected void addParameterName(String parameterName)
    {
        this.parameterNameList.add(parameterName);
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

    protected Map<String, String> convertParameters(List<String> parameters)
    {
        Map<String, String> parameters20 = new LinkedHashMap<String, String>(parameters.size());

        for (int index = 0; index < parameters.size(); ++index) {
            convertParameter(parameters20, index, parameters.get(index));
        }

        return parameters20;
    }

    protected void convertParameter(Map<String, String> parameters20, int index, String value)
    {
        if (this.parameterNameList.size() > index) {
            String key = this.parameterNameList.get(index);
            if (key != null) {
                parameters20.put(this.parameterNameList.get(index), value);
            }
        }
    }

    protected String convertContent(List<String> parameters, FilterContext context)
    {
        return null;
    }

    protected String cleanQuotes(String value)
    {
        String cleaned = value;

        if (!StringUtils.isEmpty(cleaned)) {
            char firstChar = value.charAt(0);
            char lastChar = value.charAt(value.length() - 1);

            if ((firstChar == '"' || firstChar == '\'') && firstChar == lastChar) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
        }

        return cleaned;
    }

    public String convert(String name, List<String> parameters, FilterContext context)
    {
        StringBuffer begin = new StringBuffer();
        String content = convertContent(parameters, context);
        Map<String, String> params = convertParameters(parameters);

        begin.append("{{");
        begin.append(convertName(name));
        if (params.size() > 0) {
            begin.append(' ');
            appendParameters(begin, params);
        }

        StringBuffer result = new StringBuffer();

        if (content != null) {
            begin.append("}}");
            result.append(!protectResult() ? context.addProtectedContent(begin.toString(), isInline()) : begin);

            result.append(content);

            StringBuffer end = new StringBuffer();
            end.append("{{/");
            end.append(convertName(name));
            end.append("}}");
            result.append(!protectResult() ? context.addProtectedContent(end.toString(), isInline()) : end);
        } else {
            begin.append("/");
            begin.append("}}");

            result.append(!protectResult() ? context.addProtectedContent(begin.toString(), isInline()) : begin);
        }

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
            parametersSB.append(parameter.getValue());
        }

        result.append(parametersSB);
    }
}
