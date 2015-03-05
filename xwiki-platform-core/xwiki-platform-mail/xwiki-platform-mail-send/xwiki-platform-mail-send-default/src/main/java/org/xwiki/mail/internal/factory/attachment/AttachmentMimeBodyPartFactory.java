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
package org.xwiki.mail.internal.factory.attachment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.mail.internal.factory.AbstractMimeBodyPartFactory;

import com.xpn.xwiki.api.Attachment;

/**
 * Creates an attachment Body Part from an {@link Attachment} object. This will be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("xwiki/attachment")
@Singleton
public class AttachmentMimeBodyPartFactory extends AbstractMimeBodyPartFactory<Attachment> implements Initializable
{
    private static final String HEADERS_PARAMETER_KEY = "headers";

    @Inject
    private Environment environment;

    /**
     * Provides access to the logger.
     */
    @Inject
    private Logger logger;

    private File temporaryDirectory;

    @Override
    public void initialize() throws InitializationException
    {
        this.temporaryDirectory = new File(this.environment.getTemporaryDirectory(), "mail");
        this.temporaryDirectory.mkdirs();
    }

    @Override
    public MimeBodyPart create(Attachment attachment, Map<String, Object> parameters) throws MessagingException
    {
        // Create the attachment part of the email
        MimeBodyPart attachmentPart = new MimeBodyPart();

        // Save the attachment to a temporary file on the file system and wrap it in a Java Mail Data Source.
        DataSource source = createTemporaryAttachmentDataSource(attachment);
        attachmentPart.setDataHandler(new DataHandler(source));

        attachmentPart.setHeader("Content-Type", attachment.getMimeType());

        // Add a content-id so that we can uniquely reference this attachment. This is used for example to
        // display the attachment inline in some mail HTML content.
        // Note: According to http://tools.ietf.org/html/rfc2392 the id must be enclosed in angle brackets.
        attachmentPart.setHeader("Content-ID", "<" + attachment.getFilename() + ">");

        attachmentPart.setFileName(source.getName());

        // Handle headers passed as parameter
        addHeaders(attachmentPart, parameters);

        return attachmentPart;
    }

    private DataSource createTemporaryAttachmentDataSource(Attachment attachment) throws MessagingException
    {
        File temporaryAttachmentFile;
        FileOutputStream fos = null;
        try {
            temporaryAttachmentFile = File.createTempFile("attachment", ".tmp", this.temporaryDirectory);
            temporaryAttachmentFile.deleteOnExit();
            fos = new FileOutputStream(temporaryAttachmentFile);
            fos.write(attachment.getContent());
        } catch (Exception e) {
            throw new MessagingException(
                String.format("Failed to save attachment [%s] to the file system", attachment.getFilename()), e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                // Only an error at closing, we continue
                this.logger.warn("Failed to close the temporary file attachment when sending an email. "
                    + "Root reason: [{}]", ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return new FileDataSource(temporaryAttachmentFile);
    }
}
