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
package org.xwiki.notifications.notifiers.internal.email;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;
import javax.inject.Provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserAvatarAttachmentExtractor}.
 *
 * @version $Id$
 */
@ComponentTest
class UserAvatarAttachmentExtractorTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "XWiki", "Alice");

    private static final String AVATAR_FILE_NAME = "avatar";

    private static final int SIZE = 32;

    @InjectMockComponents
    private UserAvatarAttachmentExtractor extractor;

    @MockComponent
    private Environment environment;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @XWikiTempDir
    private File temporaryDirectory;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext xcontext;

    private XWikiDocument userProfileDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        // The attachment content holding the resized avatar is stored in a temporary file of the environment.
        Utils.setComponentManager(this.componentManager);
        when(this.environment.getTemporaryDirectory()).thenReturn(this.temporaryDirectory);

        // The default avatar is red, so that it can be distinguished from the (blue) avatars of the users.
        when(this.environment.getResourceAsStream("/resources/icons/xwiki/noavatar.png"))
            .thenAnswer(invocation -> new ByteArrayInputStream(createImage(Color.RED, "png")));

        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        this.userProfileDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(USER_REFERENCE, this.xcontext)).thenReturn(this.userProfileDocument);
        when(this.userProfileDocument.getStringValue(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE,
            "avatar")).thenReturn(AVATAR_FILE_NAME);
    }

    @AfterEach
    void tearDown()
    {
        Utils.setComponentManager(null);
    }

    @Test
    void getUserAvatarWithSupportedFormat() throws Exception
    {
        mockAvatar("image/png", createImage(Color.BLUE, "png"));

        Attachment avatar = this.extractor.getUserAvatar(USER_REFERENCE, SIZE);

        assertEquals("Alice.jpg", avatar.getFilename());
        assertAvatar(Color.BLUE, avatar);
    }

    @Test
    void getUserAvatarWithUnsupportedFormat() throws Exception
    {
        // ImageIO has no reader for SVG images.
        mockAvatar("image/svg+xml",
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>".getBytes(StandardCharsets.UTF_8));

        Attachment avatar = this.extractor.getUserAvatar(USER_REFERENCE, SIZE);

        // The default avatar is used, without any warning (the log capture fails the test otherwise).
        assertEquals("Alice.jpg", avatar.getFilename());
        assertAvatar(Color.RED, avatar);
    }

    @Test
    void getUserAvatarWhenUserHasNoAvatar() throws Exception
    {
        Attachment avatar = this.extractor.getUserAvatar(USER_REFERENCE, SIZE);

        assertEquals("Alice.jpg", avatar.getFilename());
        assertAvatar(Color.RED, avatar);
    }

    @Test
    void getUserAvatarWhenAvatarCannotBeRead() throws Exception
    {
        XWikiAttachment attachment = mockAvatar("image/png", new byte[0]);
        when(attachment.getContentInputStream(this.xcontext)).thenThrow(new XWikiException());

        Attachment avatar = this.extractor.getUserAvatar(USER_REFERENCE, SIZE);

        assertEquals("Alice.jpg", avatar.getFilename());
        assertAvatar(Color.RED, avatar);
        assertEquals("Failed to get the avatar of [wiki:XWiki.Alice]. Fallback to default one. "
            + "Root cause: [XWikiException: Error number 0 in 0]", this.logCapture.getMessage(0));
    }

    @Test
    void getUserAvatarWhenAvatarIsCorrupted() throws Exception
    {
        // A PNG signature followed by garbage instead of the PNG header.
        mockAvatar("image/png", new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 1, 2, 3 });

        Attachment avatar = this.extractor.getUserAvatar(USER_REFERENCE, SIZE);

        assertEquals("Alice.jpg", avatar.getFilename());
        assertAvatar(Color.RED, avatar);
        // The root cause of the decoding failure depends on the ImageIO implementation.
        assertTrue(this.logCapture.getMessage(0)
            .startsWith("Failed to get the avatar of [wiki:XWiki.Alice]. Fallback to default one. Root cause: ["));
    }

    @Test
    void getUserAvatarOfGuest() throws Exception
    {
        Attachment avatar = this.extractor.getUserAvatar(null, SIZE);

        assertEquals("XWikiGuest.jpg", avatar.getFilename());
        assertAvatar(Color.RED, avatar);
    }

    private XWikiAttachment mockAvatar(String mediaType, byte[] content) throws XWikiException
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(this.userProfileDocument.getAttachment(AVATAR_FILE_NAME)).thenReturn(attachment);
        when(attachment.getMimeType(this.xcontext)).thenReturn(mediaType);
        when(attachment.getContentInputStream(this.xcontext)).thenReturn(new ByteArrayInputStream(content));

        return attachment;
    }

    private static byte[] createImage(Color color, String format) throws IOException
    {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);

        return output.toByteArray();
    }

    private static void assertAvatar(Color expectedColor, Attachment avatar) throws Exception
    {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(avatar.getContentAsBytes()));

        assertEquals(SIZE, image.getWidth());
        assertEquals(SIZE, image.getHeight());

        // The avatar is a lossy JPEG image so the color of its pixels is only close to the original one.
        Color actualColor = new Color(image.getRGB(SIZE / 2, SIZE / 2));
        assertTrue(Math.abs(expectedColor.getRed() - actualColor.getRed()) < 20
            && Math.abs(expectedColor.getGreen() - actualColor.getGreen()) < 20
            && Math.abs(expectedColor.getBlue() - actualColor.getBlue()) < 20,
            String.format("Expected a color close to [%s] but got [%s]", expectedColor, actualColor));
    }
}
