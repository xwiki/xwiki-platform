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

import org.xwiki.component.annotation.Role;
import org.xwiki.resource.Resource;
import org.xwiki.stability.Unstable;

/**
 * The Action Manager's goal is to locate the right {@link Action} implementations to call in the right order.
 *
 * @version $Id$
 * @since 6.0M1
 */
@Role
@Unstable
public interface ActionManager
{
    /**
     * Executes a passed {@link org.xwiki.resource.Resource}.
     *
     * @param resource the resource and action to handle
     * @return true if the Resource was handled by an {@link org.xwiki.action.Action} or false otherwise
     * @throws ActionException if an error happened during the execution of the action
     */
    boolean execute(Resource resource) throws ActionException;
}
