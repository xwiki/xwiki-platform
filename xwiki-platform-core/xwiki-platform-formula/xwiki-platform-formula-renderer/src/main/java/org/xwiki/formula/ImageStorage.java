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
package org.xwiki.formula;

import org.xwiki.component.annotation.Role;

/**
 * A component for storing generated images for later retrieval in a subsequent request.
 * 
 * @version $Id$
 */
@Role
public interface ImageStorage
{
    /**
     * Retrieve the image with the given identifier.
     * 
     * @param id the identifier of the data in the storage
     * @return an {@link ImageData} instance, or {@code null} if no image is stored under this identifier
     */
    ImageData get(String id);

    /**
     * Store the image under the given identifier.
     * 
     * @param id the identifier of the data in the storage
     * @param data the image to be stored
     */
    void put(String id, ImageData data);
}
