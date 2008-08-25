package org.xwiki.rendering.macro.parameter;

public class ParameterValueTooLowException extends MacroParameterException
{
    public ParameterValueTooLowException(int maxValue)
    {
        super("The value is too low. The lowest allowed value is " + maxValue + ".");
    }
}
