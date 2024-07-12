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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ImagePlugin} class.
 *
 * @version $Id$
 */
@OldcoreTest
class ImagePluginTest
{
    private static final byte[] IMAGE_CONTENT =
        Base64.decodeBase64("iVBORw0KGgoAAAANSUhEUgAAAJYAAAA8CAMAAACzWLNYAAACZFBMVEXUVQD////+"
            + "/fz//v7UVgLVWQbUVgH+/PvWXg3XXw/XYBD+/PrcdC7WXQz89fD78Oj44tT77+fi"
            + "jlXWXg799vHpqn/XYBHdejfWWwn77uXefj3ZaB3ccy3abSTVVwP89O/++/nqrIPW"
            + "XAvWXArvwqPZah/eeznYZBb9+PT33s7bcCj34NHvv5/noHD99vLpqHzpp3v338/d"
            + "ejjijFLZaR7fgEHyzbTxya7klmHjkFjijVTuvp322sfZaiDz0Ln00rznnm7cdTDu"
            + "vJr66+H34NDjkFnfgkTXYhTopXjVWATpqX7bciv12MT23czdeTb++vfVWgfYZBfa"
            + "bCPaayH89O7deDX9+PXtuJTyzLLVWAXjkVrxxqrcdC/12cbXYRL++vj88+3VWgjv"
            + "vp7cdjL34dLfgULvwKHqrIL559v01L/00rv67OPlmmfghEfll2P67eTYZhn56N3o"
            + "pHbww6XppnrtuZb66t/YYxX77ubcdjHyyq/qroXstI7gg0Xz0LjwxKfuvJv44tPZ"
            + "aBzuvZzqqoDghEbkk13ghUjjjlbllmLhiE3opnn22sjklWDnn2/ZZxvrsYrssozj"
            + "j1fzz7f12MXuupjXYhPstI/z0brmnWz88uvefTzdeDTlmWbuu5nii1Hrrobefj7m"
            + "nGvfgEDhh0v56NzhilDrsIjwxqnwwqT228nbciz55trqq4HbcSr99/P9+fboo3Xy"
            + "y7H23Mr45NbbbyfklF/45djhiU7YZRj//v3007378OnabiXlmGTnoXLtt5PyzLPe"
            + "fDr88uztuJXxx6v01cDstpHhiEzbcCnfgkPttpJaT6BrAAAD6klEQVR4Xu2YY5Ms"
            + "SxCG6xmvbdt7bNu2bV3btm3btn3/1O2s7p7G7n6ZihNx4kZXREa+uTk5+0RXdmFU"
            + "NCKLLLLIIovsf2Gp1AWJdVNx/wXDsvfF9a4s4wFXvjrxgLhkd7WvoPrZHb6ofa24"
            + "VHeR/TVXN/sBmhq1q61J5kZVuJWyQkeXMOSoF+Cg+IOsTmQLEhO40SvMlFFluU72"
            + "SFR0BZf5AGYWMFP8GhbmhjUeOHdLCKsKiHVY4jPw5vUIsDEb3Qnb9RMmz3K7YbYH"
            + "UJ5GP/g8KMsN6x6AgYoA1v0Aj4naBUu81gPGZ6N74SFdQ77lFsB9Aao+gc2HEpXb"
            + "WAhwcoevt5YWAMMLvP9tj+RmoFRLl3h0rLuEaq8yw1rxKMCgyIdLpMsaK4H0GXem"
            + "5gWeK41u+Dr0B7GCVIZYqqgP4DY3fPAUwFk72AD73cSlsA6udaKKOL2J0bAeiWkq"
            + "Yyx10SKA6XaQWQxwzM3VwWRbdfTCEFzsJF6W7gtiBajMsdSqYiB9vZ7SuQBHs6mp"
            + "8IbzFGDCimegxg6HYNsoWDM8KmMsdUQ3uTTWLIB53qrZBu/b6hq4RF0OT9rhUTg7"
            + "EmuJj8ocS92BvXytBLg5z0tMgu1a5O0j3qXq4aqEjg/DoRBWmMocS40DOHwaoK7B"
            + "l9gE57SYrnspdQqmSHggTUEyjLUzRGWOpW4FiAPDuwOJFrhS/FPwtKwh2NvJFlis"
            + "QlgulTnWnH5rPKF7/QYASN+uE12SWGaJEyB/6YkR22D552C/dN7zMBjCqnSozLGu"
            + "A6C4S3SmFIC7deIlACqb9EeWW9lX4KTOHIfXLPc4zA9hEaQyx2oYgfUmAMMb9cvw"
            + "lpVd7WaOwduWewfqzxtWoknmqig4iV5CNvB34T2lJsdpzdclH8CHtar5I+IZg0k0"
            + "bvnmdQLwMXziFAxAuVoFx5VBy5stEGKfynT1wQwnXg7TZGp3KWWwQJgtp05zfw69"
            + "7e7LC/sq7BfBYDk12nzETsNgFb7j8mz44gScEW2w+Rhs1WJrYeBL+CpbMB/mFkOP"
            + "aIOt2uBgI1YbIw4t3g3m62+IQ51Ig4ON0TFQrBRglq/gW4CponI4BuZwaN723feh"
            + "Q7NYJ8BSX8UPAG2iDA7NRlcMsR+BEv/dtFryk0SdvytG/RgXMu+OMwX4KVDzM7DJ"
            + "lmNfyPKNsFaOdX1t7fD6ll8CNTuhxZFjX1/HGV1f52zl15GX/d/i/J4tmMYfqUBN"
            + "eyl/ej2wRnfoItr8l/3WeLn4PXSq3EbDXz2j/DTy9z++V3ViJlSzrGa9K5M1tdpX"
            + "bwl+otAu7U4p4/Hv5kPqQhwJx0cWWWSRRRZZZNH4DzmZwO7NW2cKAAAAAElFTkSu"
            + "QmCC");

    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    private ImagePlugin plugin;

