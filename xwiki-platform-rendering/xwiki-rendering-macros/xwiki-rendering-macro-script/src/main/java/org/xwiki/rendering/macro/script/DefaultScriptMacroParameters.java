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
package org.xwiki.rendering.macro.script;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the Script Macro.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class DefaultScriptMacroParameters extends JSR223ScriptMacroParameters
{
    /**
     * The identifier of the script language.
     */
    private String language;

    /**
     * @param language the identifier of the script language.
     */
    @PropertyDescription("The identifier of the script language (\"groovy\", \"python\", etc)")
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return the identifier of the script language.
     */
    public String getLanguage()
    {
        return this.language;
    }
}
