package org.xwiki.rendering.macro.parameter.instance;

import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * Macro parameter with String value.
 * 
 * @version $Id$
 */
public class StringMacroParameter extends AbstractMacroParameter<String>
{
    /**
     * @param parameterDescriptor the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public StringMacroParameter(MacroParameterDescriptor<String> parameterDescriptor, String stringValue)
    {
        super(parameterDescriptor, stringValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#parseValue()
     */
    @Override
    protected String parseValue()
    {
        return getValueAsString();
    }
}
