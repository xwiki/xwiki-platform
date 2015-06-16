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
package org.xwiki.platform.flavor;

import org.xwiki.component.annotation.Role;
import org.xwiki.extension.ExtensionId;

/**
 * Component that filter the flavors the user can install.
 *  
 * @since 7.2M1  
 * @version $Id$
 */
@Role
public interface FlavorFilter
{
    /**
     * @param flavor the flavor to test.
     * @return wether or not the user is authorized to install the flavor
     * @throws FlavorManagerException if problem occur 
     */
    boolean isFlavorAuthorized(ExtensionId flavor) throws FlavorManagerException;

    /**
     * Add a filter to the query to only find the authorized flavors.
     * @param flavorQuery the query where the filters will be added
     * @throws FlavorManagerException if problem occur
     */
    void addFilterToQuery(FlavorQuery flavorQuery) throws FlavorManagerException;
}
