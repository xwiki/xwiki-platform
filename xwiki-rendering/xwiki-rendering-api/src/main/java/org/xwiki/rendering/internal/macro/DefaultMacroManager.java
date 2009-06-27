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
import java.util.Set;
import java.util.TreeSet;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxFactory;

/**
 * Default {@link MacroManager} implementation, retrieves all {@link Macro} implementations that are registered against
 * XWiki's component manager.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@Component
public class DefaultMacroManager extends AbstractLogEnabled implements MacroManager, Initializable
{
    /**
     * Cache of macros for syntax-specific macros. Index is the syntax and value is a Map with an index being the macro
     * name the value the Macro.
     */
    protected Map<Syntax, Map<String, Macro< ? >>> syntaxSpecificMacros;

    /**
     * Cache of macros for macros registered for all syntaxes. Index is the syntax and value is a Map with an index
     * being the macro name the value the Macro.
     */
    protected Map<String, Macro< ? >> allSyntaxesMacros;

    /**
     * Allows transforming a syntax specified as text into a {@link Syntax} object. Injected by the component manager
     * subsystem.
     */
    @Requirement
    protected SyntaxFactory syntaxFactory;

    /**
     * The component manager we use to lookup macro implementations registered as components.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Creates a new {@link DefaultMacroManager} instance.
     */
    public DefaultMacroManager()
    {
        // Create macro caches.
        this.syntaxSpecificMacros = new HashMap<Syntax, Map<String, Macro< ? >>>();
        this.allSyntaxesMacros = new HashMap<String, Macro< ? >>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    @SuppressWarnings("unchecked")
    public void initialize() throws InitializationException
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
     * Register a macro for a specific syntax.
     * 
     * @param macroName the name of the macro to register.
     * @param syntax the syntax for which to register the macro. If null the macro is registered for all syntaxes.
     * @param macro the macro to register
     */
    protected void registerMacroForSyntax(String macroName, Syntax syntax, Macro< ? > macro)
    {
        Map<String, Macro< ? >> macrosForSyntax = this.syntaxSpecificMacros.get(syntax);
        if (macrosForSyntax == null) {
            macrosForSyntax = new HashMap<String, Macro< ? >>();
            this.syntaxSpecificMacros.put(syntax, macrosForSyntax);
        }

        macrosForSyntax.put(macroName, macro);
    }

    /**
     * Register a macro for all syntaxes.
     * 
     * @param macroName the name of the macro to register.
     * @param macro the macro to register
     */
    protected void registerMacroForAllSyntaxes(String macroName, Macro< ? > macro)
    {
        this.allSyntaxesMacros.put(macroName, macro);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacroNames(Syntax)
     */
    public Set<String> getMacroNames(Syntax syntax)
    {
        Set<String> result = new TreeSet<String>();

        // first we put the macros that are not specific to any syntax.
        result.addAll(this.allSyntaxesMacros.keySet());

        // then we add macros for this syntax in particular if any.
        // if macro with same name is defined for both, the one specific to the desired syntax wins.
        if (this.syntaxSpecificMacros.containsKey(syntax)) {
            result.addAll(this.syntaxSpecificMacros.get(syntax).keySet());
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacro(java.lang.String, org.xwiki.rendering.parser.Syntax)
     */
    public Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroLookupException
    {
        // First check in macros registered for all syntaxes
        Map<String, Macro< ? >> macrosForSyntax = this.syntaxSpecificMacros.get(syntax);
        if (macrosForSyntax != null && macrosForSyntax.containsKey(macroName)) {
            return macrosForSyntax.get(macroName);
        }

        // If not found, check in macros for all syntaxes
        return this.getMacro(macroName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacro(java.lang.String)
     */
    public Macro< ? > getMacro(String macroName) throws MacroLookupException
    {
        if (this.allSyntaxesMacros.containsKey(macroName)) {
            return this.allSyntaxesMacros.get(macroName);
        }

        throw new MacroLookupException("No [" + macroName + "] could be found");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#exists(java.lang.String, org.xwiki.rendering.parser.Syntax)
     */
    public boolean exists(String macroName, Syntax syntax)
    {
        return this.syntaxSpecificMacros.get(syntax) != null
            && this.syntaxSpecificMacros.get(syntax).get(macroName) != null;

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#exists(java.lang.String)
     */
    public boolean exists(String macroName)
    {
        return this.allSyntaxesMacros.get(macroName) != null;
    }
}
