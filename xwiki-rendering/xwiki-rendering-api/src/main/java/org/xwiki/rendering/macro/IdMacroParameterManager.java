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
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.StringMacroParameterDescriptor;

/**
 * Parse and convert ID macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id$
 * @since 1.6M1
 */
// TODO: Use an I8N service to translate the descriptions in several languages
public class IdMacroParameterManager
{
    /**
     * The name of the macro parameter "document".
     */
    private static final String PARAM_NAME = "name";

    /**
     * The description of the macro parameter "document".
     */
    private static final String PARAM_NAME_DESC = "Insert a block with a id that can be targeted.";

    /**
     * The default value of the macro parameter "document".
     */
    private static final String PARAM_NAME_DEF = null;

    /**
     * The name of the macro parameter "context".
     */
    private static final String PARAM_CONTEXT = "context";

    /**
     * The macro parameters manager. Parse and transform string value to java objects.
     */
    private MacroParameterManager macroParameterManager = new DefaultMacroParameterManager();

    /**
     * Set the macro parameters class list.
     */
    public IdMacroParameterManager()
    {
        StringMacroParameterDescriptor nameParamClass =
            new StringMacroParameterDescriptor(PARAM_NAME, PARAM_NAME_DESC, PARAM_NAME_DEF);
        nameParamClass.setRequired(true);
        this.macroParameterManager.registerParameterDescriptor(nameParamClass);
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
     * @return the unique name of the id.
     * @exception MacroParameterException error when converting value.
     */
    public String getName() throws MacroParameterException
    {
        return this.macroParameterManager.getParameterValue(PARAM_NAME);
    }
}
