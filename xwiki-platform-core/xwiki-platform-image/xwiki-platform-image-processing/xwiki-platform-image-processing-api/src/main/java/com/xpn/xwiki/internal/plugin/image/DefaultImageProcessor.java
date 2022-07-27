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
package com.xpn.xwiki.internal.plugin.image;

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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.plugin.image.ImageProcessor;

/**
 * Default {@link ImageProcessor} implementation.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Singleton
public class DefaultImageProcessor implements ImageProcessor
{
    @Override
    public Image readImage(InputStream inputStream) throws IOException
    {
        return ImageIO.read(inputStream);
    }

    @Override
    public void writeImage(RenderedImage image, String mimeType, float quality, OutputStream out) throws IOException
    {
        if ("image/jpeg".equals(mimeType)) {
            // Find a JPEG writer.
            ImageWriter writer = null;
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByMIMEType(mimeType);
            if (iter.hasNext()) {
                writer = iter.next();
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

    @Override
    public RenderedImage scaleImage(Image image, int width, int height)
    {
        // Draw the given image to a buffered image object and scale it to the new size on-the-fly.
        int imageType = getBestImageTypeFor(image);
        BufferedImage bufferedImage = new BufferedImage(width, height, imageType);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        // We should test the return code here because an exception can be throw but caught.
        if (!graphics2D.drawImage(image, 0, 0, width, height, null)) {
            // Conversion failed.
            throw new RuntimeException("Failed to resize image.");
        }
        return bufferedImage;
    }

    @Override
    public boolean isMimeTypeSupported(String mimeType)
    {
        try {
            return Arrays.asList(ImageIO.getReaderMIMETypes()).contains(mimeType);
        } catch (NoClassDefFoundError e) {
            // Happens on certain systems where the javax.imageio package is not available.
            return false;
        }
    }

    protected int getBestImageTypeFor(Image image)
    {
        int imageType = BufferedImage.TYPE_4BYTE_ABGR;
        if (image instanceof BufferedImage) {
            imageType = ((BufferedImage) image).getType();
            if (imageType == BufferedImage.TYPE_BYTE_INDEXED || imageType == BufferedImage.TYPE_BYTE_BINARY
                || imageType == BufferedImage.TYPE_CUSTOM) {
                // INDEXED and BINARY: GIFs or indexed PNGs may lose their transparent bits, for safety revert to ABGR.
                // CUSTOM: Unknown image type, fall back on ABGR.
                imageType = BufferedImage.TYPE_4BYTE_ABGR;
            }
        }
        return imageType;
    }
}
