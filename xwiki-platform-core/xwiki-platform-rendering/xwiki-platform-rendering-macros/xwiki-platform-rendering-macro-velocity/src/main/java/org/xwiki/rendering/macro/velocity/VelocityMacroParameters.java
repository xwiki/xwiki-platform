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
package org.xwiki.rendering.macro.velocity;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;

/**
 * Parameters for the velocity macro.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class VelocityMacroParameters extends ScriptMacroParameters
{
    /**
     * Indicate the output result has to be inserted back in the document.
     * @since 2.0M1
     */
    private String filter;

    /**
     * @param filter indicate which filtering mode to use.
     * @since 2.0M1
     */
    @PropertyDescription("indicate which filtering mode to use")
    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    /**
     * @return indicate which filtering mode to use
     * @since 2.0M1
     */
    public String getFilter()
    {
        return filter;
    }
}
