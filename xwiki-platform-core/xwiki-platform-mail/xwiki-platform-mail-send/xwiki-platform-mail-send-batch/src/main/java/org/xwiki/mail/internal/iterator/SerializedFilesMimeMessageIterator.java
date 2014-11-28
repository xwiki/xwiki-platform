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
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 *  Generate messages from a list of files.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class SerializedFilesMimeMessageIterator extends AbstractMessageIterator
{
    private final List<File> files;

    /**
     *
     * @param files the list of file that contains serialized MimeMessages
     * @param parameters the parameters from which to extract the session
     */
    public SerializedFilesMimeMessageIterator(List<File> files, Map<String, Object> parameters)
    {
        this.iteratorSize = files.size();
        this.files = files;
        this.parameters = parameters;
    }

    @Override protected MimeMessage createMessage() throws MessagingException
    {
        File file = this.files.get(this.position);
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
