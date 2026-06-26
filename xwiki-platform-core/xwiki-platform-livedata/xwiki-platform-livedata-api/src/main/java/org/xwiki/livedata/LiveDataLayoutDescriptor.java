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
package org.xwiki.livedata;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Holds the layout configuration.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LiveDataLayoutDescriptor extends BaseDescriptor implements InitializableLiveDataElement
{
    private String name;

    private Map<String, Object> icon;

    /**
     * Default constructor.
     */
    public LiveDataLayoutDescriptor()
    {
    }

    /**
     * Creates a descriptor for the layout with the given id.
     * 
     * @param id the layout id
     */
    public LiveDataLayoutDescriptor(String id)
    {
        setId(id);
    }

    /**
     * @return the layout pretty name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the layout pretty name.
     * 
     * @param name the new layout name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the icon meta data
     */
    public Map<String, Object> getIcon()
    {
        return icon;
    }

    /**
     * Set the icon meta data.
     * 
     * @param icon the icon meta data
     */
    public void setIcon(Map<String, Object> icon)
    {
        this.icon = icon;
    }

    @Override
    public void initialize()
    {
        if (this.icon == null) {
            this.icon = new HashMap<>();
        }
    }
}
