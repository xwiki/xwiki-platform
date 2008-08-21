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

import org.xwiki.rendering.macro.descriptor.AbstractMacroDescriptor;
import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;

/**
 * Describe the macro.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
// TODO: Use an I8N service to translate the descriptions in several languages
public class XHTMLMacroDescriptor extends AbstractMacroDescriptor<XHTMLMacroDescriptor.Parameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts XHTML code into the page.";

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
     * Set the macro parameters class list.
     */
    public XHTMLMacroDescriptor()
    {
        BooleanMacroParameterDescriptor escapeWikiSyntaxParamDescriptor =
            new BooleanMacroParameterDescriptor(PARAM_ESCAPEWIKISYNTAX, PARAM_ESCAPEWIKISYNTAX_DESC,
                PARAM_ESCAPEWIKISYNTAX_DEF);
        escapeWikiSyntaxParamDescriptor.setValueHasToBeValid(false);
        registerParameterDescriptor(escapeWikiSyntaxParamDescriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.MacroDescriptor#getDescription()
     */
    public String getDescription()
    {
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.AbstractMacroDescriptor#createMacroParameters(java.util.Map)
     */
    @Override
    public XHTMLMacroDescriptor.Parameters createMacroParameters(Map<String, String> parameters)
    {
        return new Parameters(parameters, this);
    }

    // /////////////////////////////////////////////////////////////////////
    // Parameters

    public class Parameters extends DefaultMacroParameters
    {
        public Parameters(Map<String, String> parameters, XHTMLMacroDescriptor macroDescriptor)
        {
            super(parameters, macroDescriptor);
        }

        /**
         * @return indicate if the user has asked to escape wiki syntax or not.
         * @exception MacroParameterException error when converting value.
         */
        public boolean isWikiSyntaxEscaped() throws MacroParameterException
        {
            return this.<Boolean> getParameterValue(PARAM_ESCAPEWIKISYNTAX);
        }
    }
}
