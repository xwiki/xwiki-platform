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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
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
public class FileSystemMailContentStore implements MailContentStore
{
    /**
     * The subdirectory in the permanent directory where we store mails.
     */
    public static final String ROOT_DIRECTORY = "mails";

    @Inject
    private Environment environment;

    @Override
    public void save(MimeMessage message) throws MailStoreException
    {
        String messageID = null;
        String batchID = null;
        File batchDirectory = null;
        try {
            messageID = message.getHeader("X-MailID", null);
            batchID = message.getHeader("X-BatchID", null);
            batchDirectory = getBatchDirectory(batchID);
            batchDirectory.mkdirs();
            File file = new File(batchDirectory, messageID);
            OutputStream os = new FileOutputStream(file);
            message.writeTo(os);
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to save message (id [%s], batch id [%s]) to the file system at [%s]",
                messageID, batchID, batchDirectory), e);
        }
    }

    @Override
    public MimeMessage load(Session session, String batchID, String messageID) throws MailStoreException
    {
        File batchDirectory = null;
        try {
            batchDirectory = getBatchDirectory(batchID);
            File file = new File(batchDirectory, messageID);
            InputStream is = new FileInputStream(file);
            MimeMessage message = new MimeMessage(session, is);
            return message;
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to load message (id [%s], batch id [%s]) from the file system at [%s]",
                messageID, batchID, batchDirectory), e);
        }
    }

    @Override
    public void delete(String batchID, String messageID) throws MailStoreException
    {
        File batchDirectory = null;
        try {
            batchDirectory = getBatchDirectory(batchID);
            File file = new File(batchDirectory, messageID);
            file.delete();
        } catch (Exception e) {
            throw new MailStoreException(String.format(
                "Failed to delete message (id [%s], batch id [%s]) from the file system at [%s]",
                messageID, batchID, batchDirectory), e);
        }
    }

    private File getBatchDirectory(String batchID)
    {
        return new File(new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY), batchID);
    }
}
