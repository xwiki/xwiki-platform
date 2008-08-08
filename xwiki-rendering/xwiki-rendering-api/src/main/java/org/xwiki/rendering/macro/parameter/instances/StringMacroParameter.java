package org.xwiki.rendering.macro.parameter.instances;

import org.xwiki.rendering.macro.parameter.classes.MacroParameterClass;

/**
 * Macro parameter with String value.
 * 
 * @version $Id: $
 */
public class StringMacroParameter extends AbstractMacroParameter<String>
{
    /**
     * @param parameterClass the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public StringMacroParameter(MacroParameterClass<String> parameterClass, String stringValue)
    {
        super(parameterClass, stringValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instances.AbstractMacroParameter#parseValue()
     */
    protected String parseValue()
    {
        return getValueAsString();
    }
}
