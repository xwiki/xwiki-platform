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
package org.xwiki.uiextension;

import java.util.List;
import java.util.Optional;

import org.xwiki.component.annotation.Role;

/**
 * A UIExtensionManager retrieves extensions for a given extension point.
 *
 * @version $Id$
 * @since 4.3.1
 */
@Role
public interface UIExtensionManager
{
    /**
     * Retrieves all the {@link UIExtension}s for a given Extension Point.
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    List<UIExtension> get(String extensionPointId);

    /**
     * Get the UI extension from the id.
     * 
     * @param id the identifier of the UI extension
     * @return the UI extension
     * @since 15.1RC1
     * @since 14.10.5
     * @since 14.4.4
     */
    default Optional<UIExtension> getUIExtension(String id)
    {
        return Optional.empty();
    }
}
