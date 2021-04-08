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
package org.xwiki.notifications.filters.expression;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.notifications.filters.expression.generics.AbstractBinaryOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;

/**
 * Define a "&lt;=" condition in a filtering expression.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public final class LesserThanNode extends AbstractBinaryOperatorNode
{
    private final boolean orEquals;

    /**
     * Constructs a new "&lt;=" node.
     *
     * @param leftOperand the left operand
     * @param rightOperand the right operand
     */
    public LesserThanNode(AbstractValueNode leftOperand, AbstractValueNode rightOperand)
    {
        this(leftOperand, rightOperand, true);
    }

    /**
     * Constructs a new "&lt;=" node.
     *
     * @param leftOperand the left operand
     * @param rightOperand the right operand
     * @param orEquals true if it's a lesser or equals, false if it's only lesser
     * @since 12.7RC1
     * @since 12.6.1
     */
    public LesserThanNode(AbstractValueNode leftOperand, AbstractValueNode rightOperand, boolean orEquals)
    {
        super(leftOperand, rightOperand);

        this.orEquals = orEquals;
    }

    /**
     * @return true if it's a lesser or equals, false if it's only lesser
     * @since 12.7RC1
     * @since 12.6.1
     */
    public boolean isOrEquals()
    {
        return this.orEquals;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }

        if (o instanceof LesserThanNode) {
            return super.equals(o) && isOrEquals() == ((LesserThanNode) o).isOrEquals();
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.appendSuper(super.hashCode());
        builder.append(isOrEquals());

        return builder.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("%s %s %s", getLeftOperand(), isOrEquals() ? "<=" : "<", getRightOperand());
    }
}
