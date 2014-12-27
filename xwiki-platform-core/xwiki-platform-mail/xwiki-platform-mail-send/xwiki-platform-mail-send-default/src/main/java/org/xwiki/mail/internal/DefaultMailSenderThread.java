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
import java.util.Queue;
import java.util.UUID;

import javax.inject.Inject;
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
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.script.MimeMessageWrapper;

/**
 * Thread that regularly check for mails on a Queue, and for each mail tries to send it.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultMailSenderThread extends Thread implements MailSenderThread
{
    @Inject
    private Logger logger;

    @Inject
    private MailSenderConfiguration configuration;

    /**
     * The queue containing mails to send.
     */
    private Queue<MailSenderQueueItem> mailQueue;

    /**
     * Allows to stop this thread, used in {@link #stopProcessing()}.
     */
    private volatile boolean shouldStop;

    private Transport currentTransport;

    private Session currentSession;

    private int count;

    @Override
    public void startProcessing(Queue<MailSenderQueueItem> mailQueue)
    {
        this.mailQueue = mailQueue;
        start();
    }

    @Override
    public void run(Queue<MailSenderQueueItem> mailQueue)
    {
        this.mailQueue = mailQueue;
        run();
    }

    @Override
    public void run()
    {
        try {
            do {
                // Handle next message in the queue
                if (!this.mailQueue.isEmpty()) {
                    // Important: only remove the mail item from the queue after the mail has been sent as otherwise,
                    // MailSender.waitTillSent() may return before the mail is actually sent!
                    MailSenderQueueItem mailItem = this.mailQueue.peek();
                    try {
                        sendMail(mailItem);
                    } finally {
                        this.mailQueue.remove(mailItem);
                    }
                }
                // Make some pause to not overload the server
                try {
                    DefaultMailSenderThread.sleep(100L);
                } catch (Exception e) {
                    // There was an unexpected problem, we stop this thread and log the problem.
                    this.logger.debug("Mail Sender Thread was forcefully stopped", e);
                    break;
                }
            } while (!this.shouldStop);
        } finally {
            closeTransport();
        }
    }

    /**
     * Stop the thread.
     */
    public void stopProcessing()
    {
        this.shouldStop = true;
        // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
        interrupt();
    }

    /**
     * Send the mail.
     *
     * @param item the queue item containing all the data for sending the mail
     */
    protected void sendMail(MailSenderQueueItem item)
    {
        Iterator<? extends MimeMessage> messages = item.getMessages().iterator();
        MailListener listener = item.getListener();
        UUID batchID = item.getBatchID();
        while (messages.hasNext()) {
            MimeMessage mimeMessage = messages.next();
            MimeMessage message = initializeMessage(mimeMessage, listener, batchID);
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

    private MimeMessage initializeMessage(MimeMessage mimeMessage, MailListener listener, UUID batchID)
    {
        MimeMessage message = null;
        try {
            if ((mimeMessage instanceof MimeMessageWrapper)) {
                message = ((MimeMessageWrapper) mimeMessage).getMessage();
            } else {
                message = mimeMessage;
            }

            // If the user has not set the From header then use the default value from configuration and if it's not
            // set then raise an error since a message must have a from set!
            message.setHeader("X-BatchID", batchID.toString());
            message.setHeader("X-MailID", UUID.randomUUID().toString());
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
