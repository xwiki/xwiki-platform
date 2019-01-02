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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.expression.generics.AbstractValueNode;
import org.xwiki.stability.Unstable;

/**
 * Define a IN operation into a given sub query in a filtering expression.
 *
 * @version $Id$
 * @since 10.8RC1
 * @since 9.11.8
 */
@Unstable
public final class InSubQueryNode extends AbstractOperatorNode
{
    private AbstractValueNode leftOperand;

    private String subQuery;

    private Map<String, Object> parameters;

    /**
     * Construct an IN operation node.
     * @param leftOperand the left operand
     * @param subQuery a list of values
     * @param parameters the named parameters for the sub query
     */
    public InSubQueryNode(AbstractValueNode leftOperand,
            String subQuery, Map<String, Object> parameters)
    {
        this.leftOperand = leftOperand;
        this.subQuery = subQuery;
        this.parameters = parameters;
        if (parameters == null) {
            this.parameters = new HashMap<>();
        }
    }

    /**
     * @return the left operand
     */
    public AbstractNode getLeftOperand()
    {
        return leftOperand;
    }

    /**
     * @return the sub query
     */
    public String getSubQuery()
    {
        return subQuery;
    }

    /**
     * @return the named parameters for the sub query
     */
    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof InSubQueryNode && super.equals(o));
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
        s.append(subQuery);
        s.append(")");
        return s.toString();
    }
}
