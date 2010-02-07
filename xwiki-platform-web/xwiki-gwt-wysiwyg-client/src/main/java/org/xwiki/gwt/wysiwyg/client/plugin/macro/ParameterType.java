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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Defines the parameter type.
 * 
 * @version $Id$
 */
public class ParameterType implements IsSerializable
{
    /**
     * The name of the parameter type. Usually this is the value returned by {@link Class#getName()}.
     */
    private String name;

    /**
     * The values that can be assigned to a parameter of this type. They are stored in a map for localization purposes.
     * The map key is the value id as returned by {@link Class#getEnumConstants()} while the map value is the
     * localization in the execution language.
     */
    private Map<String, String> enumConstants;

    /**
     * @return the parameter type name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the parameter type name.
     * 
     * @param name the new name for this parameter type
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@code true} if this type is an enumeration, i.e. its values are explicitly specified, {@code false}
     *         otherwise
     */
    public boolean isEnum()
    {
        return getEnumConstants() != null;
    }

    /**
     * @return the values that can be assigned to parameters of this type
     */
    public Map<String, String> getEnumConstants()
    {
        return enumConstants;
    }

    /**
     * Sets the values that can be assigned to parameter of this type.
     * 
     * @param enumConstants the new allowed values
     */
    public void setEnumConstants(Map<String, String> enumConstants)
    {
        this.enumConstants = enumConstants;
    }
}
