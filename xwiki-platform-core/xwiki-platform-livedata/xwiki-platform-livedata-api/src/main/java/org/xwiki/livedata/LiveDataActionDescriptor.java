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

import org.xwiki.stability.Unstable;

/**
 * Describes a live data action.
 * 
 * @version $Id$
 * @since 12.10.1
 * @since 13.0
 */
@Unstable
public class LiveDataActionDescriptor extends BaseDescriptor
{
    /**
     * The action pretty name.
     */
    private String name;

    /**
     * The action description.
     */
    private String description;

    /**
     * Specifies the action icon. The map contains meta data about the icon, such as the icon set name, icon set type,
     * URL or CSS class name.
     */
    private Map<String, Object> icon;

    /**
     * @return the action pretty name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the action pretty name.
     * 
     * @param name the new pretty name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the action description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the action description.
     * 
     * @param description the new action description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the icon meta data
     */
    public Map<String, Object> getIcon()
    {
        return icon;
    }

    /**
     * Sets the icon meta data.
     * 
     * @param icon the new icon meta data
     */
    public void setIcon(Map<String, Object> icon)
    {
        this.icon = icon;
    }

    /**
     * Prevent {@code null} values where it's possible.
     */
    public void initialize()
    {
        if (this.icon == null) {
            this.icon = new HashMap<>();
        }
    }
}
