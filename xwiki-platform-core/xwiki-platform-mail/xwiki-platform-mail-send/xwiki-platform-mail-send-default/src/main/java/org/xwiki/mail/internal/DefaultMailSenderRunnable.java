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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.script.MimeMessageWrapper;

import com.xpn.xwiki.XWikiContext;

/**
 * Runnable that regularly check for mails on a Queue, and for each mail tries to send it.
 *
 * @version $Id$
 * @since 6.4M3
 */
@Component
@Singleton
public class DefaultMailSenderRunnable implements MailSenderRunnable
{
    /**
     * Name of the custom JavaMail header used by XWiki to uniquely identify a mail. This header allows us to follow
     * the state of the mail (prepared, sent successfully or failed to be sent).
     * <p/>
     * Note that we wanted to use the standard "Message-ID" header but unfortunately the JavaMail implementation
     * modifies this header's value when the mail is sent (even if you have called
     * {@link javax.mail.internet.MimeMessage#saveChanges()}!). Thus if we want to save the mail's state before the
     * mail is sent and then update it after it's been sent we won't find the same id...
     */
    private static final String HEADER_MAIL_ID = "X-MailID";

    /**
     * Name of the custom JavaMail header used by XWiki to uniquely identify a group of emails being sent together.
     * This can be seen as representing a mail sending session. This makes it easier to list the status of all mails
     * sent together in a session for example or to resend failed mails from a specific session.
     */
    private static final String HEADER_BATCH_ID = "X-BatchID";

    @Inject
    private Logger logger;

    @Inject
    private MailQueueManager mailQueueManager;

    @Inject
    private MailSenderConfiguration configuration;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Allows to stop this thread, used in {@link #stopProcessing()}.
     */
    private volatile boolean shouldStop;

    private Transport currentTransport;

    private Session currentSession;

    private int count;

    @Override
    public void run()
    {
        try {
            runInternal();
        } finally {
            closeTransport();
        }
    }

    private void runInternal()
    {
        do {
            try {
                // Handle next message in the queue
                if (this.mailQueueManager.hasMessageToSend()) {
                    // Important: only remove the mail item from the queue after the mail has been sent as
                    // otherwise, MailSender.waitTillSent() may return before the mail is actually sent!
                    MailSenderQueueItem mailItem = this.mailQueueManager.peekMessage();
                    try {
                        sendMail(mailItem);
                    } finally {
                        this.mailQueueManager.removeMessageFromQueue(mailItem);
                    }
                }
                // Make some pause to not overload the server
                Thread.sleep(100L);
            } catch (Exception e) {
                // There was an unexpected problem, we stop this thread and log the problem.
                this.logger.debug("Mail Sender Thread was forcefully stopped", e);
                break;
            }
        } while (!this.shouldStop);
    }

    /**
     * Prepare the contexts for sending the mail.
     *
     * @param mailItem the queue item containing all the data for sending the mail
     */
    protected void sendMail(MailSenderQueueItem mailItem) throws ExecutionContextException
    {
        // Isolate the context when sending a mail by creating a new context
        ExecutionContext executionContext = new ExecutionContext();
        this.executionContextManager.initialize(executionContext);

        // Since the Execution Context has been created there's no XWikiContext in it and we initialize one
        XWikiContext xwikiContext = this.xwikiContextProvider.get();

        // Set the wiki in which to execute
        xwikiContext.setWikiId(mailItem.getWikiId());

        sendMailInternal(mailItem);
    }

    /**
     * Send the mail.
     *
     * @param item the queue item containing all the data for sending the mail
     */
    private void sendMailInternal(MailSenderQueueItem item)
    {
        Iterator<? extends MimeMessage> messages = item.getMessages().iterator();
        MailListener listener = item.getListener();
        UUID batchId = item.getBatchId();

// TODO: this is not correct I think, we need to prepare all the mails before sending them as otherwise they'll be in
// memory till they are sent... and if XWiki crashes they're lost and in any case we won't have any status for them
// till their turn comes which isn't good!
// ==> we need to serialize them all and reload them when their turn comes...

        while (messages.hasNext()) {
            MimeMessage mimeMessage = messages.next();
            MimeMessage message = initializeMessage(mimeMessage, listener, batchId);
            try {
                // Step 1: If the current Session in use is different from the one passed then close
                // the current Transport, get a new one and reconnect.
                // Also do that every 100 mails sent.
                // TODO: explain why!
                // TODO: Also explain why we don't use Transport.send()
                if (item.getSession() != this.currentSession || (this.count % 100) == 0) {
                    closeTransport();
                    this.currentSession = item.getSession();
                    this.currentTransport = this.currentSession.getTransport("smtp");
                    this.currentTransport.connect();
                } else if (!this.currentTransport.isConnected()) {
                    this.currentTransport.connect();
                }

                // Step 2: Send the mail
                this.currentTransport.sendMessage(message, message.getAllRecipients());
                this.count++;

                // Step 3: Notify the user of the success if a listener has been provided
                if (listener != null) {
                    listener.onSuccess(message);
                }
            } catch (Exception e) {
                // An error occurred, notify the user if a listener has been provided
                if (listener != null) {
                    listener.onError(message, e);
                }
            }
        }
    }

    private MimeMessage initializeMessage(MimeMessage mimeMessage, MailListener listener, UUID batchId)
    {
        MimeMessage message = null;
        try {
            if ((mimeMessage instanceof MimeMessageWrapper)) {
                message = ((MimeMessageWrapper) mimeMessage).getMessage();
            } else {
                message = mimeMessage;
            }

            setCustomHeaders(message, batchId);

            // Note: We don't cache the default From and BCC addresses because they can be modified at runtime
            // (from the Admin UI for example) and we need to always get the latest configured values.

            // If the user has not set the From header then use the default value from configuration and if it's not
            // set then raise an error since a message must have a from set!
            // Perform some basic verification to avoid NPEs in JavaMail
            if (message.getFrom() == null) {
                // Try using the From address in the Session
                String from = this.configuration.getFromAddress();
                if (from != null) {
                    message.setFrom(new InternetAddress(from));
                } else {
                    throw new MessagingException("Missing the From Address for sending the mail. "
                        + "You need to either define it in the Mail Configuration or pass it in your message.");
                }
            }

            // If the user has not set the BCC header then use the default value from configuration
            Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
            if (bccAddresses == null || bccAddresses.length == 0) {
                for (String address : this.configuration.getBCCAddresses()) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
                }
            }

            // Mail ready to be sent, notify the user if a listener has been provided
            if (listener != null) {
                listener.onPrepare(message);
            }
        } catch (MessagingException e) {
            // An error occurred, notify the user if a listener has been provided
            if (listener != null) {
                listener.onError(message, e);
            }
        }
        return message;
    }

    private void setCustomHeaders(MimeMessage message, UUID batchId) throws MessagingException
    {
        // Set custom XWiki mail headers.
        // See #HEADER_MAIL_ID and #HEADER_BATCH_ID
        message.setHeader(HEADER_BATCH_ID, batchId.toString());
        //For serialized message we keep the existent #HEADER_MAIL_ID
        if (message.getHeader(HEADER_MAIL_ID, null) == null)
        {
            message.setHeader(HEADER_MAIL_ID, UUID.randomUUID().toString());
        }
    }

    @Override
    public void stopProcessing()
    {
        this.shouldStop = true;
    }

    private void closeTransport()
    {
        if (this.currentTransport != null) {
            try {
                this.currentTransport.close();
            } catch (MessagingException e) {
                this.logger.warn("Failed to close JavaMail Transport connection. Reason [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
