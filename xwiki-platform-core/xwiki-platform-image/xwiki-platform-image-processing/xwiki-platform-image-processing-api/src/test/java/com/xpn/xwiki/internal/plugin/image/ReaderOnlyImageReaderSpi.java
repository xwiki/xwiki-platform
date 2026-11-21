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

import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * An {@link ImageReaderSpi} for a fake image format that ImageIO can read but not write, to be registered in the
 * default {@link IIORegistry} for the duration of a test.
 *
 * @version $Id$
 */
class ReaderOnlyImageReaderSpi extends ImageReaderSpi
{
    /**
     * The media type of the fake image format.
     */
    static final String MIME_TYPE = "image/x-xwiki-reader-only";

    ReaderOnlyImageReaderSpi()
    {
        super("XWiki", "1.0", new String[] { "xwiki-reader-only" }, new String[] { "xro" },
            new String[] { MIME_TYPE }, "com.xpn.xwiki.internal.plugin.image.ReaderOnlyImageReader",
            new Class<?>[] { ImageInputStream.class }, null, false, null, null, null, null, false, null, null, null,
            null);
    }

    @Override
    public boolean canDecodeInput(Object source)
    {
        return false;
    }

    @Override
    public ImageReader createReaderInstance(Object extension)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription(Locale locale)
    {
        return "Reader-only test image format";
    }

    /**
     * Registers this provider in the default {@link IIORegistry}.
     */
    void register()
    {
        IIORegistry.getDefaultInstance().registerServiceProvider(this, ImageReaderSpi.class);
    }

    /**
     * Removes this provider from the default {@link IIORegistry}.
     */
    void deregister()
    {
        IIORegistry.getDefaultInstance().deregisterServiceProvider(this, ImageReaderSpi.class);
    }
}
