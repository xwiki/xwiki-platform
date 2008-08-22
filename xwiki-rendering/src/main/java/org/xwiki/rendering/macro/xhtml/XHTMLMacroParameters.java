package org.xwiki.rendering.macro.xhtml;

import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;

public class XHTMLMacroParameters extends DefaultMacroParameters
{
    public XHTMLMacroParameters(Map<String, String> parameters, XHTMLMacroDescriptor macroDescriptor)
    {
        super(parameters, macroDescriptor);
    }

    /**
     * @return indicate if the user has asked to escape wiki syntax or not.
     * @exception MacroParameterException error when converting value.
     */
    public boolean isWikiSyntaxEscaped() throws MacroParameterException
    {
        return this.<Boolean> getParameterValue(XHTMLMacroDescriptor.PARAM_ESCAPEWIKISYNTAX);
    }
}
