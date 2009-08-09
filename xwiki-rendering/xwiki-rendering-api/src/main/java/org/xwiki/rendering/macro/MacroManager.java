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
 * Allow retrieving all macros or all macros registered for a given syntax only. Indeed, a macro can be registered
 * and thus made available for all syntaxes or only available for a given syntax. The latter is useful for example
 * if we want to support copy pasting wiki content from another wiki and we want to support transparently the macros
 * defined in that content; in this case we could implement these macros only for that syntax and in the implementation
 * make the bridge with XWiki macros for example. 
 *
 * 
 * @version $Id$
 * @since 1.9M1
 */
@ComponentRole
public interface MacroManager
{
    /**
     * @param syntax the desired syntax
     * @return the available macros for the desired syntax (this includes macros registered for all syntaces +
     *         macros registered only for a given syntax)
     * @throws MacroLookupException error when lookup macros
     */
    Set<String> getMacroNames(Syntax syntax) throws MacroLookupException;

    /**
     * @param macroName the name of the macro to look up (eg "toc" for the TOC Macro)
     * @param syntax the syntax of the macro to look up
     * @return the macro, looked-up first as a macro for the desired syntax identifier, and then as a macro
     *         registered for all syntaxes if not found
     * @throws MacroLookupException when no macro with such name was found in both the list of macro for the specified
     *             syntax identifier and for all syntaxes
     */
    Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroLookupException;

    /**
     * @param macroName the name of the macro to look up (eg "toc" for the TOC Macro)
     * @return the macro if found, looked-up in the list of macro registered for all syntaxes
     * @throws MacroLookupException when no macro with such name was found in the list of macro for all syntaxes
     */
    Macro< ? > getMacro(String macroName) throws MacroLookupException;

    /**
     * @param macroName the name of the macro (eg "toc" for the TOC Macro)
     * @param syntax the syntax of the macro
     * @return true if a macro with the given name and for the given syntax can be found, false otherwise. Returns
     *         false if a macro with the given name exists but has been registered only for all syntaxes
     */
    boolean exists(String macroName, Syntax syntax);

    /**
     * @param macroName the name of the macro (eg "toc" for the TOC Macro)
     * @return true if a macro with given name and registered for all syntaxes can be found, false otherwise. Returns
     *         true if a macro with the given name has been registered only for a given syntax
     */
    boolean exists(String macroName);
}
