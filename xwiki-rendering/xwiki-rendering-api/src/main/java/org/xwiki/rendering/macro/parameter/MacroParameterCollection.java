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
package org.xwiki.rendering.macro.parameter;

import java.util.Map;

import org.xwiki.rendering.macro.parameter.classes.MacroParameterClass;
import org.xwiki.rendering.macro.parameter.instances.MacroParameter;

/**
 * Parse and convert macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id: $
 */
public interface MacroParameterCollection
{
    /**
     * @return the list of parameters descriptors.
     */
    Map<String, MacroParameterClass< ? >> getParametersClasses();

    /**
     * @param <P> the type of MacroParameter child class to return.
     * @param name the name of the parameter.
     * @return the parameter object.
     * @throws MacroParameterException error when trying to get macro parameter object.
     */
    <P extends MacroParameter< ? >> P getParameter(String name) throws MacroParameterException;

    /**
     * @param <C> the type of MacroParameterClass child class to return.
     * @param name the name of the parameter.
     * @return the parameter class.
     * @throws MacroParameterException error when trying to get macro parameter class.
     */
    <C extends MacroParameterClass< ? >> C getParameterClass(String name) throws MacroParameterException;

    /**
     * @param <T> the type of value returned by parameter object.
     * @param name the name of the parameter.
     * @return the parameter object.
     * @throws MacroParameterException error when trying to get macro parameter object.
     */
    <T> T getParameterValue(String name) throws MacroParameterException;
}
