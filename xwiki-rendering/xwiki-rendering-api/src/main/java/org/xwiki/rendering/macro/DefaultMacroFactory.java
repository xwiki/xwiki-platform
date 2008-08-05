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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.Syntax;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class DefaultMacroFactory implements MacroFactory, Composable, Initializable
{
    private ComponentManager componentManager;

    /**
     * Index is the syntax and value is a Map with an index being the macro name the value the Macro.
     */
    private Map<String, Map<String, Macro>> macros;

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
        // Note that the lifecycle handler ensures that the initialize() method is called
        // after the compose() method.
        this.macros = new HashMap<String, Map<String, Macro>>();

        Map<String, Macro> allMacros;
        try {
            allMacros = this.componentManager.lookupMap(Macro.ROLE);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to construct Macro cache", e);
        }

        for (String hint : allMacros.keySet()) {
            String[] hintParts = hint.split("/");
            // TODO: Add error handling and skip macro if invalid hint format
            String macroName = hintParts[0];
            String syntax = hintParts[1];

            Map<String, Macro> macrosForSyntax = this.macros.get(syntax);
            if (macrosForSyntax == null) {
                macrosForSyntax = new HashMap<String, Macro>();
                this.macros.put(syntax, macrosForSyntax);
            }
            macrosForSyntax.put(macroName, allMacros.get(hint));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroFactory#getMacro(String, org.xwiki.rendering.parser.Syntax)
     */
    public Macro getMacro(String macroName, Syntax syntax) throws MacroNotFoundException
    {
        Macro macro;
        Map<String, Macro> macrosForSyntax = this.macros.get(syntax.getType().toIdString());
        if (macrosForSyntax != null) {
            macro = macrosForSyntax.get(macroName);
            if (macro == null) {
                throw new MacroNotFoundException("Cannot find Macro [" + macroName + "] for syntax ["
                    + syntax.toString() + "]");
            }
        } else {
            throw new MacroNotFoundException("No macro has been registered for syntax [" + syntax.toString() + "]");
        }
        return macro;
    }
}
