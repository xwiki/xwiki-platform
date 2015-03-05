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
package org.xwiki.mail.internal.factory.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.mail.internal.factory.AbstractMessageIterator;

/**
 * Generate messages from a list of files.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class SerializedFilesMimeMessageIterator extends AbstractMessageIterator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializedFilesMimeMessageIterator.class);

    /**
     * The mails store directory name.
     */
    private static final String ROOT_DIRECTORY = "mailstore";

    private final File[] files;

    private ComponentManager componentManager;

    private Environment environment;

    private File batchDirectory;

    /**
     * @param batchId the name of the directory that contains serialized MimeMessages
     * @param parameters the parameters from which to extract the session
     * @param componentManager used to dynamically load components
     * @throws MessagingException when an error occurs when retrieving messages
     */
    public SerializedFilesMimeMessageIterator(String batchId, Map<String, Object> parameters,
        ComponentManager componentManager) throws MessagingException
    {
        this.componentManager = componentManager;
        try {
            this.environment = this.componentManager.getInstance(Environment.class);
        } catch (ComponentLookupException e) {
            throw new MessagingException("Failed to find an Environment Component", e);
        }
        this.batchDirectory =
            new File(new File(this.environment.getPermanentDirectory(), ROOT_DIRECTORY), batchId);
        this.files = this.batchDirectory.listFiles();
        this.iteratorSize = this.files.length;
        this.parameters = parameters;
    }

    @Override
    protected MimeMessage createMessageInternal() throws MessagingException
    {
        File file = this.files[this.position];
        try {
            FileInputStream emailStream = new FileInputStream(file);
            // Note: We don't create a Session here ATM since it's not required. The returned MimeMessage will be
            // given a valid Session when it's deserialized from the mail content store for sending.
            return new MimeMessage(null, emailStream);
        } catch (FileNotFoundException e) {
            throw new MessagingException(
                String.format("Failed to create mime message from file [%s]", file.getPath()), e);
        }
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }
}
