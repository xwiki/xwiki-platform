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

/**
 * Unary operand node definition for a filtering expression. This class can be extended in order to define new
 * unary operations.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractUnaryOperatorNode extends AbstractOperatorNode
{
    private AbstractNode operand;

    /**
     * Constructs a new {@link AbstractUnaryOperatorNode}.
     *
     * @param operand the node operand
     */
    public AbstractUnaryOperatorNode(AbstractNode operand)
    {
        if (operand == null) {
            throw new NullPointerException("A unary operand should have its operand defined.");
        }

        this.operand = operand;
    }

    /**
     * @return the node operand
     */
    public AbstractNode getOperand()
    {
        return operand;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof AbstractUnaryOperatorNode
                && operand.equals(((AbstractUnaryOperatorNode) o).operand));
    }

    @Override
    public int hashCode()
    {
        return 13 + operand.hashCode();
    }
}
