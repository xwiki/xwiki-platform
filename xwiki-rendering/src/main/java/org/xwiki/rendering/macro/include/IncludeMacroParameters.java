package org.xwiki.rendering.macro.include;

import java.util.Map;

import org.xwiki.rendering.macro.include.IncludeMacroDescriptor.Context;
import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;

public class IncludeMacroParameters extends DefaultMacroParameters
{
    public IncludeMacroParameters(Map<String, String> parameters, IncludeMacroDescriptor macroDescriptor)
    {
        super(parameters, macroDescriptor);
    }

    /**
     * @return the name of the document to include.
     * @exception MacroParameterException error when converting value.
     */
    public String getDocument() throws MacroParameterException
    {
        return getParameterValue(IncludeMacroDescriptor.PARAM_DOCUMENT);
    }

    /**
     * @return defines whether the included page is executed in its separated execution context or whether it's executed
     *         in the contex of the current page.
     * @exception MacroParameterException error when converting value.
     */
    public Context getContext() throws MacroParameterException
    {
        return getParameterValue(IncludeMacroDescriptor.PARAM_CONTEXT);
    }
}
