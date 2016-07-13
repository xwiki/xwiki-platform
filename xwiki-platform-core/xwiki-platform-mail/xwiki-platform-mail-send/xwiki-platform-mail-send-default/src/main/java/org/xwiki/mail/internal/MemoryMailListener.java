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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.ExtendedMimeMessage;
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
public class MemoryMailListener extends AbstractMailListener
{
    private MemoryMailStatusResult mailStatusResult = new MemoryMailStatusResult();

    @Override
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        super.onPrepareMessageSuccess(message, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.PREPARE_SUCCESS);
        this.mailStatusResult.setStatus(status);
    }

    @Override
    public void onPrepareMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        super.onPrepareMessageError(message, exception, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.PREPARE_ERROR);
        status.setError(exception);
        this.mailStatusResult.setStatus(status);

        // This mail will not reach the send queue, so its processing is done now.
        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {
        super.onPrepareFatalError(exception, parameters);

        //TODO: Store failure exception
        logger.error("Failure during preparation phase of thread [" + getBatchId() + "]");
    }

    @Override
    public void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        super.onPrepareMessageSuccess(message, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.SEND_SUCCESS);
        this.mailStatusResult.setStatus(status);
        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters)
    {
        super.onSendMessageFatalError(uniqueMessageId, exception, parameters);

        MailStatus status = this.mailStatusResult.getStatus(uniqueMessageId);
        if (status != null) {
            status.setState(MailState.SEND_FATAL_ERROR);
            status.setError(exception);
            this.mailStatusResult.setStatus(status);
        } else {
            this.logger.error("Failed to find a previous mail status for message id [{}] of batch [{}]. "
                + "Unable to report the fatal error encountered during mail sending.", uniqueMessageId, getBatchId(),
                exception);
        }

        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        super.onSendMessageError(message, exception, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.SEND_ERROR);
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
}
