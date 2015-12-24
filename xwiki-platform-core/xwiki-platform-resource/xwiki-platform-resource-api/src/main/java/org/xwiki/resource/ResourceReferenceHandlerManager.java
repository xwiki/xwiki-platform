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
package org.xwiki.resource;

import org.xwiki.component.annotation.Role;

/**
 * The Resource Handler Manager's goal is to locate the right {@link ResourceReferenceHandler}
 * implementations to call in the right order.
 *
 * @param <T> the qualifying element to distinguish a Resource Reference (e.g. Resource Type, Entity Resource Action)
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface ResourceReferenceHandlerManager<T>
{
    /**
     * Handles a passed {@link org.xwiki.resource.ResourceReference}.
     *
     * @param reference the Resource Reference to handle
     * @throws ResourceReferenceHandlerException if an error happens during the Handler execution, for example if no
     *         Handler was found to handle the passed Resource Reference
     */
    void handle(ResourceReference reference) throws ResourceReferenceHandlerException;

    /**
     * Check if there's a Handler for the passed Resource Reference or not.
     *
     * @param resourceReferenceQualifier the qualifying element to distinguish a Resource Reference for which we wish
     *        to check if we can handle it or not (ie we have a Handler available for it). This can be for example the
     *        Resource Type or for an Entity Resource it can be an Entity Resource Action
     * @return true if we have a Handler for the passed Resource Reference qualifier or false otherwise
     * @since 7.1M1
     */
    boolean canHandle(T resourceReferenceQualifier);
}
