/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.macro.parameter.descriptor;

/**
 * Describe a macro parameter that can be a number.
 * 
 * @param <T> the type of number.
 * @version $Id$
 */
public abstract class AbstractNumberMacroParameterDescriptor<T extends Number> extends
    AbstractMacroParameterDescriptor<T> implements NumberMacroParameterDescriptor<T>
{
    /**
     * The lowest possible value of the parameter.
     */
    private T minValue;

    /**
     * The highest possible value of the parameter.
     */
    private T maxValue;

    /**
     * True if the value has to be normalize. If false the default value is used when value is too low or too high.
     */
    private boolean normalized = true;

    /**
     * @param name the name of the parameter.
     * @param descritpion the description of the parameter.
     * @param def the default value. Have to be not null.
     */
    public AbstractNumberMacroParameterDescriptor(String name, String descritpion, T def)
    {
        super(name, descritpion, def);
    }

    /**
     * @param minValue the lowest possible value of the parameter.
     */
    public void setMinValue(T minValue)
    {
        this.minValue = minValue;
    }

    /**
     * @return the lowest possible value of the parameter.
     */
    public T getMinValue()
    {
        return this.minValue;
    }

    /**
     * @param maxValue the highest possible value of the parameter.
     */
    public void setMaxValue(T maxValue)
    {
        this.maxValue = maxValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.descriptor.NumberMacroParameterDescriptor#getMaxValue()
     */
    public T getMaxValue()
    {
        return this.maxValue;
    }

    /**
     * @param normalize true if the value has to be normalize. If false the default value is used when value is too low
     *            or too high.
     */
    public void setNormalized(boolean normalize)
    {
        this.normalized = normalize;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.descriptor.NumberMacroParameterDescriptor#isNormalized()
     */
    public boolean isNormalized()
    {
        return this.normalized;
    }
}
