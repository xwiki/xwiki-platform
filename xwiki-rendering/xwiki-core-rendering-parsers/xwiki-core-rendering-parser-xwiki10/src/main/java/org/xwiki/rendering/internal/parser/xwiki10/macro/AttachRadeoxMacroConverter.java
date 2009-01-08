package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;

public class AttachRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    @Override
    public String convert(String name, Map<String, String> parameters, String content)
    {
        StringBuffer result = new StringBuffer();

        if (parameters.size() == 1 || (parameters.size() == 2 && parameters.containsKey("document"))) {
            appendSimpleAttach(result, parameters);
        } else {
            result.append("[[");
            appendSimpleAttach(result, parameters);
            result.append("||");
            Map<String, String> parametersClone = new LinkedHashMap<String, String>(parameters);
            parametersClone.remove("");
            parametersClone.remove("document");
            appendParameters(result, parametersClone);
            result.append("]]");
        }

        return result.toString();
    }

    private void appendSimpleAttach(StringBuffer result, Map<String, String> parameters)
    {
        result.append("attach:");

        String document = parameters.get("document");
        if (document != null) {
            result.append(document);
            result.append("@");
        }

        result.append(parameters.get(""));
    }

    public boolean supportContent()
    {
        return false;
    }
}
