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

import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Allow sorting events.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public interface SortableEventQuery extends EventQuery
{
    /**
     * The sort clause to apply to the found events.
     * 
     * @version $Id$
     */
    class SortClause
    {
        /**
         * The order to apply.
         * 
         * @version $Id$
         */
        public enum Order
        {
            /**
             * Sort in descending order.
             */
            DESC,

            /**
             * Sort in ascending order.
             */
            ASC
        }

        private final String property;

        private final boolean custom;

        private final Type customType;

        private final Order order;

        /**
         * @param property see {@link #getProperty()}
         * @param order see {@link #getOrder()}
         */
        public SortClause(String property, Order order)
        {
            this(property, false, order);
        }

        /**
         * @param property see {@link #getProperty()}
         * @param custom see {@link #isCustom()}
         * @param order see {@link #getOrder()}
         * @since 13.9RC1
         */
        public SortClause(String property, boolean custom, Order order)
        {
            this(property, custom, null, order);
        }

        /**
         * @param property see {@link #getProperty()}
         * @param custom see {@link #isCustom()}
         * @param customType see {@link #getCustomType()}
         * @param order see {@link #getOrder()}
         * @since 14.2RC1
         */
        public SortClause(String property, boolean custom, Type customType, Order order)
        {
            this.property = property;
            this.custom = custom;
            this.customType = customType;
            this.order = order;
        }

        /**
         * @return the property to sort on
         */
        public String getProperty()
        {
            return this.property;
        }

        /**
         * @return true if it's a custom event parameter
         * @since 13.9RC1
         * @deprecated use {@link #isCustom()} instead
         */
        @Deprecated(since = "13.9RC1")
        public boolean isParameter()
        {
            return isCustom();
        }

        /**
         * @return true if it's a custom event parameter
         * @since 14.2RC1
         */
        public boolean isCustom()
        {
            return this.custom;
        }

        /**
         * @return the type in which that property was stored
         * @since 14.2RC1
         */
        public Type getCustomType()
        {
            return this.customType;
        }

        /**
         * @return the order to apply (ascending or descending)
         */
        public Order getOrder()
        {
            return this.order;
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(getProperty());
            builder.append(isCustom());
            builder.append(getCustomType());
            builder.append(getOrder());

            return builder.build();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) {
                return true;
            }

            if (obj instanceof SortClause) {
                SortClause otherSortClause = (SortClause) obj;

                EqualsBuilder builder = new EqualsBuilder();

                builder.append(getProperty(), otherSortClause.getProperty());
                builder.append(isCustom(), otherSortClause.isCustom());
                builder.append(getCustomType(), otherSortClause.getCustomType());
                builder.append(getOrder(), otherSortClause.getOrder());

                return builder.isEquals();
            }

            return false;
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new XWikiToStringBuilder(this);

            builder.append("property", getProperty());
            builder.append("custom", isCustom());
            builder.append("customType", getCustomType());
            builder.append("order", getOrder());

            return builder.build();
        }
    }

    /**
     * @return the sort clauses
     */
    List<SortClause> getSorts();
}
