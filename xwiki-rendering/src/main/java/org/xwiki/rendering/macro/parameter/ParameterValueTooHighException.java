package org.xwiki.rendering.macro.parameter;

public class ParameterValueTooHighException extends MacroParameterException
{
    public ParameterValueTooHighException(int maxValue)
    {
        super("The value is too high. The highest allowed value is " + maxValue + ".");
    }
}
