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

import java.util.Collection;

import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;

/**
 * Define a IN operation in a filtering expression.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public final class InNode extends AbstractOperatorNode
{
    private AbstractValueNode leftOperand;

    private Collection<AbstractValueNode> values;

    /**
     * Construct an IN operation node.
     * @param leftOperand the left operand
     * @param values a list of values
     */
    public InNode(AbstractValueNode leftOperand,
            Collection<AbstractValueNode> values)
    {
        this.leftOperand = leftOperand;
        this.values = values;
    }

    /**
     * @return the left operand
     */
    public AbstractNode getLeftOperand()
    {
        return leftOperand;
    }

    /**
     * @return the values
     */
    public Collection<AbstractValueNode> getValues()
    {
        return values;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof InNode && super.equals(o));
    }

    @Override
    public int hashCode()
    {
        return this.getClass().getTypeName().hashCode() * 571 + super.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder(leftOperand.toString());
        s.append(" IN (");
        String separator = "";
        for (AbstractValueNode value : values) {
            s.append(separator);
            s.append(value.toString());
            separator = ", ";
        }
        s.append(")");
        return s.toString();
    }
}
