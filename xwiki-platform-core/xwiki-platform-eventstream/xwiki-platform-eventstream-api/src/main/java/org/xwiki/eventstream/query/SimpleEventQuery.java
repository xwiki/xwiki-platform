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
package org.xwiki.eventstream.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.query.CompareQueryCondition.CompareType;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A very basic implementation of {@link EventQuery}.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Unstable
public class SimpleEventQuery extends GroupQueryCondition implements PageableEventQuery, SortableEventQuery
{
    private List<QueryCondition> currentConditions = conditions;

    private long limit = -1;

    private long offset;

    private List<SortClause> sorts = new ArrayList<>();

    private boolean nextReversed;

    private boolean nextOr;

    private boolean nextOpen;

    private boolean nextParameter;

    private Deque<GroupQueryCondition> groupStack = new LinkedList<>();

    /**
     * An empty query.
     */
    public SimpleEventQuery()
    {
        super(false, false);
    }

    /**
     * @param offset the maximum number of events to return
     * @param limit the maximum number of events to return
     */
    public SimpleEventQuery(long offset, long limit)
    {
        this();

        setOffset(offset);
        setLimit(limit);
    }

    /**
     * @return limit the maximum number of events to return, -1 for no limit (0 return no results)
     * @see #setLimit(long)
     */
    @Override
    public long getLimit()
    {
        return this.limit;
    }

    /**
     * @param limit the maximum number of events to return, -1 for no limit (0 return no results)
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
     * Reverse the following filter.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery not()
    {
        this.nextReversed = true;

        return this;
    }

    /**
     * Group together the previous and next condition in a OR group.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery or()
    {
        nextOr(true);

        return this;
    }

    /**
     * Group together the previous and next condition in a AND group.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery and()
    {
        nextOr(false);

        return this;
    }

    /**
     * Next call will be about custom event parameters.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 13.9RC1
     */
    public SimpleEventQuery parameter()
    {
        this.nextParameter = true;

        return this;
    }

    private void nextOr(boolean or)
    {
        if (!this.nextOpen && !this.currentConditions.isEmpty()) {
            this.nextOr = or;
        }
    }

    /**
     * Start a new group of conditions.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery open()
    {
        if (this.nextOr) {
            forceOpen(true);

            this.nextOr = false;
        } else if (this.nextOpen || this.nextReversed) {
            forceOpen(false);
        } else {
            this.nextOpen = true;
        }

        return this;
    }

    private void forceOpen(boolean or, QueryCondition... newConditions)
    {
        GroupQueryCondition group = new GroupQueryCondition(or, this.nextReversed, newConditions);

        this.currentConditions.add(group);
        this.groupStack.push(group);
        this.currentConditions = group.conditions;

        this.nextReversed = false;
    }

    /**
     * Stop the current group of conditions.
     * 
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery close()
    {
        if (this.nextOpen) {
            this.nextOpen = false;
        } else {
            GroupQueryCondition group = this.groupStack.pop();

            this.currentConditions = this.groupStack.isEmpty() ? this.conditions : this.groupStack.peek().conditions;

            if (group.getConditions().isEmpty()) {
                // Cleanup empty group
                this.currentConditions.remove(this.currentConditions.size() - 1);
            } else if (group.getConditions().size() == 1
                && group.getConditions().get(0) instanceof GroupQueryCondition) {
                // Optimize group containing only a group
                this.currentConditions.set(this.currentConditions.size() - 1, group.getConditions().get(0));
            }
        }

        return this;
    }

    private void addCompareCondition(String property, Object value, CompareType type)
    {
        addCondition(new CompareQueryCondition(property, this.nextParameter, value, type, this.nextReversed));
    }

    private void addCondition(QueryCondition newCondition)
    {
        if (this.nextOpen) {
            forceOpen(false);

            this.nextOpen = false;
        }

        if (this.nextOr) {
            QueryCondition previousCondition = this.currentConditions.get(this.currentConditions.size() - 1);
            if (previousCondition instanceof GroupQueryCondition && ((GroupQueryCondition) previousCondition).isOr()) {
                ((GroupQueryCondition) previousCondition).conditions.add(newCondition);
            } else {
                // Remove previous condition from the list
                this.currentConditions.remove(this.currentConditions.size() - 1);

                // Add a OR condition with the previous and new conditions
                this.currentConditions
                    .add(new GroupQueryCondition(true, this.nextReversed, previousCondition, newCondition));
            }
        } else {
            this.currentConditions.add(newCondition);
        }

        // Reset flags
        this.nextReversed = false;
        this.nextOr = false;
        this.nextParameter = false;
    }

    /**
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     */
    public SimpleEventQuery eq(String property, Object value)
    {
        addCompareCondition(property, value, CompareType.EQUALS);

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
        addCompareCondition(property, value, CompareType.LESS);

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
        addCompareCondition(property, value, CompareType.LESS_OR_EQUALS);

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
        addCompareCondition(property, value, CompareType.GREATER);

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
        addCompareCondition(property, value, CompareType.GREATER_OR_EQUALS);

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
     * @param property the name of the property
     * @param values the values to compare to the property
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery in(String property, List<?> values)
    {
        addCondition(new InQueryCondition(this.nextReversed, property, this.nextParameter, (List) values));

        return this;
    }

    /**
     * @param property the name of the property
     * @param values the values to compare to the property
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery in(String property, Object... values)
    {
        return in(property, Arrays.asList(values));
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
        addCondition(new StatusQueryCondition(entityId, null, this.nextReversed));

        return this;
    }

    /**
     * Select only event associated with the passed status entity.
     * 
     * @param entityId event status entity id
     * @param read indicate if read or unread statues should selected, null for all
     * @return this {@link SimpleEventQuery}
     * @since 12.5RC1
     */
    public SimpleEventQuery withStatus(String entityId, boolean read)
    {
        addCondition(new StatusQueryCondition(entityId, read, this.nextReversed));

        return this;
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
        addCondition(new StatusQueryCondition(null, read, this.nextReversed));

        return this;
    }

    /**
     * Select events associated for which the passed entity should receive mails.
     * 
     * @param entityId the identifier of the entity that should receive the mail
     * @return this {@link SimpleEventQuery}
     * @since 12.6
     */
    public SimpleEventQuery withMail(String entityId)
    {
        addCondition(new MailEntityQueryCondition(entityId, this.nextReversed));

        return this;
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
        this.sorts.add(new SortClause(property, this.nextParameter, order));

        // Reset flag
        this.nextParameter = false;

        return this;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(getLimit());
        builder.append(getOffset());
        builder.append(getSorts());

        return builder.build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SimpleEventQuery) {
            SimpleEventQuery query = (SimpleEventQuery) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.appendSuper(super.equals(obj));
            builder.append(getLimit(), query.getLimit());
            builder.append(getOffset(), query.getOffset());
            builder.append(getSorts(), query.getSorts());

            return builder.build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);

        builder.append("limit", getLimit());
        builder.append("offset", getOffset());
        builder.append("conditions", getConditions());
        builder.append("sorts", getSorts());

        return builder.build();
    }
}
