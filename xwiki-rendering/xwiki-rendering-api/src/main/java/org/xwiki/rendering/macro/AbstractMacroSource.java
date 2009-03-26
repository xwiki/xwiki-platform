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

import org.xwiki.component.logging.AbstractLogEnabled;

import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxFactory;

/**
 * Abstract source of macros. Specific macro managers (implementing {@link MacroSource}) can extend this class to
 * benefit from the logic that deals with macros for all syntaxes versus macro for a specific syntax. Implementations that
 * extend this class are responsible to initialize and keep up to date the two maps {@link #allSyntaxesMacros and
 * {@link #syntaxSpecificMacros}.
 * 
 * @since 1.9M1
 */
public abstract class AbstractMacroSource extends AbstractLogEnabled implements MacroSource
{

    /**
     * Allows transforming a syntax specified as text into a {@link Syntax} object. Injected by the component manager
     * subsystem.
     */
    protected SyntaxFactory syntaxFactory;

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
     * Configured by the component manager.
     */
    protected int priority = 100;

    public AbstractMacroSource()
    {
        // Create macro caches.
        this.syntaxSpecificMacros = new HashMap<Syntax, Map<String, Macro< ? >>>();
        this.allSyntaxesMacros = new HashMap<String, Macro< ? >>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroSource#getAllMacros(Syntax)
     */
    public Map<String, Macro< ? >> getAllMacros(Syntax syntax)
    {
        Map<String, Macro< ? >> result = new HashMap<String, Macro< ? >>();

        // first we put the macros that are not specific to any syntax.
        result.putAll(this.allSyntaxesMacros);

        // then we add macros for this syntax in particular if any.
        // if macro with same name is defined for both, the one specific to the desired syntax wins.
        if (this.syntaxSpecificMacros.containsKey(syntax)) {
            result.putAll(this.syntaxSpecificMacros.get(syntax));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroSource#getMacro(String, Syntax)
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
     * @see MacroSource#getMacro(String)
     */
    public Macro< ? > getMacro(String macroName) throws MacroLookupException
    {
        if (this.allSyntaxesMacros.containsKey(macroName)) {
            return this.allSyntaxesMacros.get(macroName);
        }
        throw new MacroLookupException("No [" + macroName + "] could be found");
    }

    /**
     * Register
     * 
     * @param macroName
     * @param syntaxAsString
     * @param macro
     * @throws InitializationException
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

    protected void registerMacroForAllSyntaxes(String macroName, Macro< ? > macro)
    {
        this.allSyntaxesMacros.put(macroName, macro);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MacroSource#getPriority()
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

    /**
     * {@inheritDoc}
     * 
     * @see MacroManager#exists(String, Syntax)
     */
    public boolean exists(String macroName, Syntax syntax)
    {
        if (this.syntaxSpecificMacros.get(syntax) != null
            && this.syntaxSpecificMacros.get(syntax).get(macroName) != null) {
            return true;
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
        if (this.allSyntaxesMacros.get(macroName) != null) {
            return true;
        }
        return false;
    }

}
