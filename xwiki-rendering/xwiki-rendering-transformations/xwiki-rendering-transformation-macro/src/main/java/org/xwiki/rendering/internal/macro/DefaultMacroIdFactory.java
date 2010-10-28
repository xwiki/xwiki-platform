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
package org.xwiki.rendering.internal.macro;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.parser.ParseException;

/**
 * Default implementation for {@link org.xwiki.rendering.macro.MacroIdFactory}.
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultMacroIdFactory implements MacroIdFactory
{
    /**
     * Error message when the macro id format is invalid.
     */
    private static final String INVALID_MACRO_ID_FORMAT = "Invalid macro id format [%s]";

    /**
     * For creating Syntax objects when creating MacroId from a string representing the syntax id and the
     * syntax version.
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroIdFactory#createMacroId(String)
     */
    public MacroId createMacroId(String macroIdAsString) throws ParseException
    {
        MacroId macroId;

        // Verify if we have a syntax specified.
        String[] hintParts = macroIdAsString.split("/");
        if (hintParts.length == 3) {
            // We've found a macro id for a macro that should be available only for a given syntax
            Syntax syntax;
            try {
                syntax = this.syntaxFactory.createSyntaxFromIdString(
                    String.format("%s/%s", hintParts[1], hintParts[2]));
            } catch (ParseException e) {
                throw new ParseException(String.format(INVALID_MACRO_ID_FORMAT, macroIdAsString), e);
            }
            macroId = new MacroId(hintParts[0], syntax);
        } else if (hintParts.length == 1) {
            // We've found a macro registered for all syntaxes
            macroId = new MacroId(macroIdAsString);
        } else {
            // Invalid format
            throw new ParseException(String.format(INVALID_MACRO_ID_FORMAT, macroIdAsString));
        }

        return macroId;
    }
}
