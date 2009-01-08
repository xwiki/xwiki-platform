package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractRadeoxMacroConverter implements RadeoxMacroConverter
{
    private String name;

    protected AbstractRadeoxMacroConverter()
    {

    }

    protected AbstractRadeoxMacroConverter(String name)
    {
        this.name = name;
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

    protected Map<String, String> convertParameters(Map<String, String> parameters)
    {
        Map<String, String> parameters20 = new LinkedHashMap<String, String>(parameters.size());

        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            convertParameter(parameters20, parameter.getKey(), parameter.getValue());
        }

        return parameters20;
    }

    protected void convertParameter(Map<String, String> parameters20, String key, String value)
    {
        parameters20.put(key, value);
    }

    protected String convertContent(String content)
    {
        return content;
    }

    public String convert(String name, Map<String, String> parameters, String content)
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
            result.append(convertContent(content));
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
            parametersSB.append(parameter.getValue());
        }

        result.append(parametersSB);
    }
}
