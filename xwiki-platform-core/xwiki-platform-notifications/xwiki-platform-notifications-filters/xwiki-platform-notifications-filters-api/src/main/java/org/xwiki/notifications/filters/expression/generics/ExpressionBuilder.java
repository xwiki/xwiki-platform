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
package org.xwiki.notifications.filters.expression.generics;

import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.expression.NotNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;

/**
 * Class used to give a couple of helper methods in order to create a filtering expression.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public final class ExpressionBuilder
{
    /**
     * As {@link ExpressionBuilder} is an utility class, hide its default constructor.
     */
    private ExpressionBuilder()
    {
        super();
    }

    /**
     * Instantiate a new {@link PropertyValueNode} using the given property.
     *
     * @param property the value of the node
     * @return the generated {@link PropertyValueNode}
     */
    public static PropertyValueNode value(NotificationFilterProperty property)
    {
        return new PropertyValueNode(property);
    }

    /**
     * Instantiate a new {@link StringValueNode} using the given value.
     *
     * @param value the value of the node
     * @return the generated {@link StringValueNode}
     */
    public static StringValueNode value(String value)
    {
        return new StringValueNode(value);
    }

    /**
     * Instantiate a new {@link NotNode} using the given value.
     *
     * @param node the operator wrapped by the not node
     * @return the generated {@link NotNode}
     */
    public static NotNode not(AbstractOperatorNode node)
    {
        return new NotNode(node);
    }
}
