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
package org.xwiki.mail.internal.factory;

import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xwiki.mail.MimeMessageFactory;

/**
 * Abstract class to generate iterator of MimeMessage.
 *
 * @version $Id$
 * @since 6.4M3
 */
public abstract class AbstractMessageIterator implements Iterator<MimeMessage>, Iterable<MimeMessage>
{
    protected MimeMessageFactory<MimeMessage> factory;

    protected int position;

    protected int iteratorSize;

    protected Map<String, Object> parameters;

    /**
     * @return the MimeMessage as the current element of Iterator.
     * @throws MessagingException  when an error occurs
     */
    protected abstract MimeMessage createMessageInternal() throws MessagingException;

    protected abstract Logger getLogger();

    /**
     * @return the MimeMessage as the current element of Iterator.
     * @throws MessagingException  when an error occurs
     */
    public MimeMessage createMessage() throws MessagingException
    {
        MimeMessage message = createMessageInternal();

        // Set the Message type if passed in parameters
        String type = (String) parameters.get("type");
        if (type != null) {
            message.addHeader("X-MailType", type);
        }

        return message;
    }

    @Override
    public MimeMessage next()
    {
        MimeMessage mimeMessage;
        try {
            mimeMessage = createMessage();
        } catch (Exception e) {
            //TODO We need to save all the errors and display them in the status of all emails in the admin UI.
            getLogger().error("Failed to create Mime Message", e);
            mimeMessage = null;
        }
        this.position++;
        return mimeMessage;
    }

    @Override
    public boolean hasNext()
    {
        return this.iteratorSize != this.position;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<MimeMessage> iterator()
    {
        return this;
    }
}
