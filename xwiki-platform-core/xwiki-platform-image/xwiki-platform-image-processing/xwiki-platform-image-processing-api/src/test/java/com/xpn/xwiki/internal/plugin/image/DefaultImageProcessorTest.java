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

import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link DefaultImageProcessor}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultImageProcessorTest
{
    private final ReaderOnlyImageReaderSpi readerOnlyImageReaderSpi = new ReaderOnlyImageReaderSpi();

    @InjectMockComponents
    private DefaultImageProcessor imageProcessor;

    @BeforeEach
    void beforeEach()
    {
        this.readerOnlyImageReaderSpi.register();
    }

    @AfterEach
    void afterEach()
    {
        this.readerOnlyImageReaderSpi.deregister();
    }

    @ParameterizedTest
    @ValueSource(strings = { "image/jpeg", "image/png", "image/gif", "image/bmp" })
    void isMimeTypeSupportedWithReaderAndWriter(String mimeType)
    {
        assertTrue(this.imageProcessor.isMimeTypeSupported(mimeType));
    }

    @Test
    void isMimeTypeSupportedWithReaderOnly()
    {
        assertTrue(Arrays.asList(ImageIO.getReaderMIMETypes()).contains(ReaderOnlyImageReaderSpi.MIME_TYPE));

        assertFalse(this.imageProcessor.isMimeTypeSupported(ReaderOnlyImageReaderSpi.MIME_TYPE));
    }

    @Test
    void isMimeTypeSupportedWithoutReader()
    {
        assertFalse(this.imageProcessor.isMimeTypeSupported("image/x-unknown"));
        assertFalse(this.imageProcessor.isMimeTypeSupported(null));
    }
}
