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
package org.xwiki.ratings;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Allow to create dedicated instances of {@link RatingsManager} for any rating usage.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Role
public interface RatingsManagerFactory
{
    /**
     * The hint of the Ratings Manager to use for the default Ratings Application.
     */
    String DEFAULT_APP_HINT = "ratings";

    /**
     * Create or retrieve an instance of {@link RatingsManager} for the given managerName.
     * If the instance needs to be created, the {@link RatingsConfiguration} based on this managerName will be used to
     * create it. If there is no instance of {@link RatingsConfiguration} matching the given managerName, the default
     * implementation will be used.
     *
     * @param managerName an managerName of an instance to create or retrieve.
     * @return a {@link RatingsManager} identified with the given managerName.
     * @throws RatingsException in case of problem when creating or retrieving the component.
     */
    RatingsManager getRatingsManager(String managerName) throws RatingsException;

    /**
     * Retrieve the list of instantiated managers from the factory.
     *
     * @return the list of {@link RatingsManager} instantiated through the factory.
     * @throws RatingsException in case of problem when retrieving the managers.
     */
    List<RatingsManager> getInstantiatedManagers() throws RatingsException;
}
