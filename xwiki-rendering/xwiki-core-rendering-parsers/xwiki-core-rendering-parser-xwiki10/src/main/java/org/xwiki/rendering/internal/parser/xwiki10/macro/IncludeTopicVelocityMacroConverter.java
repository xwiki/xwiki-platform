package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.List;
import java.util.Map;

public class IncludeTopicVelocityMacroConverter extends IncludeInContextVelocityMacroConverter
{
    @Override
    protected Map<String, String> convertParameters(List<String> parameters)
    {
        Map<String, String> parameters20 = super.convertParameters(parameters);

        parameters20.put("context", "new");

        return parameters20;
    }
}
