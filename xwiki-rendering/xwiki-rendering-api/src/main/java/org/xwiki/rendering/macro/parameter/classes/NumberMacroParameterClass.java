package org.xwiki.rendering.macro.parameter.classes;

/**
 * Describe a macro parameter that can be a number.
 * 
 * @param <T> the type of number.
 * @version $Id: $
 */
public interface NumberMacroParameterClass<T> extends MacroParameterClass<T>
{
    /**
     * @return the lowest possible value of the parameter.
     */
    T getMinValue();

    /**
     * @return the highest possible value of the parameter.
     */
    T getMaxValue();

    /**
     * @return true if the value has to be normalize. If false the default value is used when value is too low or too
     *         high.
     */
    boolean isNormalized();
}
