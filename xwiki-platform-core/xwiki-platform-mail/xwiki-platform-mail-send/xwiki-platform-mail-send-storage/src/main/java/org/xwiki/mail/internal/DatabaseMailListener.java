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

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;

/**
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("database")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DatabaseMailListener implements MailListener, Initializable
{
    @Inject
    private Logger logger;

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    @Named("database")
    private MailStatusStore mailStatusStore;

    private DatabaseMailStatusResult mailStatusResult;

    @Override
    public void initialize() throws InitializationException
    {
        this.mailStatusResult = new DatabaseMailStatusResult(this.mailStatusStore);
    }

    @Override
    public void onPrepare(MimeMessage message)
    {
        // Initialize the DatabaseMailStatusResult on first execution, in order to save the Batch ID
        String batchId = getMessageBatchId(message);
        this.mailStatusResult.setBatchId(batchId);

        final MailStatus result = new MailStatus(getMessageId(message));
        result.setBatchId(batchId);
        result.setState(MailState.READY);
        result.setRecipients(getMessageRecipients(message));
        saveStatus(result);
    }

    @Override
    public void onSuccess(MimeMessage message)
    {
        String messageId = getMessageId(message);
        MailStatus status = getMailStatus(messageId);
        if (status != null) {
            // If the mail has previously failed to be sent, then remove it from the file system since it has now
            // succeeded!
            if (status.getState().equals(MailState.FAILED.toString())) {
                String batchId = getMessageBatchId(message);
                try {
                    this.mailContentStore.delete(batchId, messageId);
                } catch (MailStoreException e) {
                    // Failed to delete saved mail, raise a warning but continue since it's not critical
                    this.logger.warn("Failed to remove previously failing message from the file system. Reason [{}]. "
                        + "However it has now been sent successfully.", ExceptionUtils.getRootCauseMessage(e));
                }
            }
            status.setState(MailState.SENT);
            saveStatus(status);
        }
    }

    @Override
    public void onError(MimeMessage message, Exception exception)
    {
        String messageId = getMessageId(message);
        MailStatus status = getMailStatus(messageId);
        if (status != null) {
            // Since there's been an error, we save the message to the file system so that it can be resent later on
            // if need be.
            try {
                this.mailContentStore.save(message);
            } catch (MailStoreException e) {
                // The mail has failed to be saved on disk, which means we won't be able to resend it. Since this can
                // an important problem we log an error but we don't throw an Exception since that would cause an
                // infinite loop as we're already here because the mail has already failed to be sent and raising an
                // exception would call us again!
                this.logger.error("Failed to save message to the file system, this message "
                    + "won't be able to be sent again later on.", e);
            }
            status.setState(MailState.FAILED);
            status.setError(exception);
            saveStatus(status);
        }
    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return this.mailStatusResult;
    }

    private String getMessageId(MimeMessage message)
    {
        try {
            return message.getHeader("X-MailID", null);
        } catch (MessagingException e) {
            this.logger.error("Failed to retrieve Message ID from message.", e);
            return null;
        }
    }

    private String getMessageBatchId(MimeMessage message)
    {
        try {
            return message.getHeader("X-BatchID", null);
        } catch (MessagingException e) {
            this.logger.error("Failed to retrieve Batch ID from message.", e);
            return null;
        }
    }

    private String getMessageRecipients(MimeMessage message)
    {
        try {
            return InternetAddress.toString(message.getAllRecipients());
        } catch (MessagingException e) {
            this.logger.error("Failed to retrieve recipients from message.", e);
            return null;
        }
    }

    private MailStatus getMailStatus(String messageId)
    {
        MailStatus status;
        try {
            status = this.mailStatusStore.loadFromMessageId(messageId);
        } catch (MailStoreException e) {
            // Failed to load the status in the DB, we continue but log an error
            this.logger.error("Failed to load mail status for message id [{}] from the database", messageId, e);
            status = null;
        }
        return status;
    }

    private void saveStatus(MailStatus status)
    {
        try {
            this.mailStatusStore.save(status);
        } catch (MailStoreException e) {
            // Failed to save the status in the DB, we continue but log an error
            this.logger.error("Failed to save mail status [{}] to the database", status, e);
        }
    }
}
