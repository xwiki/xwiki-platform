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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroSource;
import org.xwiki.rendering.parser.Syntax;

/**
 * The default macro manager is an aggregator of {@link MacroSource} specific macro managers.
 * Gets its sources of macros injected by the component engine.
 */
@Component
public class DefaultMacroManager extends AbstractLogEnabled implements MacroManager, Initializable
{
    /**
     * List of macro sources, ie locations where macros can be found. 
     */
    @Requirement(role = MacroSource.class)
    private List<MacroSource> macroSources;

    private int priority = 10;
    
    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        Collections.sort(macroSources);
    }

    /**
     * @param sources the list of {@link MacroSource} this manager should aggregates. Convenience method
     * to be used when this macro manager isn't used in a componentized context
     */
    public void setMacroSources(List<MacroSource> sources) 
    {
        this.macroSources = sources;
        Collections.sort(this.macroSources);
    }    
    
    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#getMacroNames(Syntax)
     */
    public Set<String> getMacroNames(Syntax syntax) throws MacroLookupException
    {
        Set<String> result = new TreeSet<String>();

        List<MacroSource> reversedList = new ArrayList<MacroSource>();
        reversedList.addAll(macroSources);
        Collections.sort(reversedList, Collections.reverseOrder());

        for (MacroSource provider : reversedList) {
            result.addAll(provider.getMacroNames(syntax));
        }

        if (result.isEmpty()) {
            throw new MacroLookupException("Could not find any macro for syntax [" + syntax.toIdString() + "]");
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#getMacro(String, Syntax)
     */
    public Macro< ? > getMacro(String macroName, Syntax syntax) throws MacroLookupException
    {
        // First, we look over all iterate over the ordered providers to try and find a macro
        // with such name registered for the define syntax
        for (MacroSource provider : macroSources) {
            if (provider.exists(macroName, syntax)) {
                return provider.getMacro(macroName, syntax);
            }
        }
        // If no provider gave us such macro, we iterate again over the ordered list of registered providers
        // to see if one has it as a macro registered for all syntaxes.
        return this.getMacro(macroName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#getMacro(String)
     */
    public Macro< ? > getMacro(String macroName) throws MacroLookupException
    {
        for (MacroSource provider : macroSources) {
            if (provider.exists(macroName)) {
                return provider.getMacro(macroName);
            }
        }
        // None of the registered providers found it, let's accept our failure.
        throw new MacroLookupException("No [" + macroName + "] macro has been registered");
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#exists(String, Syntax)
     */
    public boolean exists(String macroName, Syntax syntax)
    {
        for (MacroSource provider : macroSources) {
            if (provider.exists(macroName, syntax)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#exists(String)
     */
    public boolean exists(String macroName)
    {
        for (MacroSource provider : macroSources) {
            if (provider.exists(macroName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#getPriority()
     */
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(MacroManager manager)
    {
        if (getPriority() != manager.getPriority()) {
            return getPriority() - manager.getPriority();
        }
        return this.getClass().getSimpleName().compareTo(manager.getClass().getSimpleName());
    }

}
