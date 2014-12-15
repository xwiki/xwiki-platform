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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.AbstractMailListener;
import org.xwiki.mail.MailStatus;

/**
 * Saves errors when sending messages, in a local variable.
 *
 * @version $Id$
 * @since 6.4M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named("memory")
public class MemoryMailListener extends AbstractMailListener
{
    @Inject
    private Logger logger;

    private BlockingQueue<MailStatus> errorQueue = new LinkedBlockingQueue<>(100);

    @Override public void onPrepare(MimeMessage message)
    {
        // We're only interested in errors in the scripting API.
    }

    @Override
    public void onSuccess(MimeMessage message)
    {
        // We're only interested in errors in the scripting API.
    }

    @Override
    public void onError(MimeMessage message, Exception e)
    {
        try {
            this.errorQueue.add(new MailStatus(message.getMessageID(), ExceptionUtils.getMessage(e)));
        } catch (MessagingException ex) {
            this.logger.warn("Failed to retrieve Message ID from message. Reason: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * @return the list of exceptions raised when sending mails in the current thread
     */
    @Override
    public Iterator<MailStatus> getErrors()
    {
        return this.errorQueue.iterator();
    }

    @Override
    public int getErrorsNumber()
    {
        return this.errorQueue.size();
    }
}
