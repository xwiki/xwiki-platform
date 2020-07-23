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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes how the user interacts with a given property.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Unstable
public class LiveDataPropertyDescriptor
{
    private static final String ID = "id";

    private static final String NAME = "name";

    private static final String DEFAULT_OPERATOR = "defaultOperator";

    private static final String OPERATORS = "operators";

    /**
     * Holds the filter configuration.
     */
    public static class FilterDescriptor extends HashMap<String, Object>
    {
        private static final long serialVersionUID = 1L;

        /**
         * @return the filter id
         */
        public String getId()
        {
            return (String) get(ID);
        }

        /**
         * Sets the filter id.
         * 
         * @param id the new filter id
         * @return the previous filter id
         */
        public String setId(String id)
        {
            return (String) put(ID, id);
        }

        /**
         * @return the list of operators supported by this filter
         */
        @SuppressWarnings("unchecked")
        public List<OperatorDescriptor> getOperators()
        {
            Object operators = get(OPERATORS);
            if (!(operators instanceof List)) {
                operators = new ArrayList<>();
                put(OPERATORS, operators);
            }
            return (List<OperatorDescriptor>) operators;
        }

        /**
         * Adds a new supported operator.
         * 
         * @param id the operator id
         * @param name the operator pretty name
         * @return the added operator
         */
        public OperatorDescriptor addOperator(String id, String name)
        {
            OperatorDescriptor operator = new OperatorDescriptor(id, name);
            getOperators().add(operator);
            return operator;
        }

        /**
         * @return the default operator to use when the user doesn't specify one
         */
        public String getDefaultOperator()
        {
            return (String) get(DEFAULT_OPERATOR);
        }

        /**
         * Sets the default operator to use.
         * 
         * @param operator the new default operator to use
         * @return the previous default operator
         */
        public String setDefaultOperator(String operator)
        {
            return (String) put(DEFAULT_OPERATOR, operator);
        }
    }

    /**
     * An operator to use when filtering the live data.
     */
    public static class OperatorDescriptor extends HashMap<String, Object>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
        public OperatorDescriptor()
        {
        }

        /**
         * Creates a new operator with the specified id and name.
         * 
         * @param id the operator id
         * @param name the operator name
         */
        public OperatorDescriptor(String id, String name)
        {
            setId(id);
            setName(name);
        }

        /**
         * @return the operator id
         */
        public String getId()
        {
            return (String) get(ID);
        }

        /**
         * Sets the operator id.
         * 
         * @param id the new operator id
         * @return the previous operator id
         */
        public String setId(String id)
        {
            return (String) put(ID, id);
        }

        /**
         * @return the operator pretty name
         */
        public String getName()
        {
            return (String) get(NAME);
        }

        /**
         * Sets the operator pretty name.
         * 
         * @param name the new operator pretty name
         * @return the previous operator pretty name
         */
        public String setName(String name)
        {
            return (String) put(NAME, name);
        }
    }

    /**
     * Holds the displayer configuration.
     */
    public static class DisplayerDescriptor extends HashMap<String, Object>
    {
        private static final long serialVersionUID = 1L;

        /**
         * @return the displayer id
         */
        public String getId()
        {
            return (String) get(ID);
        }

        /**
         * Sets the displayer id.
         * 
         * @param id the new displayer id
         * @return the previous displayer id
         */
        public String setId(String id)
        {
            return (String) put(ID, id);
        }
    }

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
    private Boolean sortable;

    /**
     * Whether this property should be displayed or not.
     */
    private Boolean hidden;

    /**
     * Whether the user can filter by this property or not.
     */
    private Boolean filterable;

    /**
     * Displayer configuration, specifies how the property value should be displayed or edited.
     */
    private final DisplayerDescriptor displayer = new DisplayerDescriptor();

    /**
     * Filter configuration, specifies how the user can filter the values of this property.
     */
    private final FilterDescriptor filter = new FilterDescriptor();

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
     * @return whether this property can be used to sort the live data or not; use {@code null} to inherit from the
     *         property type descriptor
     */
    public Boolean isSortable()
    {
        return sortable;
    }

    /**
     * Sets whether this property can be used to sort the live data.
     * 
     * @param sortable whether this property can be used to sort the live data; pass {@code null} to inherit from the
     *            property type descriptor
     */
    public void setSortable(Boolean sortable)
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
     * @return whether this property should be displayed or not; the returned value can be {@code null} in which case
     *         the value should be inherited from the property type descriptor
     */
    public Boolean isHidden()
    {
        return hidden;
    }

    /**
     * Sets whether this property should be displayed or not. Pass {@code null} to inherit from the property type
     * descriptor.
     * 
     * @param hidden {@code true} to hide this property, {@code false} to display this property, {@code null} to inherit
     *            from the property type descriptor
     */
    public void setHidden(Boolean hidden)
    {
        this.hidden = hidden;
    }

    /**
     * @return the displayer configuration
     */
    public DisplayerDescriptor getDisplayer()
    {
        return displayer;
    }

    /**
     * @return whether the user can filter by this property or not; the returned value can be {@code null} in which case
     *         the value should be inherited from the property type descriptor
     */
    public Boolean isFilterable()
    {
        return filterable;
    }

    /**
     * Sets whether the user can filter by this property or not. Pass {@code null} to inherit from the property type
     * descriptor.
     * 
     * @param filterable {@code true} if the user can filter by this property, {@code false} if the user can't filter by
     *            this property, {@code null} to inherit from the property descriptor type
     */
    public void setFilterable(Boolean filterable)
    {
        this.filterable = filterable;
    }

    /**
     * @return the filter configuration
     */
    public FilterDescriptor getFilter()
    {
        return filter;
    }
}
