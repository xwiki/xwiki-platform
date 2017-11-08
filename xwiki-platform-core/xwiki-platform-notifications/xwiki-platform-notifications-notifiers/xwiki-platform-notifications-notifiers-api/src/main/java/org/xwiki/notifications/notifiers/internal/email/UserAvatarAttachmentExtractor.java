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
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;

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

    public Attachment getUserAvatar(DocumentReference userReference, int size) throws Exception
    {
        InputStream imageStream = null;
        try {
            imageStream = getUserAvatarStream(userReference);

            XWikiAttachment fakeAttachment = new XWikiAttachment();
            XWikiAttachmentContent content = new XWikiAttachmentContent(fakeAttachment);

            resizeImage(imageStream, size, content.getContentOutputStream());

            fakeAttachment.setAttachment_content(content);
            fakeAttachment.setFilename(
                    String.format("%s.jpg", userReference != null ? userReference.getName() : "XWikiGuest")
            );

            return new Attachment(null, fakeAttachment, xwikiContextProvider.get());

        } catch (Exception e) {
            throw new Exception(String.format("Failed to resize the avatar of [%s].", userReference), e);
        } finally {
            IOUtils.closeQuietly(imageStream);
        }
    }

    private InputStream getUserAvatarStream(DocumentReference userReference)
    {
        if (userReference != null) {
            try {

                XWikiContext context = xwikiContextProvider.get();
                XWiki xwiki = context.getWiki();

                XWikiDocument userProfileDocument = xwiki.getDocument(userReference, context);
                DocumentReference usersClassReference = xwiki.getUserClass(context).getDocumentReference();
                String avatarFileName = userProfileDocument.getStringValue(usersClassReference, "avatar");
                XWikiAttachment attachment = userProfileDocument.getAttachment(avatarFileName);

                if (attachment != null && attachment.isImage(context)) {
                    return attachment.getContentInputStream(context);
                }
            } catch (Exception e) {
                logger.warn("Failed to get the avatar of [{}]. Fallback to default one.", userReference, e);
            }
        }

        return getDefaultAvatarStream();
    }

    private InputStream getDefaultAvatarStream()
    {
        return environment.getResourceAsStream("/resources/icons/xwiki/noavatar.png");
    }

    private void resizeImage(InputStream imageFileInputStream, int size, OutputStream outputStream) throws IOException
    {
        BufferedImage bufferedImage = ImageIO.read(imageFileInputStream);
        int sourceWidth = bufferedImage.getWidth();
        int sourceHeight = bufferedImage.getHeight();

        int smallestDimension = Math.min(sourceWidth, sourceHeight);

        Thumbnails.of(bufferedImage).sourceRegion(sourceWidth / 2 - smallestDimension / 2,
                sourceHeight / 2 - smallestDimension / 2, smallestDimension, smallestDimension)
                .forceSize(size, size).outputFormat("jpg").toOutputStream(outputStream);

        IOUtils.closeQuietly(outputStream);
    }
}
