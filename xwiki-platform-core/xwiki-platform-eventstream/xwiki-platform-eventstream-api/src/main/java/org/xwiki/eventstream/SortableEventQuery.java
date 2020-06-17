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

import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Allow sorting events.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
@Unstable
public interface SortableEventQuery extends EventQuery
{
    /**
     * The sort close to apply to the found events.
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

        private final Order order;

        public SortClause(String property, Order order)
        {
            this.property = property;
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
         * @return the order to apply
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
            builder.append(getOrder());

            return builder.build();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) {
                return true;
            }

            return obj instanceof SortClause && ((SortClause) obj).getProperty().equals(getProperty())
                && ((SortClause) obj).getOrder() == getOrder();
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new XWikiToStringBuilder(this);

            builder.append("property", this.property);
            builder.append("order", this.order);
            return builder.build();
        }
    }

    /**
     * @return the sort closes
     */
    List<SortClause> getSorts();
}
