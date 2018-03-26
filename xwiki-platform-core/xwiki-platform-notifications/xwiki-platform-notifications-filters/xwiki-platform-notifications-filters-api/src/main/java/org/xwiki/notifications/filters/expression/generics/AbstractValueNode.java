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

import java.util.Collection;
import java.util.stream.Collectors;

import org.xwiki.notifications.filters.expression.EndsWith;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.GreaterThanNode;
import org.xwiki.notifications.filters.expression.InNode;
import org.xwiki.notifications.filters.expression.LesserThanNode;
import org.xwiki.notifications.filters.expression.NotEqualsNode;
import org.xwiki.notifications.filters.expression.StartsWith;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.stability.Unstable;

/**
 * Define a node that contains a specific value accessible with {@link AbstractValueNode#getContent()}.
 *
 * @param <T> the type of the contained value
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Unstable
public abstract class AbstractValueNode<T> extends AbstractNode
{
    private T content;

    /**
     * Constructs a new Value Node.
     *
     * @param content the node content
     */
    public AbstractValueNode(T content)
    {
        if (content == null) {
            throw new NullPointerException("A value node should have its content defined.");
        }

        this.content = content;
    }

    /**
     * Helper method used to easily create expressions such as "VALUE1 = VALUE2" without having to instantiate new
     * objects. Note that the method is called "eq" in order not to be confused with the standard
     * {@link #equals(Object)}.
     *
     * @param node the node that will be the second operand of the equals
     * @return an {@link EqualsNode} that has the current object as the fist operand, and the parameter as the second
     * operand
     *
     * @since 9.8RC1
     */
    public EqualsNode eq(AbstractValueNode node)
    {
        return new EqualsNode(this, node);
    }

    /**
     * Just as {@link #eq(AbstractValueNode)}, this method is a helper to create expressions without having to
     * instantiate new objects.
     *
     * @param node the node that will be the second operand of the "not equals" node
     * @return a {@link NotEqualsNode} composed of the current object as the first operand and the parameter as
     * the second operand
     *
     * @since 9.8RC1
     */
    public NotEqualsNode notEq(AbstractValueNode node)
    {
        return new NotEqualsNode(this, node);
    }

    /**
     * Helper that allows to create {@link StartsWith} without having to instantiate new objects.
     *
     * @param node the node that will be the second operand of the "like" node
     * @return a {@link StartsWith} where the current object is the first operand and the parameter is the second operand
     *
     * @since 9.8RC1
     */
    public StartsWith startsWith(AbstractValueNode node)
    {
        return new StartsWith(this, node);
    }

    /**
     * Helper that allows to create {@link EndsWith} without having to instantiate new objects.
     *
     * @param node the node that will be the second operand of the "like" node
     * @return a {@link StartsWith} where the current object is the first operand and the parameter is the second operand
     *
     * @since 10.2
     * @since 9.11.4
     */
    public EndsWith endsWith(AbstractValueNode node)
    {
        return new EndsWith(this, node);
    }

    /**
     * Helper that allows to create {@link GreaterThanNode} without having to instantiate new objects.
     *
     * @param node the node that will be the second operand of the "&gt;=" node
     * @return a {@link GreaterThanNode} where the current object is the first operand and the parameter is the second
     * operand
     *
     * @since 9.9RC1
     */
    public GreaterThanNode greaterThan(AbstractValueNode node)
    {
        return new GreaterThanNode(this, node);
    }

    /**
     * Helper that allows to create {@link LesserThanNode} without having to instantiate new objects.
     *
     * @param node the node that will be the second operand of the "&lt;=" node
     * @return a {@link LesserThanNode} where the current object is the first operand and the parameter is the second
     * operand
     *
     * @since 9.9RC1
     */
    public LesserThanNode lesserThan(AbstractValueNode node)
    {
        return new LesserThanNode(this, node);
    }

    /**
     * Helper that allows to create {@link InNode} without having to instantiate new objects.
     *
     * @param values a collection of nodes
     * @return a {@link InNode} where the current object is the first operand and the parameter is the second
     * operand
     *
     * @since 9.10RC1
     */
    public InNode in(Collection<AbstractValueNode> values)
    {
        return new InNode(this, values);
    }

    /**
     * Helper that allows to create {@link InNode} without having to instantiate new objects.
     *
     * @param values a collection of string
     * @return a {@link InNode} where the current object is the first operand and the parameter is the second
     * operand
     *
     * @since 9.10RC1
     */
    public InNode inStrings(Collection<String> values)
    {
        return in(values.stream().map(s -> new StringValueNode(s)).collect(Collectors.toList()));
    }

    /**
     * @return the node content
     */
    public T getContent()
    {
        return content;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o instanceof AbstractValueNode
                && content.equals(((AbstractValueNode) o).content));
    }

    @Override
    public int hashCode()
    {
        return 13 + content.hashCode();
    }
}
