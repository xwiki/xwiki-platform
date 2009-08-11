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

import java.io.Serializable;

import org.xwiki.formula.FormulaRenderer.Type;

/**
 * Rendered image.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public class ImageData implements Serializable
{
    /** Unique version number used for serialization. */
    private static final long serialVersionUID = 200908111440L;

    /** Binary image data. */
    protected final byte[] data;

    /** Image format. */
    protected final Type type;

    /**
     * Simple constructor which initializes the binary image data and the image format.
     * 
     * @param data the binary image data. See {@link #data}.
     * @param type the image format. See {@link #type}.
     */
    public ImageData(byte[] data, Type type)
    {
        this.data = data;
        this.type = type;
    }

    /**
     * Access to the image data.
     * 
     * @return the binary image data
     * @see #data
     */
    public byte[] getData()
    {
        return this.data;
    }

    /**
     * Access to the image format.
     * 
     * @return the image format
     * @see #type
     */
    public Type getType()
    {
        return this.type;
    }

    /**
     * Quick access to the image MIME type.
     * 
     * @return the MIME type corresponding to the stored image, in the format defined in RFC 2045
     * @see #type
     */
    public String getMimeType()
    {
        return this.type.getMimetype();
    }
}
