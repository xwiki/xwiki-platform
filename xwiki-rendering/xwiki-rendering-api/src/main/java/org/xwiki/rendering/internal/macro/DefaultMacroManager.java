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
import java.util.Set;
import java.util.TreeSet;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
public class DefaultMacroManager extends AbstractLogEnabled implements MacroManager
{
    /**
     * Allows transforming a syntax specified as text into a {@link Syntax} object. Injected by the component manager
     * subsystem.
     */
    @Requirement
    private SyntaxFactory syntaxFactory;

    /**
     * The component manager we use to lookup macro implementations registered as components.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacroNames(Syntax)
     */
    @SuppressWarnings("unchecked")
    public Set<String> getMacroNames(Syntax syntax) throws MacroLookupException
    {
        Set<String> result = new TreeSet<String>();

        // Lookup all registered macros
        Map<String, Macro> allMacros;
        try {
            allMacros = this.componentManager.lookupMap(Macro.class);
        } catch (ComponentLookupException e) {
            throw new MacroLookupException("Failed to lookup Macros", e);
        }

        // Loop through all the macros and filter those macros that will work with the given syntax.
        for (Map.Entry<String, Macro> entry : allMacros.entrySet()) {
            // Verify if we have a syntax specified.
            String[] hintParts = entry.getKey().split("/");
            if (hintParts.length == 3) {
                // We've found a macro registered for a given syntax
                String syntaxAsString = hintParts[1] + "/" + hintParts[2];
                String macroName = hintParts[0];
                Syntax macroSyntax;
                try {
                    macroSyntax = this.syntaxFactory.createSyntaxFromIdString(syntaxAsString);
                } catch (ParseException e) {
                    throw new MacroLookupException("Failed to initialize Macro [" + macroName
                        + "] due to an invalid Syntax [" + syntaxAsString + "]", e);
                }
                if (macroSyntax.equals(syntax)) {
                    result.add(macroName);
                }
            } else if (hintParts.length == 1) {
                // We've found a macro registered for all syntaxes
                result.add(hintParts[0]);
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

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacro(java.lang.String, org.xwiki.rendering.parser.Syntax)
     */
    public Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroLookupException
    {
        // First search for a macro registered for the given syntax.
        String macroHint = macroName + "/" + syntax.toIdString();
        try {
            return componentManager.lookup(Macro.class, macroHint);
        } catch (ComponentLookupException ex1) {
            // Now search for a macro registered for all syntaxes.
            try {
                return componentManager.lookup(Macro.class, macroName);
            } catch (ComponentLookupException ex2) {
                throw new MacroLookupException(String.format("No macro named [%s] is available for syntax [%s].",
                    macroName, syntax.toIdString()));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacro(java.lang.String)
     */
    public Macro< ? > getMacro(String macroName) throws MacroLookupException
    {
        try {
            return componentManager.lookup(Macro.class, macroName);
        } catch (ComponentLookupException ex) {
            throw new MacroLookupException(String.format("No macro named [%s] can be found.", macroName));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#exists(java.lang.String, org.xwiki.rendering.parser.Syntax)
     */
    public boolean exists(String macroName, Syntax syntax)
    {
        String macroHint = macroName + "/" + syntax.toIdString();
        boolean hasMacro = true;
        try {
            componentManager.lookup(Macro.class, macroHint);
        } catch (ComponentLookupException ex) {
            hasMacro = false;
        }
        return hasMacro;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#exists(java.lang.String)
     */
    public boolean exists(String macroName)
    {
        boolean hasMacro = true;
        try {
            componentManager.lookup(Macro.class, macroName);
        } catch (ComponentLookupException ex) {
            hasMacro = false;
        }
        return hasMacro;
    }
}
