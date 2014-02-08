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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.action.Action;
import org.xwiki.action.ActionChain;
import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;
import org.xwiki.resource.Resource;

/**
 * For any passed Resource, find the correct {@link Action} to execute by sorting them by priority.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class DefaultActionManager implements ActionManager
{
    @Inject
    private Provider<List<Action>> actionProvider;

    @Override
    public boolean execute(Resource resource) throws ActionException
    {
        boolean result;

        // Look for an Action supporting the action located in the passed Resource object.
        // TODO: Use caching to avoid having to sort all Actions at every call.
        Set<Action> orderedActions = new TreeSet<Action>();
        for (Action action : this.actionProvider.get()) {
            if (action.getSupportedActionIds().contains(resource.getActionId())) {
                orderedActions.add(action);
            }
        }

        if (!orderedActions.isEmpty()) {
            // Create the Action chain
            ActionChain chain = new DefaultActionChain(orderedActions);

            // Call the first Action
            chain.executeNext(resource);

            result = true;
        } else {
            // Resource has not been handled since no Action was found for it!
            result = false;
        }

        return result;
    }
}
