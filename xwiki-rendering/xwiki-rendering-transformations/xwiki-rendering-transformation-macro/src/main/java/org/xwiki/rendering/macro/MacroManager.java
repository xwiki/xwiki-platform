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
import org.xwiki.rendering.syntax.Syntax;

/**
 * Allow retrieving and test the existence of macros. Macros can be available for all syntaxes or only available for
 * a given syntax.
 * 
 * <p>
 * Indeed, a macro can be registered and thus made available for all syntaxes or only available for a given syntax.
 * The latter is useful for example if we want to support copy pasting wiki content from another wiki and we want to
 * support transparently the macros defined in that content; in this case we could implement these macros only for that
 * syntax and in the implementation make the bridge with XWiki macros for example.
 * </p> 
 *
 * @version $Id$
 * @since 1.9M1
 */
@ComponentRole
public interface MacroManager
{
    /**
     * @return all the available macros whether they are registered for a given syntax or for all syntaxes
     * @throws MacroLookupException error when lookup macros
     * @since 2.03M
     */
    Set<MacroId> getMacroIds() throws MacroLookupException;

    /**
     * @param syntax the desired syntax
     * @return the available macro ids for the desired syntax (this includes macros registered for all syntaxes and
     *         macros registered only for a given syntax)
     * @throws MacroLookupException error when lookup macros
     * @since 2.03M
     */
    Set<MacroId> getMacroIds(Syntax syntax) throws MacroLookupException;

    /**
     * @param macroId the id of the macro to lookup
     * @return the macro, looked-up first as a macro for the desired syntax identifier (if any is specified in the
     *         MacroId) and then as a macro registered for all syntaxes if not found
     * @throws MacroLookupException when no macro with such id was found in both the list of macro for the specified
     *         syntax identifier and for all syntaxes
     * @since 2.03M
     */
    Macro< ? > getMacro(MacroId macroId) throws MacroLookupException;

    /**
     * @param macroId the id of the macro to lookup
     * @return true if a macro with the given id and for the given syntax can be found (if any is specified in the
     *         MacroId), false otherwise. Returns false if a macro with the given id exists but has been registered
     *         only for all syntaxes
     * @since 2.03M
     */
    boolean exists(MacroId macroId);
}
