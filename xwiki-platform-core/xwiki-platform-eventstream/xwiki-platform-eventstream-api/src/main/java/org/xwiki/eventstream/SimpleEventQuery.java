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
import java.util.List;

import org.xwiki.stability.Unstable;

/**
 * A very basic implementation of {@link EventQuery}.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Unstable
public class SimpleEventQuery implements PageableEventQuery
{
    /**
     * An equal condition.
     * 
     * @version $Id$
     */
    public static class EqualQueryCondition
    {
        private String property;

        private Object value;

        /**
         * @param property the name of the property
         * @param value the value the property should be equal to
         */
        public EqualQueryCondition(String property, Object value)
        {
            this.property = property;
            this.value = value;
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
    }

    private final List<EqualQueryCondition> conditions = new ArrayList<>();

    private long limit = -1;

    private long offset;

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
     * @param property the name of the property
     * @param value the value the property should be equal to
     * @return this {@link SimpleEventQuery}
     */
    public SimpleEventQuery eq(String property, Object value)
    {
        this.conditions.add(new EqualQueryCondition(property, value));

        return this;
    }

    /**
     * @return the conditions the conditions
     */
    public List<EqualQueryCondition> getConditions()
    {
        return this.conditions;
    }
}
