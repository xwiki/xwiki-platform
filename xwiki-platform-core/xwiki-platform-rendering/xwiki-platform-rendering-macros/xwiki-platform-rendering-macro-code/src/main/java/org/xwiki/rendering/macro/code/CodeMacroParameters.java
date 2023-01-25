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
package org.xwiki.rendering.macro.code;

import org.xwiki.properties.annotation.PropertyAdvanced;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.rendering.macro.box.BoxMacroParameters;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;

/**
 * Parameters for the {@link org.xwiki.rendering.internal.macro.code.CodeMacro} Macro.
 * 
 * @version $Id$
 * @since 1.7RC1
 */
public class CodeMacroParameters extends BoxMacroParameters
{
    /**
     * The language identifier.
     */
    private String language;

    /**
     * The layout format (e.g. plain or with line numbers)
     */
    private CodeMacroLayout layout = CodeMacroLayout.PLAIN;

    private MacroContentSourceReference source;

    /**
     * @param language the language identifier.
     */
    @PropertyDescription("the language identifier (java, python, etc.)")
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the language identifier.
     */
    public String getLanguage()
    {
        return this.language;
    }

    /**
     * @param layout the layout to apply to the code.
     * @since 11.5RC1
     */
    @PropertyDescription("the layout format (plain or with line numbers)")
    public void setLayout(CodeMacroLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return the layout to apply to the code.
     * @since 11.5RC1
     */
    public CodeMacroLayout getLayout()
    {
        return layout;
    }

    /**
     * @param source the reference of the content to highlight
     * @since 15.1RC1
     * @since 14.10.5
     */
    @PropertyDescription("the reference of the content to highlight instead of the content of the macro"
        + " (script:myvariable for the entry with name myvariable in the script context)")
    @PropertyAdvanced
    public void setSource(MacroContentSourceReference source)
    {
        this.source = source;
    }

    /**
     * @return the reference of the content to highlight
     * @since 15.1RC1
     * @since 14.10.5
     */
    public MacroContentSourceReference getSource()
    {
        return this.source;
    }
}
