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
package org.xwiki.mail.internal.iterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;

/**
 * Generate messages from a list of files.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class SerializedFilesMimeMessageIterator extends AbstractMessageIterator
{
    private final File[] files;

    private ComponentManager componentManager;

    private Environment environment;

    private File batchDirectory;

    /**
     * @param batchID the name of the directory that contains serialized MimeMessages
     * @param parameters the parameters from which to extract the session
     * @param componentManager used to dynamically load components
     * @throws MessagingException when an error occurs when retrieving messages
     */
    public SerializedFilesMimeMessageIterator(UUID batchID, Map<String, Object> parameters,
        ComponentManager componentManager) throws MessagingException
    {
        this.componentManager = componentManager;
        try {
            this.environment = this.componentManager.getInstance(Environment.class);
        } catch (ComponentLookupException e) {
            throw new MessagingException("Failed to find default Environment", e);
        }
        this.batchDirectory = new File(this.environment.getPermanentDirectory(), batchID.toString());
        this.files = this.batchDirectory.listFiles();
        this.iteratorSize = this.files.length;
        this.parameters = parameters;
    }

    @Override protected MimeMessage createMessage() throws MessagingException
    {
        File file = this.files[this.position];
        Session session = (Session) this.parameters.get("session");
        try {
            FileInputStream emailStream = new FileInputStream(file);
            return new MimeMessage(session, emailStream);
        } catch (FileNotFoundException e) {
            throw new MessagingException(
                String.format("Failed to create mime message from file [%s]", file.getPath()), e);
        }
    }
}
