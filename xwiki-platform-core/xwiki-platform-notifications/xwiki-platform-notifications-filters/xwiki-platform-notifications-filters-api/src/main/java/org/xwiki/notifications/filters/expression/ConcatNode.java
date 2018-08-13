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

import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.stability.Unstable;

/**
 * Define a CONCAT condition in a filtering expression.
 *
 * @version $Id$
 * @since 10.8RC1
 * @since 9.11.8
 */
@Unstable
public final class ConcatNode extends AbstractValueNode<AbstractNode>
{
    private AbstractNode rightOperand;

    /**
     * Constructs a new CONCAT node.
     *
     * @param leftOperand the left CONCAT operand
     * @param rightOperand the right CONCAT operand
     */
    public ConcatNode(AbstractNode leftOperand, AbstractNode rightOperand)
    {
        super(leftOperand);
        this.rightOperand = rightOperand;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof ConcatNode && super.equals(o));
    }

    @Override
    public int hashCode()
    {
        return this.getClass().getTypeName().hashCode() * 571 + super.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("CONCAT(%s, %s)", getLeftOperand(), getRightOperand());
    }

    /**
     * @return the left operand
     */
    public AbstractNode getLeftOperand()
    {
        // It's a trick to not have a useless left operand field
        return getContent();
    }

    /**
     * @return the right operand
     */
    public AbstractNode getRightOperand()
    {
        return rightOperand;
    }
}
