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
 * Describes how the user interacts with a given property.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Unstable
public class LiveDataPropertyDescriptor
{
    /**
     * Identifies the property that this descriptor corresponds to.
     */
    private String id;

    /**
     * The property pretty name, usually displayed before the property value.
     */
    private String name;

    /**
     * The property description, usually displayed when hovering the property name.
     */
    private String description;

    /**
     * Specifies the property icon, usually displayed before the property name. The map contains meta data about the
     * icon, such as the icon set name, icon set type, URL or CSS class name.
     */
    private final Map<String, Object> icon = new HashMap<>();

    /**
     * Indicates the property type, which usually has default settings that the property descriptor can default to.
     */
    private String type;

    /**
     * Whether the user can sort on this property or not.
     */
    private boolean sortable;

    /**
     * Displayer configuration, specifies how the property value should be displayed or edited.
     */
    private final Map<String, Object> displayer = new HashMap<>();

    /**
     * Filter configuration, specifies how the user can filter the values of this property.
     */
    private final Map<String, Object> filter = new HashMap<>();

    /**
     * Optional CSS class name to add to the HTML element used to display this property.
     */
    private String styleName;

    /**
     * @return the property id
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the property id.
     * 
     * @param id the new property id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the property pretty name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the property pretty name.
     * 
     * @param name the new pretty name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the property description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the property description.
     * 
     * @param description the new property description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the property type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the property type.
     * 
     * @param type the new property type
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return whether this property can be used to sort the live data or not
     */
    public boolean isSortable()
    {
        return sortable;
    }

    /**
     * Sets whether this property can be used to sort the live data.
     * 
     * @param sortable whether this property can be used to sort the live data
     */
    public void setSortable(boolean sortable)
    {
        this.sortable = sortable;
    }

    /**
     * @return the CSS class name to add to the HTML element used to display this property
     */
    public String getStyleName()
    {
        return styleName;
    }

    /**
     * Sets the CSS class name to add to the element used to display this property.
     * 
     * @param styleName the new style name
     */
    public void setStyleName(String styleName)
    {
        this.styleName = styleName;
    }

    /**
     * @return the icon meta data
     */
    public Map<String, Object> getIcon()
    {
        return icon;
    }

    /**
     * @return the displayer configuration
     */
    public Map<String, Object> getDisplayer()
    {
        return displayer;
    }

    /**
     * @return the filter configuration
     */
    public Map<String, Object> getFilter()
    {
        return filter;
    }
}
