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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;

import net.coobird.thumbnailator.Thumbnails;

/**
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = UserAvatarAttachmentExtractor.class)
@Singleton
public class UserAvatarAttachmentExtractor
{
    /**
     * Used to get file resources.
     */
    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    /**
     * @param userReference reference of the user
     * @param size the wanted size for the image
     * @return a fake attachment holding a resized and square-cropped version of the avatar of the given user
     * @throws Exception if an error happens
     */
    public Attachment getUserAvatar(DocumentReference userReference, int size) throws Exception
    {
        try {
            BufferedImage image = getUserAvatarImage(userReference);

            XWikiAttachment fakeAttachment = new XWikiAttachment();
            XWikiAttachmentContent content = new XWikiAttachmentContent(fakeAttachment);

            resizeImage(image, size, content.getContentOutputStream());

            fakeAttachment.setAttachment_content(content);
            fakeAttachment.setFilename(
                    String.format("%s.jpg", userReference != null ? userReference.getName() : "XWikiGuest")
            );

            return new Attachment(null, fakeAttachment, xwikiContextProvider.get());

        } catch (Exception e) {
            throw new Exception(String.format("Failed to resize the avatar of [%s].", userReference), e);
        }
    }

    private BufferedImage getUserAvatarImage(DocumentReference userReference) throws IOException
    {
        if (userReference != null) {
            try {
                BufferedImage image = readUserAvatarImage(userReference);
                if (image != null) {
                    return image;
                }
            } catch (Exception e) {
                logger.warn("Failed to get the avatar of [{}]. Fallback to default one. Root cause: [{}]",
                    userReference, ExceptionUtils.getRootCauseMessage(e));
                logger.debug("Full stack trace of the avatar loading failure:", e);
            }
        }

        return getDefaultAvatarImage();
    }

    /**
     * @param userReference reference of the user
     * @return the decoded avatar of the given user, or {@code null} if the user has no avatar or if ImageIO cannot
     *     decode it
     */
    private BufferedImage readUserAvatarImage(DocumentReference userReference) throws XWikiException, IOException
    {
        XWikiContext context = xwikiContextProvider.get();
        XWiki xwiki = context.getWiki();

        XWikiDocument userProfileDocument = xwiki.getDocument(userReference, context);
        String avatarFileName = userProfileDocument.getStringValue(
            XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE, "avatar");
        XWikiAttachment attachment = userProfileDocument.getAttachment(avatarFileName);

        if (attachment != null) {
            String mediaType = attachment.getMimeType(context);
            if (Strings.CS.startsWith(mediaType, "image/")) {
                BufferedImage image = readImage(attachment.getContentInputStream(context));
                if (image == null) {
                    // ImageIO has no reader for some image formats that are accepted as avatar (e.g. SVG or WebP),
                    // which is expected and not worth more than a debug log.
                    logger.debug("Unsupported image format [{}] for the avatar of [{}]. Fallback to default one",
                        mediaType, userReference);
                }

                return image;
            }
        }

        return null;
    }

    private BufferedImage getDefaultAvatarImage() throws IOException
    {
        return readImage(environment.getResourceAsStream("/resources/icons/xwiki/noavatar.png"));
    }

    /**
     * @param imageStream the stream of the image to read, closed by this method
     * @return the decoded image, or {@code null} if no ImageIO reader supports its format
     */
    private BufferedImage readImage(InputStream imageStream) throws IOException
    {
        try (imageStream) {
            return ImageIO.read(imageStream);
        }
    }

    private void resizeImage(BufferedImage bufferedImage, int size, OutputStream outputStream) throws IOException
    {
        int sourceWidth = bufferedImage.getWidth();
        int sourceHeight = bufferedImage.getHeight();

        int smallestDimension = Math.min(sourceWidth, sourceHeight);

        Thumbnails.of(bufferedImage).sourceRegion(sourceWidth / 2 - smallestDimension / 2,
                sourceHeight / 2 - smallestDimension / 2, smallestDimension, smallestDimension)
                .forceSize(size, size).outputFormat("jpg").toOutputStream(outputStream);

        IOUtils.closeQuietly(outputStream);
    }
}
