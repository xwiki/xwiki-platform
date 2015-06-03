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
package org.xwiki.watchlist.internal;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.image.ImageProcessor;

/**
 * Default implementation for {@link UserAvatarAttachmentExtractor}.
 *
 * @version $Id$
 * @since 7.1RC1
 */
@Component
@Singleton
public class DefaultUserAvatarAttachmentExtractor implements UserAvatarAttachmentExtractor
{
    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Used to get file resources.
     */
    @Inject
    private Environment environment;

    /**
     * Used to resize user avatars.
     */
    @Inject
    private ImageProcessor imageProcessor;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public Attachment getUserAvatar(DocumentReference userReference)
    {
        return getUserAvatar(userReference, 50, 50, String.format("%s.png", serializer.serialize(userReference)));
    }

    @Override
    public Attachment getUserAvatar(DocumentReference userReference, int width, int height, String fileName)
    {
        // FIXME: Unfortunately, the ImagePlugin is too much request-oriented and not generic enough to be reused
        // without rewriting. In the end, that might be the right way to go and rewriting it might be inevitable.

        Attachment result;

        InputStream sourceImageInputStream = null;
        try {
            XWikiContext context = xwikiContextProvider.get();
            XWiki wiki = context.getWiki();

            XWikiDocument userProfileDocument = wiki.getDocument(userReference, context);

            DocumentReference usersClassReference = wiki.getUserClass(context).getDocumentReference();
            String avatarFileName = userProfileDocument.getStringValue(usersClassReference, "avatar");

            XWikiAttachment realAvatarAttachment = userProfileDocument.getAttachment(avatarFileName);
            XWikiAttachment fakeAvatarAttachment;
            if (realAvatarAttachment != null && realAvatarAttachment.isImage(context)) {
                // Valid avatar, use the real attachment (and image), but make sure to use a clone as to not impact the
                // real document.
                fakeAvatarAttachment = (XWikiAttachment) realAvatarAttachment.clone();
                sourceImageInputStream = realAvatarAttachment.getContentInputStream(context);

                result = new Attachment(new Document(userProfileDocument, context), fakeAvatarAttachment, context);
            } else {
                // No avatar. Return a fake attachment with the "noavatar.png" standard image.
                fakeAvatarAttachment = new XWikiAttachment();
                sourceImageInputStream = environment.getResourceAsStream("/resources/icons/xwiki/noavatar.png");

                result = new Attachment(null, fakeAvatarAttachment, context);
            }

            // In both cases, set an empty attachment content that will be filled with the resized image. This way we
            // also avoid a request to the DB for the attachment content, since it will already be available.
            fakeAvatarAttachment.setAttachment_content(new XWikiAttachmentContent(fakeAvatarAttachment));

            // Resize the image and write it to the fake attachment.
            int resizedWidth = 50;
            int resizedHeight = 50;
            resizeImageToAttachment(sourceImageInputStream, resizedWidth, resizedHeight, fakeAvatarAttachment);

            // Set a fixed name for the user avatar file so that it is easy to work with in a template, for example.
            fakeAvatarAttachment.setFilename(fileName);
        } catch (Exception e) {
            logger.error("Failed to retrieve the avatar for the user {}", userReference, e);
            return null;
        } finally {
            // Close the source image input stream since we are done reading from it.
            if (sourceImageInputStream != null) {
                IOUtils.closeQuietly(sourceImageInputStream);
            }
        }

        return result;
    }

    private void resizeImageToAttachment(InputStream imageFileInputStream, int width, int height,
        XWikiAttachment outputAttachment) throws IOException
    {
        OutputStream attachmentOutputStream = null;
        try {
            Image originalImage = imageProcessor.readImage(imageFileInputStream);

            RenderedImage resizedImage = imageProcessor.scaleImage(originalImage, width, height);

            attachmentOutputStream = outputAttachment.getAttachment_content().getContentOutputStream();
            imageProcessor.writeImage(resizedImage, "image/png", 1.0f, attachmentOutputStream);
        } finally {
            // Close the attachment output stream since we are done writing to it.
            if (attachmentOutputStream != null) {
                IOUtils.closeQuietly(attachmentOutputStream);
            }
        }
    }
}
