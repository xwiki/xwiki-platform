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
package com.xpn.xwiki.plugin.image;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xwiki.component.annotation.Role;

/**
 * Component used to process images.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Role
public interface ImageProcessor
{
    /**
     * Reads an image from an input stream.
     *
     * @param inputStream the input stream to read the image from
     * @return the read image
     * @throws IOException if reading the image fails
     */
    Image readImage(InputStream inputStream) throws IOException;

    /**
     * Encodes the given image to match the specified mime type, if possible, and writes it to the output stream, using
     * the specified compression quality if appropriate.
     *
     * @param image the image to be written to the output stream
     * @param mimeType the image mime type (e.g. (e.g. "image/jpeg" or "image/png")
     * @param quality the compression quality; use this parameter to reduce the size, i.e. the number of bytes, of the
     *            image
     * @param out the output stream to write the image to
     * @throws IOException if writing the image fails
     */
    void writeImage(RenderedImage image, String mimeType, float quality, OutputStream out) throws IOException;

    /**
     * Scales the given image to the specified dimensions.
     *
     * @param image the image to be scaled
     * @param width the new image width
     * @param height the new image height
     * @return the scaled image
     */
    RenderedImage scaleImage(Image image, int width, int height);

    /**
     * @param mimeType the mime type to be checked
     * @return {@code true} if the given mime type is supported, {@code false} otherwise
     */
    boolean isMimeTypeSupported(String mimeType);
}
