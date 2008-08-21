package org.xwiki.rendering.macro;

import java.util.Map;

import org.xwiki.rendering.macro.TocMacroDescriptor.Scope;
import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;

public class TocMacroParameters extends DefaultMacroParameters
{
    public TocMacroParameters(Map<String, String> parameters, TocMacroDescriptor macroDescriptor)
    {
        super(parameters, macroDescriptor);
    }

    /**
     * @return the minimum section level. For example if 2 then level 1 sections will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getStart() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(TocMacroDescriptor.PARAM_START);
    }

    /**
     * @return the maximum section level. For example if 3 then all section levels from 4 will not be listed.
     * @exception MacroParameterException error when converting value.
     */
    public int getDepth() throws MacroParameterException
    {
        return this.<Integer> getParameterValue(TocMacroDescriptor.PARAM_DEPTH);
    }

    /**
     * @return local or page. If local only section in the current scope will be listed. For example if the macro is
     *         written in a section, only subsections of this section will be listed.
     * @exception MacroParameterException error when converting value.
     */
    public Scope getScope() throws MacroParameterException
    {
        return getParameterValue(TocMacroDescriptor.PARAM_SCOPE);
    }

    /**
     * @return true or false. If true the section title number is printed.
     * @exception MacroParameterException error when converting value.
     */
    public boolean numbered() throws MacroParameterException
    {
        return this.<Boolean> getParameterValue(TocMacroDescriptor.PARAM_NUMBERED);
    }
}
