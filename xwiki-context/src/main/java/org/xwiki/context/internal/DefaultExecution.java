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
 *
 */
package org.xwiki.context.internal;

import java.util.Stack;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * Holds the Execution Context object. Note that we require this Execution component since we want to be able to pass
 * the Execution Context to singleton components. Thus this holder is a singleton itself and the Execution Context is
 * saved as a ThreadLocal variable. 
 *
 * @version $Id$
 * @since 1.5M2
 */
@Component
public class DefaultExecution implements Execution
{
    private ThreadLocal<Stack<ExecutionContext>> context =
        new ThreadLocal<Stack<ExecutionContext>>();

    /**
     * {@inheritDoc}
     * @see Execution#pushContext(ExecutionContext)
     */
    public void pushContext(ExecutionContext context)
    {
        this.context.get().push(context);
    }

    /**
     * {@inheritDoc}
     * @see Execution#popContext()
     */
    public void popContext()
    {
        this.context.get().pop();
    }

    /**
     * {@inheritDoc}
     * @see Execution#getContext()
     */
    public ExecutionContext getContext()
    {
        Stack<ExecutionContext> stack = this.context.get();
        return stack == null ? null : stack.peek();
    }

    /**
     * {@inheritDoc}
     * @see Execution#setContext(ExecutionContext)
     */
    public void setContext(ExecutionContext context)
    {
        Stack<ExecutionContext> stack = new Stack<ExecutionContext>();
        stack.push(context);
        this.context.set(stack);
    }

    /**
     * {@inheritDoc}
     * @see Execution#removeContext()
     */
    public void removeContext()
    {
        this.context.remove();
    }
}
