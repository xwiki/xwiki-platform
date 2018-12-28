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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;

import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.environment.Environment;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.factory.attachment.AttachmentMimeBodyPartFactory;

/**
 * Stores mail content on the file system.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("filesystem")
@Singleton
public class FileSystemMailContentStore implements MailContentStore, Initializable
{
    /**
     * The subdirectory in the permanent directory where we store mails.
     */
    public static final String ROOT_DIRECTORY = "mails";

    private File rootDirectory;

    @Inject
    private Environment environment;

    @Override
    public void initialize()
    {
        rootDirectory = new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY);
    }

    @Override
    public void save(String batchId, ExtendedMimeMessage message) throws MailStoreException
    {
        // Delete any temporary file used to hold attachments since their content will have been serialized
        // by the call to writeTo() above. This avoids keeping temporary files on the filesystem.
        // First, find if there are any.
        List<File> temporaryFiles = extractTemporaryFilesFromHeaders(message);

        String uniqueMessageId = message.getUniqueMessageId();
        File messageFile = getMessageFile(batchId, uniqueMessageId);

        try {
            // Unsaved message may have their message-ID header to be modified during serialization.
            // We ensure that the message was saved, and we save it if not saved yet, getting again the identifier
            // to be sure we have the right ones.
            if (message.ensureSaved()) {
                uniqueMessageId = message.getUniqueMessageId();
                messageFile = getMessageFile(batchId, uniqueMessageId);
            }

            message.writeTo(new FileOutputStream(messageFile));

            // Delete any found temporary attachment files
            for (File temporaryFile : temporaryFiles) {
                FileUtils.forceDelete(temporaryFile);
            }
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to save message (id [%s], batch id [%s]) into file [%s]",
                uniqueMessageId, batchId, messageFile), e);
        }
    }

    @Override
    public ExtendedMimeMessage load(Session session, String batchId, String uniqueMessageId) throws MailStoreException
    {
        File messageFile = null;
        try {
            messageFile = getMessageFile(batchId, uniqueMessageId);
            InputStream is = new FileInputStream(messageFile);
            return new ExtendedMimeMessage(session, is);
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to load message (id [%s], batch id [%s]) from file [%s]",
                uniqueMessageId, batchId, messageFile), e);
        }
    }

    @Override
    public void delete(String batchId, String uniqueMessageId) throws MailStoreException
    {
        File messageFile = null;
        try {
            messageFile = getMessageFile(batchId, uniqueMessageId);
            messageFile.delete();
            // Also remove the directory. Note that it'll succeed only the directory is empty which is what we want.
            getBatchDirectory(batchId).delete();
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to delete message (id [%s], batch id [%s]) file [%s]",
                uniqueMessageId, batchId, messageFile), e);
        }
    }

    private List<File> extractTemporaryFilesFromHeaders(ExtendedMimeMessage message) throws MailStoreException
    {
        List<File> temporaryFiles = new ArrayList<>();
        try {
            Object content = message.getContent();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    Part part = multipart.getBodyPart(i);
                    String[] temporaryFileLocations =
                        part.getHeader(AttachmentMimeBodyPartFactory.TMP_ATTACHMENT_LOCATION_FILE_HEADER);
                    if (temporaryFileLocations != null && temporaryFileLocations.length > 0) {
                        temporaryFiles.add(new File(temporaryFileLocations[0]));
                        // Remove the special marker header so that it doesn't get sent.
                        part.removeHeader(AttachmentMimeBodyPartFactory.TMP_ATTACHMENT_LOCATION_FILE_HEADER);
                        // Note: for the removed header to be really removed we need to save the message.
                        message.saveChanges();
                    }
                }
            }
        } catch (Exception e) {
            throw new MailStoreException("Failed to extract temporary file refernces from headers", e);
        }

        return temporaryFiles;
    }

    private File getBatchDirectory(String batchId)
    {
        File batchDirectory = new File(rootDirectory, getURLEncoded(batchId));
        batchDirectory.mkdirs();
        return batchDirectory;
    }

    private File getMessageFile(String batchId, String uniqueMessageId)
    {
        return new File(getBatchDirectory(batchId), getURLEncoded(uniqueMessageId));
    }

    private static String getURLEncoded(final String toEncode)
    {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not available, this Java VM is not standards compliant!");
        }
    }
}
