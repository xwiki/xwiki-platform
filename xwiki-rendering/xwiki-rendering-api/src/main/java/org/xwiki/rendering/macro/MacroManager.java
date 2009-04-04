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
package org.xwiki.rendering.macro;

import java.util.Set;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.parser.Syntax;

/**
 * Main interface to lookup {@link org.xwiki.rendering.macro.Macro} implementations available for the rendering at
 * run-time. Rendering macros are not necessarily registered as components, thus code that needs to look up a certain
 * macro, or list all available macros should always go through such a manager.
 * 
 * @version $Id$
 * @since 1.9M1
 * @see also {@link MacroSource}, a tag interface that extends MacroManager for specific macro managers that can be
 *      aggregated.
 */
@ComponentRole
public interface MacroManager extends Comparable<MacroManager>
{
    /**
     * @param syntax the desired syntax
     * @return a set of all names of available macros for the desired syntax.
     * @throws MacroLookupException error when lookup macros
     */
    Set<String> getMacroNames(Syntax syntax) throws MacroLookupException;

    /**
     * @param macroName the name of the macro looked up
     * @param syntax the syntax of the macro looked up
     * @return the macro if found, looked-up first as macro for the desired syntax identifier, and then in macro
     *         registered for all syntaxes if not found.
     * @throws MacroLookupException when no macro with such name was found in both the list of macro for the specified
     *             syntax identifier and in for all syntaxes.
     */
    Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroLookupException;

    /**
     * @param macroName the name of the macro to lookup
     * @return the macro if found, looked-up in the list of macro registered for all syntaxes.
     * @throws MacroLookupException when no macro with such name was found in the list of macro for all syntaxes.
     */
    Macro< ? > getMacro(String macroName) throws MacroLookupException;

    /**
     * @param macroName the name of the macro
     * @param syntax the syntax of the macro
     * @return true if the manager can return a macro with given name for the given syntax, false otherwise
     */
    boolean exists(String macroName, Syntax syntax);

    /**
     * @param macroName the name of the macro
     * @return true if the manager can return a macro with given name for all syntaxes, false otherwise
     */
    boolean exists(String macroName);

    /**
     * @return the priority of this manager. Used to sort manager when macros are defined in several.
     */
    int getPriority();
}
