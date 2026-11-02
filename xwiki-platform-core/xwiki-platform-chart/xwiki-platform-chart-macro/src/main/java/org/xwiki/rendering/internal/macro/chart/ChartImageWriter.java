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
import org.xwiki.model.reference.DocumentReference;
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
     * Writes the image to storage, unless an identical image of the same document is already stored.
     *
     * @param imageData the image to store
     * @param dataDocumentReference the reference of the document holding the data displayed by the chart
     * @return the id of the stored image
     * @throws MacroExecutionException if an error happened when computing the location or saving the image
     * @since 18.10.0RC1
     * @since 18.4.8
     * @since 17.10.15
     * @since 16.10.20
     */
    ImageId writeImage(byte[] imageData, DocumentReference dataDocumentReference) throws MacroExecutionException;

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
