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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.configuration.RenderingConfiguration;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroCategoriesManager;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.parser.Syntax;

/**
 * Default implementation of {@link MacroCategoriesManager}.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultMacroCategoriesManager extends AbstractLogEnabled implements MacroCategoriesManager
{
    /**
     * Used to lookup macro implementations registered as components.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Used to get macro categories defined by the user (if any).
     */
    @Requirement
    private RenderingConfiguration configuration;

    /**
     * Macro manager component used to check the existence of macros.
     */
    @Requirement
    private MacroManager macroManager;

    private interface MacroMatcher
    {
        boolean match(String macroName);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroCategoriesManager#getMacroCategories()
     */
    public Set<String> getMacroCategories() throws MacroLookupException
    {
        Set<String> categories = getMacroNamesByCategory(new MacroMatcher() {
            public boolean match(String macroName)
            {
                return true;
            }
        }).keySet();
        return Collections.unmodifiableSet(categories);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroCategoriesManager#getMacroCategories(org.xwiki.rendering.parser.Syntax) 
     */
    public Set<String> getMacroCategories(final Syntax syntax) throws MacroLookupException
    {
        Set<String> categories = getMacroNamesByCategory(new MacroMatcher() {
            public boolean match(String macroName)
            {
                return macroManager.exists(macroName, syntax);
            }
        }).keySet();
        return Collections.unmodifiableSet(categories);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroCategoriesManager#getMacroNames(String)
     */
    public Set<String> getMacroNames(String category) throws MacroLookupException
    {
        Set<String> macros = getMacroNamesByCategory(new MacroMatcher() {
            public boolean match(String macroName)
            {
                return true;
            }
        }).get(category);
        return (null != macros) ? Collections.unmodifiableSet(macros) : Collections.<String>emptySet();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.MacroCategoriesManager#getMacroNames(String, org.xwiki.rendering.parser.Syntax)   
     */
    public Set<String> getMacroNames(String category, final Syntax syntax) throws MacroLookupException
    {
        Set<String> macros = getMacroNamesByCategory(new MacroMatcher() {
            public boolean match(String macroName)
            {
                // We have to consider macros registered for the given syntax as well as macros registered for all
                // syntaxes.
                return (macroManager.exists(macroName, syntax) || macroManager.exists(macroName));
            }
        }).get(category);
        return (null != macros) ? Collections.unmodifiableSet(macros) : Collections.<String>emptySet();
    }

    /**
     * @param matcher a macro name matcher to be able to filter macros, used to filter macros for a given syntax
     * @return macro names grouped by category, including the 'null' macro category.
     */
    private Map<String, Set<String>> getMacroNamesByCategory(MacroMatcher matcher) throws MacroLookupException
    {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();

        // Lookup all registered macros
        Map<String, Macro> allMacros;
        try {
            allMacros = this.componentManager.lookupMap(Macro.class);
        } catch (ComponentLookupException e) {
            throw new MacroLookupException("Failed to lookup Macros", e);
        }

        // Loop through all the macros and categorize them.
        Properties categories = this.configuration.getMacroCategories();
        for (Map.Entry<String, Macro> entry : allMacros.entrySet()) {            
            // Extract macro name.
            String [] hintParts = entry.getKey().split("/");
            String macroName = null;
            if (hintParts.length > 0) {
                macroName = hintParts[0];
            } else {
                // Question: Will we ever reach this code?
                getLogger().warn("Invalid macro hint : [" + entry.getKey() + "]");
                // Skip this macro.
                continue;
            }            
            
            // Build category map.
            if (matcher.match(macroName)) {
                // Check if this macro's category has been overwritten.
                String category = categories.getProperty(entry.getKey());

                // If not, use the default category set by macro author.
                category = (null == category) ? entry.getValue().getDescriptor().getDefaultCategory() : category;

                // Add to category. Note the category can also be null.
                Set<String> macroNames = result.get(category);
                if (null == macroNames) {
                    macroNames = new HashSet<String>();
                }
                macroNames.add(macroName);
                result.put(category, macroNames);
            }
        }

        return result;
    }
}
