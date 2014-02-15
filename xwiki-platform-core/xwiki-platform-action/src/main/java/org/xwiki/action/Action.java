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
package org.xwiki.action;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.ActionId;
import org.xwiki.resource.Resource;
import org.xwiki.stability.Unstable;

/**
 * Executes a given {@link org.xwiki.resource.Resource}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@Role
@Unstable
public interface Action extends Comparable<Action>
{
    /**
     * The priority of execution relative to the other Actions. The lowest values have the highest priorities and
     * execute first. For example a Action with a priority of 100 will execute before one with a priority of 500.
     *
     * @return the execution priority
     */
    int getPriority();

    /**
     * @return the list of Action Ids supported by this Action
     */
    List<ActionId> getSupportedActionIds();

    /**
     * Executes the Action on the passed Resource.
     *
     * @param resource the Resource on which to execute the Action
     * @param chain the Action execution chain, needed to tell the next Action in the chain to execute (similar to the
     *        Filter Chain in the Servlet API)
     * @throws ActionException if an error happens during the Action execution
     */
    void execute(Resource resource, ActionChain chain) throws ActionException;
}
