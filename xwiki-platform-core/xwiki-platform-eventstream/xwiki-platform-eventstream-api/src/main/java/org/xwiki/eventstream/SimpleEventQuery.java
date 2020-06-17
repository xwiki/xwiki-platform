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
package org.xwiki.eventstream;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.eventstream.SimpleEventQuery.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.SortableEventQuery.SortClause.Order;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A very basic implementation of {@link EventQuery}.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Unstable
public class SimpleEventQuery implements PageableEventQuery, SortableEventQuery
{
    /**
     * An comparison between a property value and a passed value.
     * 
     * @version $Id$
     * @since 12.5RC1
     */
    public static class CompareQueryCondition
    {
        /**
         * The type of comparison.
         * 
         * @version $Id$
         */
        public enum CompareType
        {
            /**
             * The property value is greater than the passed value.
             */
            GREATER,

            /**
             * The property value is lower than the passed value.
             */
            LESS,

            /**
             * The property value is greater or equals to the passed value.
             */
            GREATER_OR_EQUALS,

            /**
             * The property value is lower or equals to the passed value.
             */
            LESS_OR_EQUALS,

            /**
             * The property value is equals to the passed value.
             */
            EQUALS
        }

        private final boolean reversed;

        private final String property;

        private final Object value;

        private final CompareType type;

        /**
         * @param property the name of the property
         * @param value the value the property should be equal to
         * @param type the type of comparison
         */
        public CompareQueryCondition(String property, Object value, CompareType type)
        {
            this(property, value, type, false);
        }

        /**
         * @param property the name of the property
         * @param value the value the property should be equal to
         * @param type the type of comparison
         * @param reversed true if the condition should be reversed
         */
        public CompareQueryCondition(String property, Object value, CompareType type, boolean reversed)
        {
            this.property = property;
            this.value = value;
            this.type = type;
            this.reversed = reversed;
        }

        /**
         * @return the property the name of the property
         */
        public String getProperty()
        {
            return this.property;
        }

        /**
         * @return the value the property should be equal to
         */
        public Object getValue()
        {
            return this.value;
        }

        /**
         * @return the type the type of comparison
         */
        public CompareType getType()
        {
            return this.type;
        }

        /**
         * @return true of the condition should be reversed
         */
        public boolean isReversed()
        {
            return this.reversed;
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(isReversed());
            builder.append(getProperty());
            builder.append(getValue());
            builder.append(getType());

            return builder.build();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) {
                return true;
            }

            if (obj instanceof CompareQueryCondition) {
                CompareQueryCondition compare = (CompareQueryCondition) obj;

                EqualsBuilder builder = new EqualsBuilder();

                builder.append(isReversed(), compare.isReversed());
                builder.append(getProperty(), compare.getProperty());
                builder.append(getValue(), compare.getValue());
                builder.append(getType(), compare.getType());

                return builder.build();
            }

            return false;
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new XWikiToStringBuilder(this);

            builder.append("reversed", isReversed());
            builder.append("property", getProperty());
            builder.append("value", getValue());
            builder.append("type", getType());

            return builder.build();
        }
    }

    private final List<CompareQueryCondition> conditions = new ArrayList<>();

    private long limit = -1;

    private long offset;

    private String statusEntityId;

    private Boolean statusRead;

    private List<SortClause> sorts = new ArrayList<>();

    private boolean reversed;

    /**
     * An empty query.
     */
    public SimpleEventQuery()
    {
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     */
    public SimpleEventQuery(String property, Object value)
    {
        eq(property, value);
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @param offset the maximum number of events to return
     * @param limit the maximum number of events to return
     */
    public SimpleEventQuery(String property, Object value, long offset, long limit)
    {
        this(property, value);

        setOffset(offset);
        setLimit(limit);
    }

    /**
     * @param offset the maximum number of events to return
     * @param limit the maximum number of events to return
     */
    public SimpleEventQuery(long offset, long limit)
    {
        setOffset(offset);
        setLimit(limit);
    }

    /**
     * @return limit the maximum number of events to return
     * @see #setLimit(long)
     */
    @Override
    public long getLimit()
    {
        return this.limit;
    }

    /**
     * @param limit the maximum number of events to return
     * @return this query.
     */
    public SimpleEventQuery setLimit(long limit)
    {
        this.limit = limit;

        return this;
    }

    /**
     * @return offset the index where to start returning events
     * @see #setOffset(long)
     */
    @Override
    public long getOffset()
    {
        return this.offset;
    }

    /**
     * @param offset the index where to start returning events
     * @return this query.
     */
    public SimpleEventQuery setOffset(long offset)
    {
        this.offset = offset;

        return this;
    }

    /**
     * Reverse the following condition.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery not()
    {
        this.reversed = true;

        return this;
    }

    private void addCondition(String property, Object value, CompareType type)
    {
        this.conditions.add(new CompareQueryCondition(property, value, type, this.reversed));

        this.reversed = false;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     */
    public SimpleEventQuery eq(String property, Object value)
    {
        addCondition(property, value, CompareType.EQUALS);

        return this;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery less(String property, Object value)
    {
        addCondition(property, value, CompareType.LESS);

        return this;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery lessOrEq(String property, Object value)
    {
        addCondition(property, value, CompareType.LESS_OR_EQUALS);

        return this;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery greater(String property, Object value)
    {
        addCondition(property, value, CompareType.GREATER);

        return this;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery greaterOrEq(String property, Object value)
    {
        addCondition(property, value, CompareType.GREATER_OR_EQUALS);

        return this;
    }

    /**
     * @param date the date before which events should be selected
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery before(Date date)
    {
        less(Event.FIELD_DATE, date);

        return this;
    }

    /**
     * @param date the date after which the events should be selected
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery after(Date date)
    {
        greater(Event.FIELD_DATE, date);

        return this;
    }

    /**
     * Select only event associated with the passed status entity.
     * 
     * @param entityId event status entity id
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery withStatus(String entityId)
    {
        this.statusEntityId = entityId;

        return this;
    }

    /**
     * Select only event associated with the passed status entity.
     * 
     * @param entity event status entity id
     * @param read indicate if read or unread statues should selected, null for all
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery withStatus(String entity, boolean read)
    {
        return withStatus(entity).withStatus(read);
    }

    /**
     * Select only event associated with the passed status entity.
     * 
     * @param read indicate if read or unread events should be selected, null if disabled
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery withStatus(boolean read)
    {
        this.statusRead = read;

        return this;
    }

    /**
     * @return the status entity to filter on or null if not enabled
     * @since 12.5RC1
     */
    public String getStatusEntityId()
    {
        return this.statusEntityId;
    }

    /**
     * @return indicate if read or unread event should be selected, null if disabled
     * @since 12.5RC1
     */
    public Boolean getStatusRead()
    {
        return this.statusRead;
    }

    /**
     * @return the conditions the conditions
     * @since 12.5RC1
     */
    public List<CompareQueryCondition> getConditions()
    {
        return this.conditions;
    }

    @Override
    public List<SortClause> getSorts()
    {
        return this.sorts;
    }

    /**
     * Adds a single sort clause to the end of the current sort information.
     * 
     * @param property the property name
     * @param order the sort order
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery addSort(String property, Order order)
    {
        this.sorts.add(new SortClause(property, order));

        return this;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("limit", this.limit);
        builder.append("offset", this.offset);
        builder.append("conditions", this.conditions);
        builder.append("statusEntityId", this.statusEntityId);
        builder.append("statusRead", this.statusRead);
        builder.append("sorts", this.sorts);

        return builder.build();
    }
}
