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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.mail.MailStore;

/**
 * Default implementation.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Singleton
public class DefaultMailStore implements MailStore
{
    /**
     * The mails store directory name.
     */
    public static final String ROOT_DIRECTORY = "mailstore";

    @Inject
    private Logger logger;

    @Inject
    private Environment environment;

    @Override
    public void save(MimeMessage message)
    {
        try {
            String messageID = message.getHeader("X-MailID", null);
            String batchID = message.getHeader("X-BatchID", null);
            File batchDirectory =
                new File(new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY), batchID);
            batchDirectory.mkdirs();
            File file = new File(batchDirectory, messageID);
            OutputStream os = new FileOutputStream(file);
            message.writeTo(os);
        } catch (IOException | MessagingException e) {
            this.logger.warn("Failed to save message on disk. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Override
    public MimeMessage load(Session session, String batchID, String messageID)
    {
        try {
            File batchDirectory =
                new File(new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY), batchID);
            File file = new File(batchDirectory, messageID);
            InputStream is = new FileInputStream(file);
            MimeMessage message = new MimeMessage(session, is);
            return message;
        } catch (MessagingException | FileNotFoundException e) {
            this.logger.warn("Failed to load message from disk. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
