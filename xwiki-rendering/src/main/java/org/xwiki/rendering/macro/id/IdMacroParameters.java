package org.xwiki.rendering.macro.id;

import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;

public class IdMacroParameters extends DefaultMacroParameters
{
    public IdMacroParameters(Map<String, String> parameters, IdMacroDescriptor macroDescriptor)
    {
        super(parameters, macroDescriptor);
    }

    public String getName() throws MacroParameterException
    {
        return getParameterValue(IdMacroDescriptor.PARAM_NAME);
    }
}
