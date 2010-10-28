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
import java.util.HashSet;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroIdFactory;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;

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
     * Allows transforming a macro id specified as text into a {@link MacroId} object.
     */
    @Requirement
    private MacroIdFactory macroIdFactory;

    /**
     * The Root Component Manager we use find the Context Component Manager (if it exists) to lookup macro
     * implementations registered as components. Note that Context Component Manager allows Macros to be 
     * registered for a specific user, for a specific wiki, etc. 
     */
    @Requirement
    private ComponentManager rootComponentManager;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroManager#getMacroIds()
     */
    public Set<MacroId> getMacroIds() throws MacroLookupException
    {
        return getMacroIds(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacroIds(Syntax)
     */
    public Set<MacroId> getMacroIds(Syntax syntax) throws MacroLookupException
    {
        Set<MacroId> result = new HashSet<MacroId>();

        // Lookup all registered macros
        Map<String, Macro> allMacros;
        try {
            allMacros = getComponentManager().lookupMap(Macro.class);
        } catch (ComponentLookupException e) {
            throw new MacroLookupException("Failed to lookup Macros", e);
        }

        // Loop through all the macros and filter those macros that will work with the given syntax.
        for (Map.Entry<String, Macro> entry : allMacros.entrySet()) {
            MacroId macroId;
            try {
                macroId = this.macroIdFactory.createMacroId(entry.getKey());
            } catch (ParseException e) {
                // One of the macros is registered against the component manager with an invalid macro id, ignore it
                // but log a warning.
                getLogger().warn("Invalid Macro descriptor format for hint [" + entry.getKey()
                    + "]. The hint should contain either the macro name only or the macro name followed by "
                    + "the syntax for which it is valid. In that case the macro name should be followed by a "
                    + "\"/\" followed by the syntax name followed by another \"/\" followed by the syntax version. "
                    + "For example \"html/xwiki/2.0\". This macro will not be available in the system.");
                continue;
            }
            if (syntax == null || macroId.getSyntax() == null || syntax == macroId.getSyntax()) {
                result.add(macroId);
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#getMacro(org.xwiki.rendering.macro.MacroId) 
     */
    public Macro< ? > getMacro(MacroId macroId) throws MacroLookupException
    {
        // First search for a macro registered for the passed macro id.
        String macroHint = macroId.toString();
        try {
            return getComponentManager().lookup(Macro.class, macroHint);
        } catch (ComponentLookupException ex1) {
            // Now search explicitly for a macro registered for all syntaxes.
            try {
                return getComponentManager().lookup(Macro.class, macroId.getId());
            } catch (ComponentLookupException ex2) {
                // TODO: Improve this since it's possible the macro wasn't found because it contains some invalid
                // requirement and since we're not passing the raised exception it's hard to know why the macro
                // couldn't be found.
                throw new MacroLookupException(String.format("No macro [%s] could be found.", macroId.toString()));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.MacroManager#exists(org.xwiki.rendering.macro.MacroId)
     */
    public boolean exists(MacroId macroId)
    {
        String macroHint = macroId.toString();
        boolean hasMacro = true;
        try {
            getComponentManager().lookup(Macro.class, macroHint);
        } catch (ComponentLookupException ex) {
            hasMacro = false;
        }
        return hasMacro;
    }

    /**
     * @return the Component Manager to use to lookup Macros. If the Context Component Manager is available
     *         we use it thus allowing Macros to be registered only for a given Wiki or for a given User (for example).
     * @since 2.2M1
     */
    private ComponentManager getComponentManager()
    {
        ComponentManager componentManagerToUse;
        
        // Look for the Context Component Manager so that Macros can be registered for a specific user, for a
        // specific wiki, etc. If it's not found use the Root Component Manager. This allows the Rendering module
        // to work outside of XWiki when there's no notion of Execution Context and Wiki Model for example.
        try {
            componentManagerToUse = this.rootComponentManager.lookup(ComponentManager.class, "context");
        } catch (ComponentLookupException e) {
            // This means the Context CM doesn't exist, use the Root CM.
            componentManagerToUse = this.rootComponentManager;
        }
        
        return componentManagerToUse;
    }
}
