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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Describes how the user interacts with a given property.
 * 
 * @version $Id$
 * @since 12.10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveDataPropertyDescriptor
{
    private static final String NAME = "name";

    /**
     * Holds the filter configuration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilterDescriptor extends BaseDescriptor
    {
        private String defaultOperator;

        private List<OperatorDescriptor> operators;

        /**
         * Default constructor.
         */
        public FilterDescriptor()
        {
        }

        /**
         * Creates a new descriptor for the filter with the specified id.
         * 
         * @param id the filter id
         */
        public FilterDescriptor(String id)
        {
            setId(id);
        }

        /**
         * @return the list of operators supported by this filter
         */
        public List<OperatorDescriptor> getOperators()
        {
            return operators;
        }

        /**
         * Set the list of operators supported by this filter.
         * 
         * @param operators the new list of supported operators
         */
        public void setOperators(List<OperatorDescriptor> operators)
        {
            this.operators = operators;
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
            if (this.operators == null) {
                this.operators = new ArrayList<>();
            }
            this.operators.add(operator);
            return operator;
        }

        /**
         * @return the default operator to use when the user doesn't specify one
         */
        public String getDefaultOperator()
        {
            return defaultOperator;
        }

        /**
         * Sets the default operator to use.
         * 
         * @param defaultOperator the new default operator to use
         */
        public void setDefaultOperator(String defaultOperator)
        {
            this.defaultOperator = defaultOperator;
        }

        /**
         * Prevent {@code null} values where it's possible.
         */
        public void initialize()
        {
            if (this.operators == null) {
                this.operators = new ArrayList<>();
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FilterDescriptor that = (FilterDescriptor) o;

            return new EqualsBuilder().appendSuper(super.equals(o))
                .append(defaultOperator, that.defaultOperator).append(operators, that.operators).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(23, 87)
                .appendSuper(super.hashCode())
                .append(defaultOperator).append(operators)
                .toHashCode();
        }

        @Override
        public String toString()
        {
            return new XWikiToStringBuilder(this)
                .appendSuper(super.toString())
                .append("defaultOperator", defaultOperator)
                .append("operators", operators)
                .toString();
        }
    }

    /**
     * An operator to use when filtering the live data.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OperatorDescriptor extends BaseDescriptor
    {
        private String name;

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
         * @return the operator pretty name
         */
        public String getName()
        {
            return name;
        }

        /**
         * Sets the operator pretty name.
         * 
         * @param name the new operator pretty name
         */
        public void setName(String name)
        {
            this.name = name;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OperatorDescriptor that = (OperatorDescriptor) o;

            return new EqualsBuilder().appendSuper(super.equals(o)).append(name, that.name)
                .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 69).appendSuper(super.hashCode()).append(name).toHashCode();
        }

        @Override
        public String toString()
        {
            return new XWikiToStringBuilder(this)
                .appendSuper(super.toString())
                .append(NAME, name)
                .toString();
        }
    }

    /**
     * Holds the displayer configuration.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DisplayerDescriptor extends BaseDescriptor
    {
        /**
         * Default constructor.
         */
        public DisplayerDescriptor()
        {
        }

        /**
         * Creates a new descriptor for the displayer with the specified id.
         * 
         * @param id the displayer id
         */
        public DisplayerDescriptor(String id)
        {
            setId(id);
        }
    }

    /**
     * The property id.
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
    private Map<String, Object> icon;

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
    private Boolean visible;

    /**
     * Whether the user can filter by this property or not.
     */
    private Boolean filterable;

    /**
     * Whether the user can edit this property.
     */
    private Boolean editable;

    /**
     * Displayer configuration, specifies how the property value should be displayed or edited.
     */
    private DisplayerDescriptor displayer;

    /**
     * Filter configuration, specifies how the user can filter the values of this property.
     */
    private FilterDescriptor filter;

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
     * Set the property id.
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
     * Sets the icon meta data.
     * 
     * @param icon the new icon meta data
     */
    public void setIcon(Map<String, Object> icon)
    {
        this.icon = icon;
    }

    /**
     * @return whether this property should be displayed or not; the returned value can be {@code null} in which case
     *         the value should be inherited from the property type descriptor
     */
    public Boolean isVisible()
    {
        return visible;
    }

    /**
     * Sets whether this property should be displayed or not. Pass {@code null} to inherit from the property type
     * descriptor.
     * 
     * @param visible {@code true} to display this property, {@code false} to hide it, {@code null} to inherit from the
     *            property type descriptor
     */
    public void setVisible(Boolean visible)
    {
        this.visible = visible;
    }

    /**
     * @return the displayer configuration
     */
    public DisplayerDescriptor getDisplayer()
    {
        return displayer;
    }

    /**
     * Sets the displayer configuration.
     * 
     * @param displayer the new displayer configuration
     */
    public void setDisplayer(DisplayerDescriptor displayer)
    {
        this.displayer = displayer;
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

    /**
     * Sets the filter configuration.
     * 
     * @param filter the new filter configuration
     */
    public void setFilter(FilterDescriptor filter)
    {
        this.filter = filter;
    }

    /**
     * @return whether the user can edit the values of this property or not; the returned value can be {@code null} in
     *         which case the value should be inherited from the property type descriptor
     */
    public Boolean isEditable()
    {
        return editable;
    }

    /**
     * Sets whether the user can edit the values of this property or not. Pass {@code null} to inherit from the property
     * type descriptor.
     * 
     * @param editable {@code true} if the user can edit the values of this property, {@code false} if the user
     *            shouldn't be able to edit the property values, {@code null} to inherit from the property descriptor
     *            type
     */
    public void setEditable(Boolean editable)
    {
        this.editable = editable;
    }

    /**
     * Prevent {@code null} values where it's possible.
     */
    public void initialize()
    {
        if (this.visible == null) {
            this.visible = true;
        }
        if (this.icon == null) {
            this.icon = new HashMap<>();
        }
        if (this.displayer == null) {
            this.displayer = new DisplayerDescriptor();
        }
        if (this.filter == null) {
            this.filter = new FilterDescriptor();
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LiveDataPropertyDescriptor that = (LiveDataPropertyDescriptor) o;

        return new EqualsBuilder().append(id, that.id).append(name, that.name)
            .append(description, that.description).append(icon, that.icon).append(type, that.type)
            .append(sortable, that.sortable).append(visible, that.visible).append(filterable, that.filterable)
            .append(editable, that.editable).append(displayer, that.displayer).append(filter, that.filter)
            .append(styleName, that.styleName).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(27, 37).append(id).append(name).append(description).append(icon).append(type)
            .append(sortable).append(visible).append(filterable).append(editable).append(displayer).append(filter)
            .append(styleName).toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .appendSuper(super.toString())
            .append("id", id)
            .append(NAME, name)
            .append("description", description)
            .append("icon", icon)
            .append("type", type)
            .append("sortable", sortable)
            .append("visible", visible)
            .append("filterable", filterable)
            .append("editable", editable)
            .append("displayer", displayer)
            .append("filter", filter)
            .append("styleName", styleName)
            .toString();
    }
}
