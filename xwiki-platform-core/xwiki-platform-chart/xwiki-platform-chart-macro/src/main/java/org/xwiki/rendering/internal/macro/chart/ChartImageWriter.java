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

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Writes an Image represented as an array of Bytes to storage. Also provides a helper method to get an XWiki URL to
 * access the written data.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Role
public interface ChartImageWriter
{
    /**
     * Writes the image to storage.
     *
     * @param imageId the image id that we use to generate a unique storage location
     * @param imageData the image to store
     * @throws MacroExecutionException if an error happened when computing the location or saving the image
     */
    void writeImage(ImageId imageId, byte[] imageData) throws MacroExecutionException;

    /**
     * Compute the URL to use to access the stored generate chart image.
     *
     * @param imageId the image id for the image that we have stored
     * @return the URL to use to access the stored generate chart image
     * @throws MacroExecutionException if an error happened when computing the URL (eg if the current wiki cannot be
     *         computed)
     */
    String getURL(ImageId imageId) throws MacroExecutionException;
}
