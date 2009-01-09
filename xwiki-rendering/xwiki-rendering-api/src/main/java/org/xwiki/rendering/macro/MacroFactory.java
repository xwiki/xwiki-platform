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

import org.xwiki.rendering.parser.Syntax;

/**
 * @version $Id$
 * @since 1.5M2
 */
public interface MacroFactory
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = MacroFactory.class.getName();

    /**
     * Look up for a Macro component matching the macro name passed as a parameter + the syntax type (eg "xwiki",
     * "confluence", etc). The format is <code>macroname-syntaxtype</code>. The reason for this is that different
     * syntax types can have different Macros (eg the XWiki {html} macro vs the Confluence {html} macro).
     * 
     * @param macroName the macro name for the macro to return
     * @param syntax the syntax for which to find the corresponding macro
     * @return the matching macro or null if not found
     * @throws MacroNotFoundException if the macro cannot be found
     */
    Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroNotFoundException;
}
