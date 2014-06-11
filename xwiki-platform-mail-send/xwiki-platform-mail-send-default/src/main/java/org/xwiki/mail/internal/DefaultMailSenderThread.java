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
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.mail.MailSenderErrorEvent;
import org.xwiki.observation.ObservationManager;

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
    private Provider<ObservationManager> observationManagerProvider;

    /**
     * The queue containing mails to send.
     */
    private Queue<Pair<MimeMessage, Session>> mailQueue;

    /**
     * Allows to stop this thread, used in {@link #stopProcessing()}.
     */
    private volatile boolean shouldStop;

    private Transport currentTransport;

    private Session currentSession;

    private int count;

    @Override
    public void startProcessing(Queue<Pair<MimeMessage, Session>> mailQueue)
    {
        this.mailQueue = mailQueue;
        start();
    }

    @Override
    public void run(Queue<Pair<MimeMessage, Session>> mailQueue)
    {
        this.mailQueue = mailQueue;
        run();
    }

    @Override
    public void run()
    {
        try {
            do {
                try {
                    // Handle next message in the queue
                    if (!this.mailQueue.isEmpty()) {
                        Pair<MimeMessage, Session> mailData = this.mailQueue.poll();
                        sendMail(mailData.getLeft(), mailData.getRight());
                    }
                    // Make some pause to not overload the server
                    DefaultMailSenderThread.sleep(100L);
                } catch (Exception e) {
                    // There was an unexpected problem, we stop this thread and log the problem.
                    this.logger.error("Mail Sender Thread was stopped due to some problem", e);
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
     * Send the mail
     */
    protected void sendMail(MimeMessage message, Session session)
    {
        try {
            // Step 1: If the current Session in use is different from the one passed then close the current Transport,
            // get a new one and reconnect. Also do that every 100 mails sent.
            // TODO: explain why!
            // TODO: Also explain why we don't use Transport.send()
            if (session != this.currentSession || (this.count % 100) == 0) {
                closeTransport();
                this.currentSession = session;
                this.currentTransport = session.getTransport("smtp");
                this.currentTransport.connect();
            } else if (!this.currentTransport.isConnected()) {
                this.currentTransport.connect();
            }

            // Step 2: Send the mail
            this.currentTransport.sendMessage(message, message.getAllRecipients());
            this.count++;
        } catch (MessagingException e) {
            sendErrorEvent(message, e);
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

    private void sendErrorEvent(MimeMessage message, MessagingException e)
    {
        // Dynamically look for an Observation Manager and only send the event if one can be found.
        ObservationManager observationManager = this.observationManagerProvider.get();
        if (observationManager != null) {
            observationManager.notify(new MailSenderErrorEvent(), message, e);
        }
    }
}
