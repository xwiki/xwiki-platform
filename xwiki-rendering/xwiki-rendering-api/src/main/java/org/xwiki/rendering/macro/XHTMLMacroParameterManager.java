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

import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameterManager;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.MacroParameterManager;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * Parse and convert XHTML macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id: $
 * @since 1.6M1
 */
// TODO: Use an I8N service to translate the descriptions in several languages
public class XHTMLMacroParameterManager
{
    /**
     * The name of the macro parameter "escapeWikiSyntax".
     */
    private static final String PARAM_ESCAPEWIKISYNTAX = "escapeWikiSyntax";

    /**
     * The description of the macro parameter "escapeWikiSyntax".
     */
    private static final String PARAM_ESCAPEWIKISYNTAX_DESC =
        "If true then the XHTML element contents won't be rendered even if they contain valid wiki syntax.";

    /**
     * The default value of the macro parameter "escapeWikiSyntax".
     */
    private static final boolean PARAM_ESCAPEWIKISYNTAX_DEF = false;

    /**
     * The macro parameters manager. Parse and transform string value to java objects.
     */
    private MacroParameterManager macroParameterManager = new DefaultMacroParameterManager();

    /**
     * Set the macro parameters class list.
     */
    public XHTMLMacroParameterManager()
    {
        BooleanMacroParameterDescriptor escapeWikiSyntaxParamClass =
            new BooleanMacroParameterDescriptor(PARAM_ESCAPEWIKISYNTAX, PARAM_ESCAPEWIKISYNTAX_DESC,
                PARAM_ESCAPEWIKISYNTAX_DEF);
        escapeWikiSyntaxParamClass.setValueHasToBeValid(false);
        this.macroParameterManager.registerParameterDescriptor(escapeWikiSyntaxParamClass);
    }

    /**
     * @return the list of parameters descriptors.
     */
    public Map<String, MacroParameterDescriptor< ? >> getParametersDescriptorMap()
    {
        return this.macroParameterManager.getParametersDescriptorMap();
    }

    /**
     * @param parameters load parameters from parser as parameters objects list.
     */
    public void load(Map<String, String> parameters)
    {
        this.macroParameterManager.load(parameters);
    }

    // /////////////////////////////////////////////////////////////////////
    // Parameters

    /**
     * @return indicate if the user has asked to escape wiki syntax or not.
     * @exception MacroParameterException error when converting value.
     */
    public boolean isWikiSyntaxEscaped() throws MacroParameterException
    {
        return this.macroParameterManager.<Boolean> getParameterValue(PARAM_ESCAPEWIKISYNTAX);
    }
}
