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
 * Component interface for managing macro category information. Each rendering macro defines a default category under
 * which it falls, but this category may be overwritten by xwiki configuration mechanism. This component will handle
 * such overwriting operations and make sure latest macro category information is presented to client code.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface MacroCategoryManager
{
    /**
     * Returns all the macro categories currently available in the system. Macros that don't have default or overridden
     * categories are not included and thus clients should be aware that there can be macros in the system which do
     * not belong to any category.
     * 
     * @return the macro categories available in the system.
     * @throws MacroLookupException error when looking up macros
     */
    Set<String> getMacroCategories() throws MacroLookupException;

    /**
     * Returns all the macro categories currently available in the system for macros registered for a given syntax
     * and for all syntaxes. Macros that don't have default or overridden categories are not included and thus clients
     * should be aware that there can be macros in the system which do not belong to any category.
     *
     * @param syntax the syntax to filter the macros by syntax.
     * @return the macro categories available for the given syntax
     * @throws MacroLookupException error when looking up macros
     */
    Set<String> getMacroCategories(Syntax syntax) throws MacroLookupException;

    /**
     * @param category name of the category or null.
     * @return ids of all the macros belonging to the given category or if the category parameter is null, ids of
     *         all the macros which do not belong to any category.
     * @throws MacroLookupException error when lookup macros
     */
    Set<MacroId> getMacroIds(String category) throws MacroLookupException;

    /**
     * @param category name of the category or null.
     * @param syntax the syntax to filter the macros by syntax.
     * @return ids of all the macros belonging to the given category (and registered for the given syntax) or if the
     *         category parameter is null, ids of all the macros which do not belong to any category (and registered
     *         for the given syntax).
     * @throws MacroLookupException error when lookup macros
     */
    Set<MacroId> getMacroIds(String category, Syntax syntax) throws MacroLookupException;
}
