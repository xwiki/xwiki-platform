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

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.mail.MimeMessageFactory;

/**
 * Abstract class to generate iterator of MimeMessage.
 *
 * @version $Id$
 * @since 6.4M2
 */
public abstract class AbstractMessageIterator implements Iterator<MimeMessage>
{
    protected MimeMessageFactory factory;

    protected int position;

    protected int iteratorSize;

    protected Map<String, Object> parameters;

    /**
     * Provides access to the logger.
     */
    @Inject
    private Logger logger;

    /**
     * @return the MimeMessage as the current element of Iterator.
     * @throws MessagingException  when an error occurs
     */
    protected abstract MimeMessage createMessage() throws MessagingException;

    @Override public MimeMessage next()
    {
        MimeMessage mimeMessage = null;
        try {
            mimeMessage = createMessage();
        } catch (MessagingException e) {
            this.logger.error("Failed to create mime message. "
                + "Root reason: [{}]", ExceptionUtils.getRootCauseMessage(e));
            //TODO We need to save all the errors and display them in the status of all emails in the admin UI.
        }
        position++;
        return mimeMessage;
    }

    @Override public boolean hasNext()
    {
        return this.iteratorSize != position;
    }

    @Override public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
