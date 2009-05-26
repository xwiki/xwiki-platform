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

import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.AbstractMacroSource;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroSource;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Syntax;

/**
 * Default {@link MacroSource} implementation. 
 * This specific macro manager retrieves all {@link Macro} implementations that are registered
 * against XWiki's component manager.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@Component
public class RealmMacroSource extends AbstractMacroSource implements Initializable, Composable
{
    /**
     * The component manager we use to lookup macro implementations registered as components.
     */
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        registerMacros();
        
        // Set a lower priority for this source than the default to ensure other sources such as
        // a source that would take its macro from wiki pages would run first.
        this.priority = 10;
    }

    /**
     * Register all macros (macros for all syntaxes, macros for a given syntax).
     * 
     * @throws InitializationException in case of an error when registering a macro
     */
    private void registerMacros() throws InitializationException
    {
        // Find all registered macros
        Map<String, Macro> allMacros;
        try {
            allMacros = this.componentManager.lookupMap(Macro.class);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup Macros", e);
        }

        // Now sort through the ones that are registered for a given syntax and those registered for all syntaxes.
        for (Map.Entry<String, Macro> entry : allMacros.entrySet()) {

            // Verify if we have a syntax specified.
            String[] hintParts = entry.getKey().split("/");
            if (hintParts.length == 3) {
                // We've found a macro registered for a given syntax
                String syntaxAsString = hintParts[1] + "/" + hintParts[2];
                String macroName = hintParts[0];
                Syntax syntax;
                try {
                    syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxAsString);
                } catch (ParseException e) {
                    throw new InitializationException("Failed to initialize Macro [" + macroName
                        + "] due to an invalid Syntax [" + syntaxAsString + "]", e);
                }
                registerMacroForSyntax(macroName, syntax, entry.getValue());
            } else if (hintParts.length == 1) {
                // We've found a macro registered for all syntaxes
                registerMacroForAllSyntaxes(hintParts[0], entry.getValue());
            } else {
                // We ignore invalid macro descriptors but log it as warning.
                getLogger()
                    .warn(
                        "Invalid Macro descriptor format for hint ["
                            + entry.getKey()
                            + "]. The hint should contain either the macro name only or the macro name followed by "
                            + "the syntax for which it is valid. In that case the macro name should be followed by a "
                            + "\"/\" followed by the syntax name followed by another \"/\" followed by the syntax version. "
                            + "This macro will not be available in the system.");
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

}
