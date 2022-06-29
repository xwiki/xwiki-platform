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
package org.xwiki.export.pdf.test.po;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Represents an image from a PDF document.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PDFImage
{
    private final PDImageXObject image;

    /**
     * Creates a new instance wrapping the given PDF image.
     * 
     * @param image the PDF image to wrap
     */
    protected PDFImage(PDImageXObject image)
    {
        this.image = image;
    }

    /**
     * @return the image height
     */
    public int getHeight()
    {
        return this.image.getHeight();
    }

    /**
     * @return the image width
     */
    public int getWidth()
    {
        return this.image.getWidth();
    }
}
