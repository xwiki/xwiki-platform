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
package org.xwiki.notifications.sources.internal;

import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;

/**
 * Define an ORDER BY operation in a filtering expression.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class OrderByNode extends AbstractOperatorNode
{
    /**
     * The different kind of ordering.
     */
    public enum Order
    {
        /**
         * Ascending order.
         */
        ASC,
        /**
         * Descending order.
         */
        DESC
    }

    private AbstractOperatorNode query;

    private PropertyValueNode property;

    private Order order;

    /**
     * Construct an ORDER BY node.
     * @param query the query to order
     * @param property the property on which to order on
     * @param order the order to use
     */
    public OrderByNode(AbstractOperatorNode query, PropertyValueNode property,
            Order order)
    {
        this.query = query;
        this.property = property;
        this.order = order;
    }

    /**
     * @return the query to order
     */
    public AbstractOperatorNode getQuery()
    {
        return query;
    }

    /**
     * @return the property to order on
     */
    public PropertyValueNode getProperty()
    {
        return property;
    }

    /**
     * @return the order to use
     */
    public Order getOrder()
    {
        return order;
    }

    @Override
    public String toString()
    {
        return String.format("%s ORDER BY %s %s", query, property, order.name());
    }
}
