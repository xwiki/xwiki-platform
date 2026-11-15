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

import org.xwiki.model.reference.DocumentReference;

/**
 * Identifies the image that the chart macro generates: an id computed from the image content, and the document holding
 * the charted data.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ImageId
{
    /**
     * @see #getId()
     */
    private final String id;

    /**
     * @see #getDataDocumentReference()
     */
    private final DocumentReference dataDocumentReference;

    /**
     * @param id the id of the image, computed from its content
     * @param dataDocumentReference the reference of the document holding the data displayed by the chart
     * @since 18.10.0RC1
     * @since 18.4.8
     * @since 17.10.15
     * @since 16.10.20
     */
    public ImageId(String id, DocumentReference dataDocumentReference)
    {
        this.id = id;
        this.dataDocumentReference = dataDocumentReference;
    }

    /**
     * @return the id of the image, used for storing the generated chart image; identical images have the same id
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the reference of the document holding the data displayed by the chart; the image reveals this data, so
     *         it is accessible only to those who can view this document
     * @since 18.10.0RC1
     * @since 18.4.8
     * @since 17.10.15
     * @since 16.10.20
     */
    public DocumentReference getDataDocumentReference()
    {
        return this.dataDocumentReference;
    }
}
