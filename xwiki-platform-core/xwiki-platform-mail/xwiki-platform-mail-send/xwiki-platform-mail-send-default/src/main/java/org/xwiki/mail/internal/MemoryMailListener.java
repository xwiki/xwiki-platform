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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;

/**
 * Saves errors when sending messages, in a local variable.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("memory")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class MemoryMailListener implements MailListener
{
    @Inject
    private Logger logger;

    private MemoryMailStatusResult mailStatusResult = new MemoryMailStatusResult();

    private String batchId;

    @Override
    public void onPrepareBegin(String batchId, Map<String, Object> parameters)
    {
        if (this.batchId != null) {
            throw new RuntimeException("A mail listener cannot be reused. This listener has been used for batch ["
                + this.batchId + "] and is now called for batch [" + batchId + "].");
        }

        logger.debug("Mail preparation begins for batch [{}].", batchId);

        this.batchId = batchId;
    }

    @Override
    public void onPrepareMessageSuccess(MimeMessage message, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail preparation succeed for message [{}] of batch [{}].", getMessageId(message), batchId);
        }

        MailStatus status = new MailStatus(batchId, message, MailState.PREPARE_SUCCESS);
        this.mailStatusResult.setStatus(status);
    }

    @Override
    public void onPrepareMessageError(MimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail preparation failed for message [{}] of batch [{}].", getMessageId(message), batchId,
                exception);
        }

        MailStatus status = new MailStatus(batchId, message, MailState.PREPARE_ERROR);
        status.setError(exception);
        this.mailStatusResult.setStatus(status);

        // This mail will not reach the send queue, so its processing is done now.
        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onPrepareFatalError(Exception e, Map<String, Object> parameters)
    {
        //TODO: Store failure exception
        logger.error("Failure during preparation phase of thread [" + batchId + "]");
    }

    @Override
    public void onPrepareEnd(Map<String, Object> parameters)
    {
        logger.debug("Mail preparation ended for batch [{}].", batchId);
    }

    @Override
    public void onSendMessageSuccess(MimeMessage message, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail sent successfully for message [{}] of batch [{}].", getMessageId(message), batchId);
        }

        MailStatus status = new MailStatus(batchId, message, MailState.SEND_SUCCESS);
        this.mailStatusResult.setStatus(status);
        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageFatalError(String messageId, Exception exception, Map<String, Object> parameters)
    {
        logger.debug("Mail loading failed for message [{}] of batch [{}].", messageId, batchId, exception);

        MailStatus status = this.mailStatusResult.getStatus(messageId);
        if (status != null) {
            status.setState(MailState.SEND_FATAL_ERROR);
            status.setError(exception);
            this.mailStatusResult.setStatus(status);
        } else {
            this.logger.error("Failed to find a previous mail status for message id [{}] of batch [{}]. "
                + "Unable to report the fatal error encountered during mail sending.", messageId, batchId, exception);
        }

        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageError(MimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail sending failed for message [{}] of batch [{}].", getMessageId(message), batchId,
                exception);
        }

        MailStatus status = new MailStatus(batchId, message, MailState.SEND_ERROR);
        status.setError(exception);
        this.mailStatusResult.setStatus(status);

        // This mail will not reach the send queue, so its processing is done now.
        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return this.mailStatusResult;
    }

    private String getMessageId(MimeMessage message)
    {
        try {
            return message.getMessageID();
        } catch (MessagingException e) {
            // This cannot happen in practice since the implementation never throws any exception!
            logger.error("Failed to retrieve messageID from the message.", e);
            return null;
        }
    }
}
