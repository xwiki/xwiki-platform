package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    protected String convertContent(List<String> parameters)
    {
        return null;
    }

    public String convert(String name, List<String> parameters)
    {
        StringBuffer result = new StringBuffer();

        result.append("{{");
        result.append(convertName(name));
        if (parameters.size() > 0) {
            result.append(' ');
            appendParameters(result, convertParameters(parameters));
        }

        String content = convertContent(parameters);
        if (content != null) {
            result.append("}}");
            result.append(content);
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
