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

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Handles a given {@link ResourceReference}.
 *
 * @param <T> the qualifying element to specify what Resource Reference are handled by thus Handler (e.g. Resource Type,
 *            Entity Resource Action)
 * @param <T> the type of supported items
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface ResourceReferenceHandler<T> extends Comparable<ResourceReferenceHandler>
{
    /**
     * The priority of execution relative to the other Handlers. The lowest values have the highest priorities and
     * execute first. For example a Handler with a priority of 100 will execute before one with a priority of 500.
     *
     * @return the execution priority
     */
    int getPriority();

    /**
     * @return the list of qualifying Resource References elements supported by this Handler (e.g Resource Type, Entity
     *         Resource Action)
     */
    List<T> getSupportedResourceReferences();

    /**
     * Executes the Handler on the passed Resource Reference.
     *
     * @param reference the Resource Reference to handle
     * @param chain the Handler execution chain, needed to tell the next Handler in the chain to execute (similar to the
     *            Filter Chain in the Servlet API)
     * @throws ResourceReferenceHandlerException if an error happens during the Handler execution
     */
    void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException;
}
