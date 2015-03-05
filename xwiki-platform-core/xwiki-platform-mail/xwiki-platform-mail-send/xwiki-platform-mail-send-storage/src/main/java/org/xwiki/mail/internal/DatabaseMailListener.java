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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
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
import org.xwiki.mail.MailStorageConfiguration;
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

    @Inject
    private MailStorageConfiguration configuration;

    private DatabaseMailStatusResult mailStatusResult;

    @Override
    public void initialize() throws InitializationException
    {
        this.mailStatusResult = new DatabaseMailStatusResult(this.mailStatusStore);
    }

    @Override
    public void onPrepare(MimeMessage message, Map<String, Object> parameters)
    {
        MailStatus status = createMailStatus(message, MailState.READY, parameters);
        saveStatus(status, parameters);

        // Initialize the DatabaseMailStatusResult on first execution by passing the batch id
        this.mailStatusResult.setBatchId(status.getBatchId());
    }

    @Override
    public void onSuccess(MimeMessage message, Map<String, Object> parameters)
    {
        String messageId = getMessageId(message);
        MailStatus status;
        try {
            status = loadMailStatus(messageId, parameters);
            if (status == null) {
                // It's not normal to have no status in the mail status store since onPrepare should have been called
                // before.
                this.logger.warn("Failed to find a previous mail status for message id [{}]. However the mail was sent "
                    + "successfully. Forcing status to [{}]", messageId, MailState.SENT);
            }
        } catch (MailStoreException e) {
            this.logger.error("Error when looking for a previous mail status for message id [{}]. However the mail was "
                + "sent successfully. Forcing status to [{}]", messageId, MailState.SENT, e);
            status = null;
        }

        if (status != null) {
            status.setState(MailState.SENT);
        } else {
            status = createMailStatus(message, MailState.SENT, parameters);
        }

        // Since the mail was sent successfully we don't need to keep its serialized content
        deleteMailContent(status);

        // If the user doesn't want to keep it for tracability we remove the mail status, otherwise we just update it
        if (this.configuration.discardSuccessStatuses()) {
            deleteStatus(status, parameters);
        } else {
            saveStatus(status, parameters);
        }
    }

    @Override
    public void onError(MimeMessage message, Exception exception, Map<String, Object> parameters)
    {
        String messageId = getMessageId(message);
        MailStatus status;
        try {
            status = loadMailStatus(messageId, parameters);
            if (status == null) {
                // It's not normal to have no status in the mail status store since onPrepare should have been called
                // before.
                this.logger.warn("Failed to find a previous mail status for message id [{}]. In addition the mail has "
                    + "failed to be sent successfully. Forcing status to [{}]", messageId, MailState.FAILED);
            }
        } catch (MailStoreException e) {
            this.logger.error("Error when looking for a previous mail status for message id [{}]. In addition the mail "
                + "has failed to be sent successfully. Forcing status to [{}]", messageId, MailState.FAILED, e);
            status = null;
        }

        if (status != null) {
            status.setState(MailState.FAILED);
        } else {
            status = createMailStatus(message, MailState.FAILED, parameters);
        }

        status.setError(exception);
        saveStatus(status, parameters);
    }

    @Override
    public MailStatusResult getMailStatusResult()
    {
        return this.mailStatusResult;
    }

    private String getMessageId(MimeMessage message)
    {
        return getSafeHeader("X-MailID", message);
    }

    private MailStatus loadMailStatus(String messageId, Map<String, Object> parameters) throws MailStoreException
    {
        MailStatus status;
        List<MailStatus> statuses = this.mailStatusStore.load(
            Collections.<String, Object>singletonMap("id", messageId), 0, 0);
        if (statuses.isEmpty()) {
            status = null;
        } else {
            status = statuses.get(0);
        }
        return status;
    }

    private void saveStatus(MailStatus status, Map<String, Object> parameters)
    {
        try {
            this.mailStatusStore.save(status, parameters);
        } catch (MailStoreException e) {
            // Failed to save the status in the DB, we continue but log an error
            this.logger.error("Failed to save mail status [{}] to the database", status, e);
        }
    }

    private void deleteStatus(MailStatus status, Map<String, Object> parameters)
    {
        try {
            this.mailStatusStore.delete(status.getMessageId(), parameters);
        } catch (MailStoreException e) {
            // Failed to delete the status in the DB, we continue but log an error
            this.logger.error("Failed to delete mail status [{}] from the database", status, e);
        }
    }

    private String getSafeHeader(String headerName, MimeMessage message)
    {
        try {
            return message.getHeader(headerName, null);
        } catch (MessagingException e) {
            // This cannot happen in practice since the implementation never throws any exception!
            this.logger.error("Failed to retrieve [{}] header from the message.", headerName, e);
            return null;
        }
    }

    private MailStatus createMailStatus(MimeMessage message, MailState state, Map<String, Object> parameters)
    {
        MailStatus status = new MailStatus(message, state);
        status.setWiki((String) parameters.get("wikiId"));
        return status;
    }

    private void deleteMailContent(MailStatus currentStatus)
    {
        if (currentStatus != null) {
            try {
                this.mailContentStore.delete(currentStatus.getBatchId(), currentStatus.getMessageId());
            } catch (MailStoreException e) {
                // Failed to delete saved mail, raise a warning but continue since it's not critical
                this.logger.warn("Failed to remove previously failing message [{}] (batch id [{}]) from the file "
                    + "system. Reason [{}].", currentStatus.getMessageId(), currentStatus.getBatchId(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
