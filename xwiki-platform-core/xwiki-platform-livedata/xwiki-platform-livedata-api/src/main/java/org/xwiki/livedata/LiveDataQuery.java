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
import java.util.stream.Stream;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The query used to get the live data.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Unstable
public class LiveDataQuery
{
    /**
     * Specifies where to take the data from. Represents the "from" clause.
     */
    public static class Source extends HashMap<String, Object>
    {
        private static final long serialVersionUID = 1L;

        private static final String ID = "id";

        /**
         * @return the source identifier, usually the hint of the {@link LiveDataSource} component
         */
        public String getId()
        {
            return (String) getOrDefault(ID, "default");
        }

        /**
         * Sets the source identifier, usually the hint of the {@link LiveDataSource} component.
         * 
         * @param id the new source identifier
         */
        public void setId(String id)
        {
            put(ID, id);
        }
    }

    /**
     * A set of constraints to apply to a given property.
     */
    public static class Filter
    {
        /**
         * The property to filter.
         */
        private String property;

        /**
         * Specifies whether all the constraints need to be met or any of them.
         */
        private boolean matchAll;

        /**
         * The constraints to apply to property values.
         */
        private final List<Constraint> constraints = new ArrayList<>();

        /**
         * Default constructor.
         */
        public Filter()
        {
        }

        /**
         * Creates a new filter for the specified property, using the given value.
         * 
         * @param property the property to filter on
         * @param value the value to match, using the default operator for the specified property
         */
        public Filter(String property, Object value)
        {
            this.property = property;
            this.constraints.add(new Constraint(value));
        }

        /**
         * Creates a new filter for the specified property, using the given value.
         * 
         * @param property the property to filter on
         * @param operator the operator to use to match the given value (e.g. 'equals', 'contains', 'startsWith')
         * @param value the value to match, using the specified operator
         */
        public Filter(String property, String operator, Object value)
        {
            this.property = property;
            this.constraints.add(new Constraint(value, operator));
        }

        /**
         * Creates a new filter for the specified property, using the given values.
         * 
         * @param property the property to filter on
         * @param matchAll whether to match all the given values or any of them
         * @param values the values to match, using the default operator for the specified property
         */
        public Filter(String property, boolean matchAll, Object... values)
        {
            this.property = property;
            this.matchAll = matchAll;
            Stream.of(values).map(Constraint::new).forEach(this.constraints::add);
        }

        /**
         * Creates a new filter for the specified property, using the given values.
         * 
         * @param property the property to filter on
         * @param operator the operator to use to match the given values (e.g. 'equals', 'contains', 'startsWith')
         * @param matchAll whether to match all the given values or any of them
         * @param values the values to match, using the specified operator
         */
        public Filter(String property, String operator, boolean matchAll, Object... values)
        {
            this.property = property;
            this.matchAll = matchAll;
            Stream.of(values).map(value -> new Constraint(value, operator)).forEach(this.constraints::add);
        }

        /**
         * @return the property to filter
         */
        public String getProperty()
        {
            return property;
        }

        /**
         * Sets the property to filter.
         * 
         * @param property the property to filter
         */
        public void setProperty(String property)
        {
            this.property = property;
        }

        /**
         * @return {@code true} if all the {@link #getConstraints()} have to be met, {@code false} if any of them need
         *         to be satisfied
         */
        public boolean isMatchAll()
        {
            return matchAll;
        }

        /**
         * Sets whether the property value needs to match all the constraints or any.
         * 
         * @param matchAll {@code true} to match all constraints, {@code false} to match any of them
         */
        public void setMatchAll(boolean matchAll)
        {
            this.matchAll = matchAll;
        }

        /**
         * @return the list of constraints to apply to a given property
         */
        public List<Constraint> getConstraints()
        {
            return constraints;
        }
    }

    /**
     * A constraint to apply to a property value.
     */
    public static class Constraint
    {
        /**
         * The operator to use between the property value and the value from this constraint.
         */
        private String operator;

        /**
         * The target value that is compared with the property value using the specified operator.
         */
        private Object value;

