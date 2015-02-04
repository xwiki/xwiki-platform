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
package org.xwiki.skin;

import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 7.0M1
 */
@Unstable
public interface ResourceRepository
{
    /**
     * @return the parent of the resource repository used to fallback
     */
    ResourceRepository getParent();

    /**
     * @return the identifier of the repository
     */
    String getId();

    /**
     * Get the resource associated to the provided name. If none is found in the resource repository fallback on parent
     * repository.
     * 
     * @param resourceName the name of the resource to search
     * @return the found resource, null if none could be found
     */
    Resource<?> getResource(String resourceName);

    /**
     * Get the resource associated to the provided name. Does not fallback on parent repository.
     * 
     * @param resourceName the name of the resource to search
     * @return the found resource, null if none could be found
     */
    Resource<?> getLocalResource(String resourceName);
}
