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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStorageConfiguration;
import org.xwiki.mail.MailStoreException;

import com.xpn.xwiki.XWikiContext;

/**
 * Saves mail statuses in the database.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Named("database")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DatabaseMailListener extends AbstractMailListener implements Initializable
{
    @Inject
    private Execution execution;

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    @Named("database")
    private MailStatusStore mailStatusStore;

    @Inject
    private MailStorageConfiguration configuration;

    private DatabaseMailStatusResult mailStatusResult;

    @Override
    public void initialize() throws InitializationException
    {
        mailStatusResult = new DatabaseMailStatusResult(this.mailStatusStore);
    }

    @Override
    public void onPrepareBegin(String batchId, Map<String, Object> parameters)
    {
        super.onPrepareBegin(batchId, parameters);
        mailStatusResult.setBatchId(batchId);
    }

    @Override
    public void onPrepareMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        super.onPrepareMessageSuccess(message, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.PREPARE_SUCCESS);
        status.setWiki(
            ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).getWikiId());
        saveStatus(status, parameters);
    }

    @Override
    public void onPrepareMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        super.onPrepareMessageError(message, exception, parameters);

        MailStatus status = new MailStatus(getBatchId(), message, MailState.PREPARE_ERROR);
        status.setWiki(
            ((XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).getWikiId());
        status.setError(exception);
        saveStatus(status, parameters);

        // This mail will not reach the send queue, so its processing is done now.
        mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onPrepareFatalError(Exception exception, Map<String, Object> parameters)
    {
        super.onPrepareFatalError(exception, parameters);

        //TODO: Store failure exception
        logger.error("Failure during preparation phase of thread [" + getBatchId() + "]", exception);
    }

    @Override
    public void onSendMessageSuccess(ExtendedMimeMessage message, Map<String, Object> parameters)
    {
        super.onSendMessageSuccess(message, parameters);

        String uniqueMessageId = message.getUniqueMessageId();
        MailStatus status = retrieveExistingMailStatus(uniqueMessageId, MailState.SEND_SUCCESS);

        if (status != null) {
            status.setState(MailState.SEND_SUCCESS);
        } else {
            this.logger.warn("Forcing a new mail status for message [{}] of batch [{}] to send_success state.",
                uniqueMessageId, getBatchId());
            status = new MailStatus(getBatchId(), message, MailState.SEND_SUCCESS);
        }

        // Since the mail was sent successfully we don't need to keep its serialized content
        deleteMailContent(status);

        // If the user doesn't want to keep success status, we remove the mail status, otherwise we just update it
        if (configuration.discardSuccessStatuses()) {
            deleteStatus(status, parameters);
        } else {
            saveStatus(status, parameters);
        }

        mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageFatalError(String uniqueMessageId, Exception exception, Map<String, Object> parameters)
    {
        super.onSendMessageFatalError(uniqueMessageId, exception, parameters);

        MailStatus status = retrieveExistingMailStatus(uniqueMessageId, MailState.SEND_FATAL_ERROR);

        if (status != null) {
            status.setState(MailState.SEND_FATAL_ERROR);
            status.setError(exception);
            saveStatus(status, parameters);
        } else {
            this.logger.error("Unable to report the fatal error encountered during mail sending for message [{}] "
                    + "of batch [{}].", uniqueMessageId, getBatchId(), exception);
        }

        this.mailStatusResult.incrementCurrentSize();
    }

    @Override
    public void onSendMessageError(ExtendedMimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        super.onSendMessageError(message, exception, parameters);

        String uniqueMessageId = message.getUniqueMessageId();
        MailStatus status = retrieveExistingMailStatus(uniqueMessageId, MailState.SEND_ERROR);

        if (status != null) {
            status.setState(MailState.SEND_ERROR);
        } else {
            this.logger.warn("Forcing a new mail status for message [{}] of batch [{}] to send_error state.",
                uniqueMessageId, getBatchId());
            status = new MailStatus(getBatchId(), message, MailState.SEND_ERROR);
        }
        status.setError(exception);
        saveStatus(status, parameters);

        this.mailStatusResult.incrementCurrentSize();
    }

    private MailStatus retrieveExistingMailStatus(String uniqueMessageId, MailState state)
    {
        MailStatus status;
        try {
            status = mailStatusStore.load(uniqueMessageId);
            if (status == null) {
                // It's not normal to have no status in the mail status store since onPrepare should have been called
                // before.
                this.logger.error("Failed to find a previous mail status for message [{}] of batch [{}].",
                    uniqueMessageId, getBatchId(), state);
            }
        } catch (MailStoreException e) {
            this.logger.error("Error when looking for a previous mail status for message [{}] of batch [{}].",
                uniqueMessageId, getBatchId(), state, e);
            status = null;
        }
        return status;
    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return mailStatusResult;
    }

    private void saveStatus(MailStatus status, Map<String, Object> parameters)
    {
        try {
            mailStatusStore.save(status, parameters);
        } catch (MailStoreException e) {
            // Failed to save the status in the DB, we continue but log an error
            logger.error("Failed to save mail status [{}] to the database", status, e);
        }
    }

    private void deleteStatus(MailStatus status, Map<String, Object> parameters)
    {
        try {
            mailStatusStore.delete(status.getMessageId(), parameters);
        } catch (MailStoreException e) {
            // Failed to delete the status in the DB, we continue but log an error
            logger.error("Failed to delete mail status [{}] from the database", status, e);
        }
    }

    private void deleteMailContent(MailStatus currentStatus)
    {
        if (currentStatus != null) {
            try {
                mailContentStore.delete(currentStatus.getBatchId(), currentStatus.getMessageId());
            } catch (MailStoreException e) {
                // Failed to delete saved mail, raise a warning but continue since it's not critical
                this.logger.warn("Failed to remove previously failing message [{}] (batch id [{}]) from the file "
                    + "system. Reason [{}].", currentStatus.getMessageId(), currentStatus.getBatchId(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