        /**
         * Default constructor.
         */
        public Constraint()
        {
        }

        /**
         * Creates a new constraint that uses the given value and the default operator.
         * 
         * @param value the value to match
         */
        public Constraint(Object value)
        {
            this(value, null);
        }

        /**
         * Creates a new constraint that uses the given value and operator.
         * 
         * @param value the value to match
         * @param operator the operator to use
         */
        public Constraint(Object value, String operator)
        {
            this.value = value;
            this.operator = operator;
        }

        /**
         * @return the operator to use between the property value and the value from this constraint
         */
        public String getOperator()
        {
            return operator;
        }

        /**
         * Sets the operator to use between the property value and the value from this constraint.
         * 
         * @param operator the new operator
         */
        public void setOperator(String operator)
        {
            this.operator = operator;
        }

        /**
         * @return the target value that is compared with the property value using the specified operator
         */
        public Object getValue()
        {
            return value;
        }

        /**
         * Sets the target value that is compared with the property value using the specified operator.
         * 
         * @param value the new constraint value
         */
        public void setValue(Object value)
        {
            this.value = value;
        }
    }

    /**
     * A sort entry.
     */
    public static class SortEntry
    {
        /**
         * The property to sort on.
         */
        private String property;

        /**
         * The sort direction.
         */
        private boolean descending;

        /**
         * Default constructor.
         */
        public SortEntry()
        {
        }

        /**
         * Creates a new sort entry for the specified property.
         * 
         * @param property the property to sort on
         */
        public SortEntry(String property)
        {
            this.property = property;
        }

        /**
         * Creates a new sort entry for the specified property and sort order.
         * 
         * @param property the property to sort on
         * @param descending the sort direction
         */
        public SortEntry(String property, boolean descending)
        {
            this.property = property;
            this.descending = descending;
        }

        /**
         * @return the property to sort on
         */
        public String getProperty()
        {
            return property;
        }

        /**
         * Sets the property to sort on.
         * 
         * @param property the property name
         */
        public void setProperty(String property)
        {
            this.property = property;
        }

        /**
         * @return the sort direction
         */
        public boolean isDescending()
        {
            return descending;
        }

        /**
         * Sets the sort direction.
         * 
         * @param descending the new sort direction
         */
        public void setDescending(boolean descending)
        {
            this.descending = descending;
        }
    }

    /**
     * The list of properties whose values we want to fetch. You can view this as the "select" clause of an SQL query.
     */
    private final List<String> properties = new ArrayList<>();

    /**
     * Where to fetch the data from. You can view this as the "from" clause of an SQL query.
     */
    private final Source source = new Source();

    /**
     * The filters to apply on the property values. You can view this as the "where" clause of an SQL query.
     */
    private final List<Filter> filters = new ArrayList<>();

    /**
     * The list of properties to sort on, along with their corresponding sort direction.
     */
    private final List<SortEntry> sort = new ArrayList<>();

    /**
     * Indicates where the current page starts.
     */
    private long offset;

    /**
     * The number of entries to fetch (the page size).
     */
    private int limit = 15;

    /**
     * @return the index where the current page of entries starts
     */
    public long getOffset()
    {
        return offset;
    }

    /**
     * Sets the index where the current page of entries starts.
     * 
     * @param offset the new offset
     */
    public void setOffset(long offset)
    {
        this.offset = offset;
    }

    /**
     * @return the number of entries to fetch (the page size)
     */
    public int getLimit()
    {
        return limit;
    }

    /**
     * Sets the number of entries to fetch (the page size).
     * 
     * @param limit the new limit
     */
    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    /**
     * @return the list of properties whose values we want to fetch
     */
    public List<String> getProperties()
    {
        return properties;
    }

    /**
     * @return where to take the data from
     */
    public Source getSource()
    {
        return source;
    }

    /**
     * @return the filters to apply on the property values
     */
    public List<Filter> getFilters()
    {
        return filters;
    }

    /**
     * @return the list of properties to sort on, along with their corresponding sort direction
     */
    public List<SortEntry> getSort()
    {
        return sort;
    }
}
