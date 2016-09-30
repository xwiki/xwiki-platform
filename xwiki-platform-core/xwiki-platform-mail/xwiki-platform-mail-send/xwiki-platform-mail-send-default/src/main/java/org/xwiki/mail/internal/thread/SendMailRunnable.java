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
package org.xwiki.mail.internal.thread;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;

import com.xpn.xwiki.XWikiContext;

/**
 * Runnable that regularly check for mails on a Queue, and for each mail tries to send it.
 *
 * @version $Id$
 * @since 6.4
 */
@Component
@Named("send")
@Singleton
public class SendMailRunnable extends AbstractMailRunnable
{
    @Inject
    private MailQueueManager<SendMailQueueItem> sendMailQueueManager;

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private Transport currentTransport;

    private Session currentSession;

    private int count;

    @Override
    public void run()
    {
        try {
            // Make sure we initialize an execution context.
            prepareContext();

            runInternal();
        } catch (ExecutionContextException e) {
            // Not much to do but log.
            logger.error("Failed to initialize the send mail thread's execution context", e);
        } finally {
            closeTransport();
        }
    }

    private void prepareContext() throws ExecutionContextException
    {
        // Create a single execution context and use it for the send mail thread.
        ExecutionContext ec = new ExecutionContext();
        this.executionContextManager.initialize(ec);
    }

    private void prepareContextForQueueItem(SendMailQueueItem mailItem)
    {
        // Set the current wiki in the context. This is needed for example to be able to locate the configuration
        // properties when processing the mail queue items (in waitSendWaitTime()).
        XWikiContext xcontext = this.contextProvider.get();
        xcontext.setWikiId(mailItem.getWikiId());
    }

    private void runInternal()
    {
        do {
            try {
                // Handle next message in the queue
                if (this.sendMailQueueManager.hasMessage()) {
                    // Important: only remove the mail item from the queue after the mail has been sent as
                    // otherwise, MailSender.waitTillSent() may return before the mail is actually sent!
                    SendMailQueueItem mailItem = this.sendMailQueueManager.peekMessage();
                    try {
                        sendMail(mailItem);
                    } finally {
                        this.sendMailQueueManager.removeMessageFromQueue(mailItem);
                    }
                    // Email throttling: Wait before processing the next mail queue item
                    // Note: it's important that we wait after the previous item has been removed from the queue in
                    // order to let users know as soon as possible that their mail has been sent (otherwise when sending
                    // a synchronous mail, the user would have to wait the send wait time!).
                    waitSendWaitTime();
                }
                // Note: a short pause to catch thread interruptions and to be kind on CPU.
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                // Thread has been stopped, exit
                this.logger.debug("Mail Sender Thread was forcefully stopped", e);
                break;
            } catch (Exception e) {
                // There was an unexpected problem, we just log the problem but keep the thread alive!
                this.logger.error("Unexpected error in the Mail Sender Thread", e);
            }
        } while (!this.shouldStop);
    }

    /**
     * Send the mail.
     *
     * @param item the queue item containing all the data for sending the mail
     */
    protected void sendMail(SendMailQueueItem item)
    {
        prepareContextForQueueItem(item);

        MailListener listener = item.getListener();

        ExtendedMimeMessage message;
        try {
            // Step 1: Load the message from the filesystem store
            message = this.mailContentStore.load(item.getSession(), item.getBatchId(), item.getUniqueMessageId());
        } catch (Exception e) {
            if (listener != null) {
                listener.onSendMessageFatalError(item.getUniqueMessageId(), e, Collections.<String, Object>emptyMap());
            }
            return;
        }

        try {
            // Step 2: If the current Session in use is different from the one passed then close
            // the current Transport, get a new one and reconnect.
            // Also do that every 100 mails sent.
            // TODO: explain why!
            if (item.getSession() != this.currentSession || (this.count % 100) == 0) {
                closeTransport();
                this.currentSession = item.getSession();
                this.currentTransport = this.currentSession.getTransport("smtp");
                this.currentTransport.connect();
            } else if (!this.currentTransport.isConnected()) {
                this.currentTransport.connect();
            }

            // Step 3: Send the mail
            // Unlike the static send method, the sendMessage method does not call the saveChanges method on the
            // message; this prevent the MessageID header to be changed.
            this.currentTransport.sendMessage(message, message.getAllRecipients());
            this.count++;

            // Step 4: Notify the user of the success if a listener has been provided
            if (listener != null) {
                listener.onSendMessageSuccess(message, Collections.<String, Object>emptyMap());
            }
        } catch (Exception e) {
            // An error occurred, notify the user if a listener has been provided.
            if (listener != null) {
                listener.onSendMessageError(message, e, Collections.<String, Object>emptyMap());
            }
        }
    }

    private void waitSendWaitTime() throws InterruptedException
    {
        long sendWaitTime = this.configuration.getSendWaitTime();
        Thread.sleep(sendWaitTime);
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