    private ImageProcessor imageProcessor;

    @BeforeEach
    void configure() throws Exception
    {
        XWiki xwiki = this.oldCore.getSpyXWiki();
        doReturn("10").when(xwiki).Param("xwiki.plugin.image.cache.capacity");
        doReturn("test").when(xwiki).Param(eq("xwiki.plugin.image.processorHint"),
            ArgumentMatchers.anyString());

        this.oldCore.getMocker().registerMockComponent(CacheManager.class);

        this.imageProcessor = this.oldCore.getMocker().registerMockComponent(ImageProcessor.class, "test");
        when(this.imageProcessor.isMimeTypeSupported("image/png")).thenReturn(true);

        this.plugin = new ImagePlugin("image", ImagePlugin.class.getName(), this.oldCore.getXWikiContext());
    }

    @Test
    void downloadAttachmentWithUnsupportedFileType()
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getMimeType()).thenReturn("image/notsupported");
        when(attachment.getDate()).thenReturn(new Date(0));
        assertSame(attachment, this.plugin.downloadAttachment(attachment, new XWikiContext()));
    }

    @Test
    void downloadAttachmentWhenNull()
    {
        assertNull(this.plugin.downloadAttachment(null, new XWikiContext()));
    }

    @Test
    void cacheOfScaledAttachment() throws Exception
    {
        Date date = new Date(0);

        XWikiContext xcontext = this.oldCore.getXWikiContext();

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getMimeType(xcontext)).thenReturn("image/png");
        InputStream attachmentInputStream = new ByteArrayInputStream(IMAGE_CONTENT);
        when(attachment.getContentInputStream(xcontext)).thenReturn(attachmentInputStream);
        when(attachment.clone()).thenReturn(attachment);
        when(attachment.getDate()).thenReturn(date);

        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(attachment.getAttachment_content()).thenReturn(attachmentContent);
        when(attachmentContent.getContentInputStream()).thenReturn(attachmentInputStream);
        OutputStream attachmentOutputStream = mock(OutputStream.class);
        when(attachmentContent.getContentOutputStream()).thenReturn(attachmentOutputStream);

        CacheManager cacheManager = this.oldCore.getMocker().getInstance(CacheManager.class);
        Cache<Object> imageCache = mock(Cache.class);
        when(cacheManager.createNewLocalCache(ArgumentMatchers.any())).thenReturn(imageCache);

        XWikiServletRequest request = mock(XWikiServletRequest.class);
        when(request.getParameter("width")).thenReturn("30");
        when(request.getParameter("height")).thenReturn("30");
        xcontext.setRequest(request);

        Image image = mock(Image.class);
        when(image.getWidth(null)).thenReturn(400);
        when(image.getHeight(null)).thenReturn(300);
        when(this.imageProcessor.readImage(attachmentInputStream)).thenReturn(image);
        RenderedImage renderedImage = mock(RenderedImage.class);
        when(this.imageProcessor.scaleImage(image, 30, 30)).thenReturn(renderedImage);

        XWikiAttachment scaled = this.plugin.downloadAttachment(attachment, xcontext);

        String cacheKey = "0;null;0;30;30;false;-1.0";
        when(imageCache.get(cacheKey)).thenReturn(scaled);

        // Load again, this time from cache.
        assertSame(scaled, this.plugin.downloadAttachment(attachment, xcontext));

        verify(this.imageProcessor, times(1)).writeImage(renderedImage, "image/png", .5F, attachmentOutputStream);
        verify(imageCache, times(1)).set(cacheKey, attachment);
    }

    @ParameterizedTest
    @CsvSource({
        "100, 500, false, 60, 300",
        "1000, 5000, false, 60, 300",
        "500, 100, false, 400, 80",
        "5000, 1000, false, 400, 80",
        "800, 600, false, 400, 300",
        "100, 100, false, 100, 100",
        "100, 75, true, 100, 75",
        "100, 100, true, 100, 75",
        "120, 75, true, 100, 75",
        "0, 75, true, 100, 75",
        "100, 0, true, 100, 75",
        "0, 75, false, 100, 75",
        "100, 0, false, 100, 75",
        "0, 0, false, 400, 300",
        "0, 0, true, 400, 300",
        "400, 75, true, 100, 75",
        "500, 75, true, 100, 75",
        "100, 300, true, 100, 75",
        "100, 400, true, 100, 75",
        "400, 300, true, 400, 300",
        "500, 400, true, 400, 300"
    })
    void scaling(int width, int height, boolean keepAspectRatio, int expectedWidth, int expectedHeight)
        throws Exception
    {
        Date date = new Date(0);

        XWikiContext xcontext = this.oldCore.getXWikiContext();

        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getMimeType(xcontext)).thenReturn("image/png");
        InputStream attachmentInputStream = new ByteArrayInputStream(IMAGE_CONTENT);
        when(attachment.getContentInputStream(xcontext)).thenReturn(attachmentInputStream);
        when(attachment.clone()).thenReturn(attachment);
        when(attachment.getDate()).thenReturn(date);

        XWikiAttachmentContent attachmentContent = mock(XWikiAttachmentContent.class);
        when(attachment.getAttachment_content()).thenReturn(attachmentContent);
        when(attachmentContent.getContentInputStream()).thenReturn(attachmentInputStream);
        OutputStream attachmentOutputStream = mock(OutputStream.class);
        when(attachmentContent.getContentOutputStream()).thenReturn(attachmentOutputStream);

        CacheManager cacheManager = this.oldCore.getMocker().getInstance(CacheManager.class);
        Cache<Object> imageCache = mock(Cache.class);
        when(cacheManager.createNewLocalCache(ArgumentMatchers.any())).thenReturn(imageCache);

        XWikiServletRequest request = mock(XWikiServletRequest.class);
        when(request.getParameter("width")).thenReturn(Integer.toString(width));
        when(request.getParameter("height")).thenReturn(Integer.toString(height));
        when(request.getParameter("keepAspectRatio")).thenReturn(Boolean.toString(keepAspectRatio));
        xcontext.setRequest(request);

        Image image = mock(Image.class);
        when(image.getWidth(null)).thenReturn(400);
        when(image.getHeight(null)).thenReturn(300);
        when(this.imageProcessor.readImage(attachmentInputStream)).thenReturn(image);
        RenderedImage renderedImage = mock(RenderedImage.class);
        when(this.imageProcessor.scaleImage(eq(image), anyInt(), anyInt())).thenReturn(renderedImage);

        this.plugin.downloadAttachment(attachment, xcontext);
        if (expectedWidth == image.getWidth(null) && expectedHeight == image.getHeight(null)) {
            verify(this.imageProcessor, never()).scaleImage(any(), anyInt(), anyInt());
        } else {
            verify(this.imageProcessor).scaleImage(image, expectedWidth, expectedHeight);
        }
    }
}
