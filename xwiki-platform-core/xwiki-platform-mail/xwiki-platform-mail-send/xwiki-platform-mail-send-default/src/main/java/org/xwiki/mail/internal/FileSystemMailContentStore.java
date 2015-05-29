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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailStoreException;

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
    public void initialize() throws InitializationException
    {
        rootDirectory = new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY);
    }

    @Override
    public void save(String batchId, MimeMessage message) throws MailStoreException
    {
        String messageId = null;
        File messageFile = null;
        try {
            messageId = getMessageId(message);
            messageFile = getMessageFile(batchId, messageId);
            OutputStream os = new FileOutputStream(messageFile);
            message.writeTo(os);
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to save message (id [%s], batch id [%s]) into file [%s]",
                messageId, batchId, messageFile), e);
        }
    }

    @Override
    public MimeMessage load(Session session, String batchId, String messageId) throws MailStoreException
    {
        File messageFile = null;
        try {
            messageFile = getMessageFile(batchId, messageId);
            InputStream is = new FileInputStream(messageFile);
            return new MimeMessage(session, is);
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to load message (id [%s], batch id [%s]) from file [%s]",
                messageId, batchId, messageFile), e);
        }
    }

    @Override
    public void delete(String batchId, String messageId) throws MailStoreException
    {
        File messageFile = null;
        try {
            messageFile = getMessageFile(batchId, messageId);
            messageFile.delete();
            // Also remove the directory. Note that it'll succeed only the directory is empty which is what we want.
            getBatchDirectory(batchId).delete();
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to delete message (id [%s], batch id [%s]) file [%s]",
                messageId, batchId, messageFile), e);
        }
    }

    private File getBatchDirectory(String batchId)
    {
        File batchDirectory = new File(rootDirectory, getURLEncoded(batchId));
        batchDirectory.mkdirs();
        return batchDirectory;
    }

    private File getMessageFile(String batchId, String messageId) {
        return new File(getBatchDirectory(batchId), getURLEncoded(messageId));
    }

    private String getMessageId(MimeMessage message) throws MessagingException
    {
        String messageId = message.getMessageID();
        if (messageId == null) {
            message.saveChanges();
            messageId = message.getMessageID();
        }

        return messageId;
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
