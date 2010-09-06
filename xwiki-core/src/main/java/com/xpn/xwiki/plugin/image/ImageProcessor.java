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
 *
 */
package com.xpn.xwiki.plugin.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

/**
 * Component used to process images.
 * <p>
 * TODO: Make this a real component.
 * 
 * @version $Id$
 */
public class ImageProcessor
{
    /**
     * Reads an image from an input stream.
     * 
     * @param inputStream the input stream to read the image from
     * @return the read image
     * @throws IOException if reading the image fails
     */
    public Image readImage(InputStream inputStream) throws IOException
    {
        return ImageIO.read(inputStream);
    }

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
    public void writeImage(RenderedImage image, String mimeType, float quality, OutputStream out) throws IOException
    {
        if ("image/jpeg".equals(mimeType)) {
            // Find a JPEG writer.
            ImageWriter writer = null;
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByMIMEType(mimeType);
            if (iter.hasNext()) {
                writer = (ImageWriter) iter.next();
            }
            JPEGImageWriteParam iwp = new JPEGImageWriteParam(null);
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(quality);

            // Prepare output file.
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);

            // Write the image.
            writer.write(null, new IIOImage(image, null, null), iwp);

            // Cleanup.
            ios.flush();
            writer.dispose();
            ios.close();
        } else {
            ImageIO.write(image, "png", out);
        }
    }

    /**
     * Scales the given image to the specified dimensions.
     * 
     * @param image the image to be scaled
     * @param width the new image width
     * @param height the new image height
     * @return the scaled image
     */
    public RenderedImage scaleImage(Image image, int width, int height)
    {
        // Draw the given image to a buffered image object and scale it to the new size on-the-fly.
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // We should test the return code here because an exception can be throw but caught.
        if (!graphics2D.drawImage(image, 0, 0, width, height, null)) {
            // Conversion failed.
            throw new RuntimeException("Failed to resize image.");
        }
        return bufferedImage;
    }

    /**
     * @param mimeType the mime type to be checked
     * @return {@code true} if the given mime type is supported, {@code false} otherwise
     */
    public boolean isMimeTypeSupported(String mimeType)
    {
        return Arrays.asList(ImageIO.getReaderMIMETypes()).contains(mimeType);
    }
}
