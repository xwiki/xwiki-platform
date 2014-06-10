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
package org.xwiki.mail.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.mail.MimeBodyPartFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;

/**
 * Creates attachment body Part to be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("attachment")
@Singleton
public class AttachmentMimeBodyPartFactory implements MimeBodyPartFactory<Attachment>
{
    @Inject
    private Environment environment;

    /**
     * Provides access to the logger.
     */
    @Inject
    private Logger logger;

    @Override public MimeBodyPart create(Attachment attachment)
    {
        return this.create(attachment, Collections.<String, Object>emptyMap());
    }

    @Override public MimeBodyPart create(Attachment attachment, Map<String, Object> parameters)
    {
        // Check if existing headers
        Boolean hasHeaders = parameters.containsKey("headers");

        // Create the attachment part of the email
        MimeBodyPart attachmentPart = new MimeBodyPart();

        FileOutputStream fos = null;
        try {
            // Get attachment informations
            String fileName = attachment.getFilename();
            String attachmentMimeType = attachment.getMimeType();
            byte[] stream = attachment.getContent();

            File tempDir = environment.getTemporaryDirectory();
            File temp = File.createTempFile("tmpfile", ".tmp", tempDir);

            fos = new FileOutputStream(temp);
            fos.write(stream);

            DataSource source = new FileDataSource(temp);

            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setHeader("Content-Type", attachmentMimeType);
            attachmentPart.setHeader("Content-ID", "<" + fileName + ">");
            attachmentPart.setFileName(source.getName());

            if (hasHeaders && parameters.get("headers") instanceof Map) {
                Map<String, String> headers = (Map<String, String>) parameters.get("headers");
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    attachmentPart.setHeader(header.getKey(), header.getValue());
                }
            }
        } catch (FileNotFoundException e) {
            logger.warn("FileNotFoundException has occurred [{}]", e.getMessage());
        } catch (XWikiException xe) {
            logger.warn("XWikiException has occurred [{}]", xe.getMessage());
        } catch (IOException iox) {
            logger.warn("IOException has occurred [{}]", iox.getMessage());
        } catch (MessagingException mx) {
            logger.warn("MessagingException has occurred [{}]", mx.getMessage());
        } finally {
            try {
                if(fos != null){
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return attachmentPart;
    }
}
