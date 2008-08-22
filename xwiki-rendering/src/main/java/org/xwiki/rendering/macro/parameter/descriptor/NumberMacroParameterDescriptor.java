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
 * Describe a macro parameter that can be a number (min and max values and whether out of range values should be
 * replaced with max or min values or whether the default value should be used instead). 
 * 
 * @param <T> the type of number.
 * @version $Id$
 */
public interface NumberMacroParameterDescriptor<T> extends MacroParameterDescriptor<T>
{
    /**
     * @return the lowest allowed value of the parameter.
     */
    T getMinValue();

    /**
     * @return the highest allowed value of the parameter.
     */
    T getMaxValue();

    /**
     * @return true if the value entered should be replaced by the max or min value if it's out of range. If false
     *         then the default value will be used instead.
     */
    boolean isNormalized();
}
