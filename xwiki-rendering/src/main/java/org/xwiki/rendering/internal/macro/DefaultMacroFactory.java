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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxFactory;
import org.xwiki.rendering.macro.MacroFactory;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroNotFoundException;

/**
 * Handles Macros in the system: finds all registered macros, cache them and provide an API to get them.
 * Note that macros can be registered for all syntaxes or only for a specific syntax. Macros registered
 * for a given syntax take priority over ones registered for all syntaxes.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultMacroFactory extends AbstractLogEnabled implements MacroFactory, Composable, Initializable
{
    /**
     * The component manager that we use to retrieve other components.
     */
    private ComponentManager componentManager;

    /**
     * Allows transforming a syntax specified as text into a {@link Syntax} object.
     * Injected by the component manager subsystem.
     */
    private SyntaxFactory syntaxFactory;
    
    /**
     * Cache of macros for syntax-specific macros. Index is the syntax and value is a Map with an index being the 
     * macro name the value the Macro.
     */
    private Map<Syntax, Map<String, Macro< ? >>> syntaxSpecificMacros;

    /**
     * Cache of macros for macros registered for all syntaxes. Index is the syntax and value is a Map with an index
     * being the macro name the value the Macro.
     */
    private Map<String, Macro< ? >> allSyntaxesMacros;

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Create macro caches.
        this.syntaxSpecificMacros = new HashMap<Syntax, Map<String, Macro< ? >>>();
        this.allSyntaxesMacros = new HashMap<String, Macro< ? >>();

        // Find all registered macros
        Map<String, Macro< ? >> allMacros;
        try {
            allMacros = this.componentManager.lookupMap(Macro.ROLE);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to lookup Macros", e);
        }

        // Now sort through the ones that are registered for a given syntax and those registered for all syntaxes.
        for (Map.Entry<String, Macro< ? >> entry : allMacros.entrySet()) {

            // Verify if we have a syntax specified.
            String[] hintParts = entry.getKey().split("/");
            if (hintParts.length == 3) {
                // We've found a macro registered for a given syntax
                registerMacroForSyntax(hintParts[0], hintParts[1] + "/" + hintParts[2], entry.getValue());
            } else if (hintParts.length == 1) {
                // We've found a macro registered for all syntaxes
                registerMacroForAllSyntaxes(hintParts[0], entry.getValue());
            } else {
                // We ignore invalid macro descriptors but log it as warning.
                getLogger().warn("Invalid Macro descriptor format for hint [" + entry.getKey() 
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
     * @see MacroFactory#getMacro(String, org.xwiki.rendering.parser.Syntax)
     */
    public Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroNotFoundException
    {
        // First look for a syntax-specific macro and if not found look for a macro that's been registered
        // for all syntaxes.
        Macro< ? > macro;
        Map<String, Macro< ? >> macrosForSyntax = this.syntaxSpecificMacros.get(syntax);
        if (macrosForSyntax != null && macrosForSyntax.containsKey(macroName)) {
            macro = macrosForSyntax.get(macroName);
        } else {
            // Look for a macro registered for all syntaxes
            macro = this.allSyntaxesMacros.get(macroName);
            if (macro == null) {
                throw new MacroNotFoundException("No [" + macroName + "] macro has been registered for syntax [" 
                    + syntax.toIdString() + "]");
            }
        }

        return macro;
    }
    
    private void registerMacroForSyntax(String macroName, String syntaxAsString, Macro< ? > macro)
        throws InitializationException
    {
        Syntax syntax;
        try { 
            syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxAsString);
        } catch (ParseException e) {
            throw new InitializationException("Failed to initialize Macro [" + macroName 
                + "] due to an invalid Syntax [" + syntaxAsString + "]", e);
        }
        
        Map<String, Macro< ? >> macrosForSyntax = this.syntaxSpecificMacros.get(syntax);
        if (macrosForSyntax == null) {
            macrosForSyntax = new HashMap<String, Macro< ? >>();
            this.syntaxSpecificMacros.put(syntax, macrosForSyntax);
        }
        macrosForSyntax.put(macroName, macro);
    }
    
    private void registerMacroForAllSyntaxes(String macroName, Macro< ? > macro)
    {
        this.allSyntaxesMacros.put(macroName, macro);
    }
}
