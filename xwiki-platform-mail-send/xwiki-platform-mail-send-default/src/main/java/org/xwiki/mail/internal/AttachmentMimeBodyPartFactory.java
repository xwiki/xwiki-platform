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
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.xwiki.mail.MimeBodyPartFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;

/**
 * Creates attachment body Part to be added to a Multi Part message.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Named("attachment")
public class AttachmentMimeBodyPartFactory implements MimeBodyPartFactory<Attachment>
{
    @Override public MimeBodyPart create(Attachment attachment, Map<String, Object> parameters)
    {
        // Create the attachment part of the email
        MimeBodyPart attachmentPart = new MimeBodyPart();

        try {
            // Get attachment informations
            String fileName = attachment.getFilename();
            String attachmentMimeType = attachment.getMimeType();
            byte[] stream = attachment.getContent();

            File temp = File.createTempFile("tmpfile", ".tmp");
            FileOutputStream fos = new FileOutputStream(temp);
            fos.write(stream);
            fos.close();
            DataSource source = new FileDataSource(temp);

            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setHeader("Content-Type", attachmentMimeType);
            attachmentPart.setHeader("Content-ID", "<" + fileName + ">");
            attachmentPart.setFileName(source.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XWikiException xwikiException) {
            xwikiException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (MessagingException messagingException) {
            messagingException.printStackTrace();
        }

        return attachmentPart;
    }
}