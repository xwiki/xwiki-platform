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
package org.xwiki.rendering.macro.wikibridge;

import java.lang.reflect.Type;

/**
 * Represents a Parameter Type for Wiki Macros.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class WikiMacroParameterType
{
    private String name;

    private String prettyName;

    private Type type;

    /**
     * Default constructor.
     *
     * @param name name of the parameter type
     * @param prettyName pretty name of the parameter type
     * @param type actual type
     */
    public WikiMacroParameterType(String name, String prettyName, Type type)
    {
        this.name = name;
        this.prettyName = prettyName;
        this.type = type;
    }

    /**
     * @return the parameter type name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the parameter type pretty name
     */
    public String getPrettyName()
    {
        return prettyName;
    }

    /**
     * @return the parameter type
     */
    public Type getType()
    {
        return type;
    }
}
