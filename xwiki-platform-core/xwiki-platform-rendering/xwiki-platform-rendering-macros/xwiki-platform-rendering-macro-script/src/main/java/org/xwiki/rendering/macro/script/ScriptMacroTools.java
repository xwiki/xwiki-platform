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
package org.xwiki.rendering.macro.script;

import java.lang.reflect.Type;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

/**
 * Various tools to manipulate script and macro related metadata.
 * 
 * @version $Id$
 * @since 18.4.0RC1
 * @since 17.10.9
 */
@Role
@Unstable
public interface ScriptMacroTools
{
    /**
     * Get the value of a script parameter.
     * <p>
     * The value is retrieved from the script context and the method also make sure the current context allow to access
     * a script value.
     * 
     * @param <T> the type to return
     * @param variableName the name of the script binding
     * @param context the macro transformation context
     * @return the value of the script binding
     * @throws MacroExecutionException if any problem occurred while retrieving the value of the script binding or
     *             converting it to the passed {@link Type}
     */
    <T> T getScriptValue(String variableName, MacroTransformationContext context) throws MacroExecutionException;

    /**
     * Get the value of a script parameter, converted into the requested type.
     * <p>
     * The value is retrieved from the script context and the method also make sure the current context allow to access
     * a script value.
     * 
     * @param <T> the type to return
     * @param variableName the name of the script binding
     * @param context the macro transformation context
     * @param type the type to return
     * @return the value of the script binding in the passed {@link Type}
     * @throws MacroExecutionException if any problem occurred while retrieving the value of the script binding or
     *             converting it to the passed {@link Type}
     */
    <T> T getScriptValue(String variableName, MacroTransformationContext context, Type type)
        throws MacroExecutionException;
}
