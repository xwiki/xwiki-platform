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
package org.xwiki.action.internal;

import java.util.Collection;
import java.util.Stack;

import org.xwiki.action.Action;
import org.xwiki.action.ActionChain;
import org.xwiki.action.ActionException;
import org.xwiki.resource.Resource;

/**
 * Default chain implementation using a Stack.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultActionChain implements ActionChain
{
    /**
     * Contains all remaining Actions to execute with Actions on top executing first.
     */
    private Stack<Action> actionStack;

    /**
     * @param orderedActions the sorted list of Action to execute
     */
    public DefaultActionChain(Collection<Action> orderedActions)
    {
        this.actionStack = new Stack<Action>();
        this.actionStack.addAll(orderedActions);
    }

    @Override
    public void executeNext(Resource resource) throws ActionException
    {
        this.actionStack.pop().execute(resource, this);
    }
}
