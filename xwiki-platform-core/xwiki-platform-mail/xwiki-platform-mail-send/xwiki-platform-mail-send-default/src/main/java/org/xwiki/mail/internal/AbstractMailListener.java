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

import org.slf4j.Logger;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailListener;

/**
 * Helper for implementation of {@link MailListener}.
 *
 * @version $Id$
 */
public abstract class AbstractMailListener implements MailListener
{
    @Inject
    protected Logger logger;

    private String batchId;

    protected String getBatchId()
    {
        return batchId;
    }

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
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail preparation succeed for message [{}] of batch [{}].",
                message.getUniqueMessageId(), batchId);
        }
    }

    @Override
    public void onPrepareMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail preparation failed for message [{}] of batch [{}].",
                message.getUniqueMessageId(), batchId, exception);
        }
    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {
        logger.debug("Failure during preparation phase of thread [" + batchId + "]");
    }

    @Override
    public void onPrepareEnd(Map<String, Object> parameters)
    {
        logger.debug("Mail preparation ended for batch [{}].", batchId);
    }

    @Override
    public void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail sent successfully for message [{}] of batch [{}].",
                message.getUniqueMessageId(), batchId);
        }
    }

    @Override
    public void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("Mail sending failed for message [{}] of batch [{}].",
                message.getUniqueMessageId(), batchId, exception);
        }
    }

    @Override
    public void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters)
    {
        logger.debug("Mail loading failed for message [{}] of batch [{}].", uniqueMessageId, batchId, exception);
    }
}
