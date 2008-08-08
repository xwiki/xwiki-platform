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
package org.xwiki.rendering.macro.parameter.instances;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.classes.MacroParameterClass;

/**
 * Macro parameters String values converter.
 * 
 * @param <T> the type of the value after conversion.
 * @version $Id: $
 */
public interface MacroParameter<T>
{
    /**
     * @return parameterClass the macro parameter descriptor.
     */
    MacroParameterClass<T> getParameterClass();

    /**
     * @return the value as String from parser.
     */
    String getValueAsString();

    /**
     * Convert the String value.
     * <p>
     * The calculation is done only once.
     * 
     * @return the converted value.
     * @throws MacroParameterException error when converting value.
     */
    T getValue() throws MacroParameterException;
}
