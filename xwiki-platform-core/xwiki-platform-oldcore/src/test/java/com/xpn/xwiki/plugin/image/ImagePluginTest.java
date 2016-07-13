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
import java.io.ByteArrayInputStream;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.plugin.image.ImageProcessor;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.Utils;
import org.apache.commons.codec.binary.Base64;
import org.jmock.Mock;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.image.ImagePlugin} class.
 * 
 * @version $Id$
 */
public class ImagePluginTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final byte[] testPngImageContent = Base64.decodeBase64(
          "iVBORw0KGgoAAAANSUhEUgAAAJYAAAA8CAMAAACzWLNYAAACZFBMVEXUVQD////+"
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

    private ImagePlugin plugin;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Mock mockXWiki = mock(XWiki.class);
        mockXWiki.stubs().method("Param").will(returnValue("10"));
        getContext().setWiki((XWiki) mockXWiki.proxy());
        this.plugin = new ImagePlugin("image", ImagePlugin.class.getName(), getContext());
    }

    public void testDownloadAttachmentWithUnsupportedFileType()
    {
        Mock attachmentMock = mock(XWikiAttachment.class);
        attachmentMock.stubs().method("getMimeType").will(returnValue("image/notsupported"));
        XWikiAttachment attachment = (XWikiAttachment) attachmentMock.proxy();
        assertSame(attachment, plugin.downloadAttachment(attachment, new XWikiContext()));
    }

    public void testCacheOfScaledAttachment() throws Exception
    {
        XWikiAttachment attach = (new XWikiAttachment() {
            @Override
            public XWikiAttachment clone() {
                XWikiAttachment out = new XWikiAttachment();
                out.setAttachment_content(new XWikiAttachmentContent(this.getAttachment_content()));
                return out;
            }
            @Override
            public String getMimeType(XWikiContext context) { return "image/png"; }
        });
        attach.setContent(new ByteArrayInputStream(testPngImageContent));
        XWikiServletRequest req = (new XWikiServletRequest(null) {
            @Override
            public String getParameter(String prop) { return "30"; }
        });
        this.getContext().setRequest(req);

        final XWikiAttachment scaled = plugin.downloadAttachment(attach, this.getContext());

        // Check that the width is indeed 30
        ImageProcessor imageProcessor = Utils.getComponent(ImageProcessor.class);
        Image scaledImage = imageProcessor.readImage(scaled.getContentInputStream(this.getContext()));
        assertEquals(30, scaledImage.getWidth(null));

        // Load the scaled attachment again and make sure it's the same object in memory.
        final XWikiAttachment cached = plugin.downloadAttachment(attach, this.getContext());
        assertTrue(scaled == cached);
        assertTrue(scaled.getAttachment_content() == cached.getAttachment_content());
    }
}
