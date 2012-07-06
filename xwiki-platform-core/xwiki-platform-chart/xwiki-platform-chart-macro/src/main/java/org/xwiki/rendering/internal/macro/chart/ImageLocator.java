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
package org.xwiki.rendering.internal.macro.chart;

import java.io.File;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Returns location for the generated chart image, both for where it's stored in the store and the URL to retrieve it.
 *
 * @version $Id$
 * @since 4.2M1
 */
@Role
public interface ImageLocator
{
    /**
     * Compute the location where to store the generated chart image.
     *
     * @param imageId the image id that we use to generate a unique storage location
     * @return the location where to store the generated chart image
     * @throws MacroExecutionException if an error happened when computing the location
     */
    File getStorageLocation(ImageId imageId) throws MacroExecutionException;

    /**
     * Compute the URL to use to access the stored generate chart image.
     *
     * @param imageId the image id for the image that we have stored
     * @return the URL to use to access the stored generate chart image
     */
    String getURL(ImageId imageId);
}
