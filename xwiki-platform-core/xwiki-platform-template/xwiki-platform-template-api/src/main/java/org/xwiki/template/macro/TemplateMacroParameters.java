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
package org.xwiki.template.macro;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.stability.Unstable;

/**
 * Parameters for the {@link org.xwiki.template.internal.macro.TemplateMacro} Macro.
 *
 * @version $Id$
 * @since 7.0M1
 */
@Unstable
public class TemplateMacroParameters
{
    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #setOutput(boolean)
     */
    private boolean output = true;

    /**
     * @param name the name of the template
     */
    @PropertyDescription("the name of the template")
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the name of the template
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return specify whether or not the result of the execution should be returned the macro.
     */
    @PropertyDescription("Specifies whether or not the result of the execution should be returned the macro.")
    public boolean isOutput()
    {
        return this.output;
    }

    /**
     * @param output specify whether or not the result of the execution should be returned the macro.
     */
    public void setOutput(boolean output)
    {
        this.output = output;
    }
}
