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

import java.util.Queue;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.MailResultListener;

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
                    MailSenderQueueItem mailItem = this.mailQueue.poll();
                    try {
                        sendMail(mailItem);
                    } catch (Exception e) {
                        // If any error happens, we log it but we don't stop this thread so that we can send
                        // other mails
                        this.logger.warn("Failed to send mail [{}]. Root reason [{}]", mailItem,
                            ExceptionUtils.getRootCauseMessage(e));
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
        MimeMessage message = item.getMessage();
        MailResultListener listener = item.getListener();

        try {
            // Step 1: If the current Session in use is different from the one passed then close the current Transport,
            // get a new one and reconnect. Also do that every 100 mails sent.
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
        } catch (MessagingException e) {
            // An error occurred, notify the user if a listener has been provided
            if (listener != null) {
                listener.onError(message, e);
            }
        }
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
