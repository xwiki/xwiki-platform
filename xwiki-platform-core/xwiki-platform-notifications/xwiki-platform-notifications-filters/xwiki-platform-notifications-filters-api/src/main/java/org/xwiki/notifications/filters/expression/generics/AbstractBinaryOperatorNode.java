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
 * Binary operand node definition for a filtering expression. This class can be extended in order to define new
 * binary operations.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractBinaryOperatorNode extends AbstractOperatorNode
{
    private AbstractNode leftOperand;

    private AbstractNode rightOperand;

    /**
     * Constructs a new binary operator node.
     * Both operands should not be null.
     *
     * @param leftOperand the left operand
     * @param rightOperand the right operand
     */
    public AbstractBinaryOperatorNode(AbstractNode leftOperand, AbstractNode rightOperand)
    {
        if (leftOperand == null || rightOperand == null) {
            throw new NullPointerException("A binary operand should have its two operands defined.");
        }

        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }

    /**
     * @return the left operand
     */
    public AbstractNode getLeftOperand()
    {
        return leftOperand;
    }

    /**
     * @return the right operand
     */
    public AbstractNode getRightOperand()
    {
        return rightOperand;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof AbstractBinaryOperatorNode
                && leftOperand.equals(((AbstractBinaryOperatorNode) o).leftOperand)
                && rightOperand.equals(((AbstractBinaryOperatorNode) o).rightOperand));
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;
        hashCode = hashCode * 13 + leftOperand.hashCode();
        hashCode = hashCode * 17 + rightOperand.hashCode();
        return hashCode;
    }
}
