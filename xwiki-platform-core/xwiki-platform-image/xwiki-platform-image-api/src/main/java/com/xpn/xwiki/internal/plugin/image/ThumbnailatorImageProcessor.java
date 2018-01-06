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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import net.coobird.thumbnailator.Thumbnails;

/**
 * Image processor implementation based on the Thumbnailator library.
 * 
 * @version $Id$
 * @since 8.4.4
 * @since 9.0RC1
 */
@Component
@Singleton
@Named("thumbnailator")
public class ThumbnailatorImageProcessor extends DefaultImageProcessor
{
    @Override
    public void writeImage(RenderedImage image, String mimeType, float quality, OutputStream out) throws IOException
    {
        if (image instanceof BufferedImage) {
            Thumbnails.of((BufferedImage) image).scale(1).outputFormat(getFormatNameForMimeType(mimeType))
                .outputQuality(quality).toOutputStream(out);
        } else {
            super.writeImage(image, mimeType, quality, out);
        }
    }

    @Override
    public RenderedImage scaleImage(Image image, int width, int height)
    {
        if (image instanceof BufferedImage) {
            try {
                return Thumbnails.of((BufferedImage) image).size(width, height).asBufferedImage();
            } catch (IOException e) {
            }
        }
        return super.scaleImage(image, width, height);
    }

    private String getFormatNameForMimeType(String mimeType)
    {
        Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mimeType);
        if (imageReaders.hasNext()) {
            try {
                return imageReaders.next().getFormatName();
            } catch (IOException e) {
            }
        }
        return mimeType;
    }
}
