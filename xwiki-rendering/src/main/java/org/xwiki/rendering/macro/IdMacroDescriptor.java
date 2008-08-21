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

import org.xwiki.rendering.macro.descriptor.AbstractMacroDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.StringMacroParameterDescriptor;

/**
 * Parse and convert ID macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id$
 * @since 1.6M1
 */
// TODO: Use an I8N service to translate the descriptions in several languages
public class IdMacroDescriptor extends AbstractMacroDescriptor
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Include other pages into the current page.";

    /**
     * The name of the macro parameter "name".
     */
    public static final String PARAM_NAME = "name";

    /**
     * The description of the macro parameter "name".
     */
    private static final String PARAM_NAME_DESC = "Insert a block with a id that can be targeted.";

    /**
     * The default value of the macro parameter "name".
     */
    private static final String PARAM_NAME_DEF = null;

    /**
     * Set the macro parameters class list.
     */
    public IdMacroDescriptor()
    {
        StringMacroParameterDescriptor nameParamClass =
            new StringMacroParameterDescriptor(PARAM_NAME, PARAM_NAME_DESC, PARAM_NAME_DEF);
        nameParamClass.setRequired(true);
        registerParameterDescriptor(nameParamClass);
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
}
