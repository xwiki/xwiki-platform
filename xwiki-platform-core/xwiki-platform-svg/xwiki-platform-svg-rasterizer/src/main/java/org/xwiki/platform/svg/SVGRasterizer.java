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
package org.xwiki.platform.svg;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for working with SVG images.
 *
 * @version $Id$
 * @since 8.0M1
 */
@Role
public interface SVGRasterizer
{
    /**
     * Rasterize an image as PNG into a temporary file.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return the file where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    File rasterizeToTemporaryFile(String content, int width, int height) throws IOException;

    /**
     * Rasterize an image as PNG into a temporary resource belonging to the current document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return the temporary resource where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height) throws IOException;

    /**
     * Rasterize an image as PNG into a temporary resource belonging to the specified document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @param targetContext the document which will "own" the new temporary resource
     * @return the temporary resource where the PNG is stored
     * @throws IOException if temporary files can't be accessed
     */
    TemporaryResourceReference rasterizeToTemporaryResource(String content, int width, int height,
        DocumentReference targetContext) throws IOException;

    /**
     * Rasterize an image as PNG into the current response.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @throws IOException if writing the response fails
     */
    void rasterizeToResponse(String content, int width, int height) throws IOException;
}
