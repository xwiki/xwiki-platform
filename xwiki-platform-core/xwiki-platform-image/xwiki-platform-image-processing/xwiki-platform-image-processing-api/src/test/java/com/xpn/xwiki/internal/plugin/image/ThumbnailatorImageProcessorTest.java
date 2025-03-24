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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link ThumbnailatorImageProcessor}.
 *
 * @version $Id$
 */
@ComponentTest
class ThumbnailatorImageProcessorTest
{
    @InjectMockComponents
    private ThumbnailatorImageProcessor imageProcessor;

    @Test
    void readImage() throws IOException
    {
        // Example image from https://github.com/recurser/exif-orientation-examples,
        // Copyright (c) 2010 Dave Perrett, http://recursive-design.com/, licensed under the MIT license.
        // Original photo by John Salvino https://unsplash.com/photos/1PPpwrTNkJI
        try (InputStream imageInput = getClass().getResourceAsStream("/Portrait_5.jpg")) {
            Image image = this.imageProcessor.readImage(imageInput);
            assertEquals(1200, image.getWidth(null));
            assertEquals(1800, image.getHeight(null));
        }
    }

    @Test
    void changeAspectRatio()
    {
        Image image = createImage(20, 20, new Color(37, 220, 182));
        RenderedImage scaledImage = this.imageProcessor.scaleImage(image, 2, 10);
        assertEquals(2, scaledImage.getWidth());
        assertEquals(10, scaledImage.getHeight());
    }

    private static BufferedImage createImage(int width, int height, Color color)
    {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }
}
