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
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.internal.UpdateableMailStatusResult;

import com.xpn.xwiki.XWikiContext;

/**
 * Runnable that regularly check for mail items on a Prepare Queue, and for each mail item there, generate the message
 * to send and persist it and put that reference on the Send Queue for sending.
 *
 * @version $Id$
 * @since 6.4
 */
@Component
@Named("prepare")
@Singleton
public class PrepareMailRunnable extends AbstractMailRunnable
{
    @Inject
    private MailQueueManager<PrepareMailQueueItem> prepareMailQueueManager;

    @Inject
    private MailQueueManager<SendMailQueueItem> sendMailQueueManager;

    @Inject
    @Named("filesystem")
    private MailContentStore mailContentStore;

    @Override
    public void run()
    {
        do {
            try {
                // Handle next message in the queue
                if (this.prepareMailQueueManager.hasMessage()) {
                    // Important: only remove the mail item after the message has been created and put on the sender
                    // queue.
                    PrepareMailQueueItem mailItem = this.prepareMailQueueManager.peekMessage();
                    try {
                        prepareMail(mailItem);
                    } finally {
                        this.prepareMailQueueManager.removeMessageFromQueue(mailItem);
                    }
                }
                // Note: a short pause to catch thread interruptions and to be kind on CPU.
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                // Thread has been stopped, exit
                this.logger.debug("Mail Prepare Thread was forcefully stopped", e);
                break;
            } catch (Exception e) {
                // There was an unexpected problem, we just log the problem but keep the thread alive!
                this.logger.error("Unexpected error in the Mail Prepare Thread", e);
            }
        } while (!this.shouldStop);
    }

    /**
     * Prepare the messages to send, persist them and put them on the Mail Sender Queue.
     *
     * @param item the queue item containing all the data for sending the mail
     * @throws org.xwiki.context.ExecutionContextException when the XWiki Context fails to be set up
     */
    protected void prepareMail(PrepareMailQueueItem item)
    {
        Iterator<? extends MimeMessage> messageIterator = item.getMessages().iterator();
        MailListener listener = item.getListener();

        if (listener != null) {
            listener.onPrepareBegin(item.getBatchId(), Collections.emptyMap());
        }

        // Count the total number of messages to process
        long messageCounter = 0;

        try {
            boolean shouldStop = false;
            while (!shouldStop) {
                // Note that we need to have the hasNext() call after the context is ready since the implementation can
                // need a valid XWiki Context.
                prepareContext(item.getContext());
                try {
                    if (messageIterator.hasNext()) {
                        MimeMessage mimeMessage = messageIterator.next();
                        prepareSingleMail(mimeMessage, item);
                        messageCounter++;
                    } else {
                        shouldStop = true;
                    }
                } finally {
                    removeContext();
                }
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onPrepareFatalError(e, Collections.emptyMap());
            }
        } finally {
            if (listener != null) {
                MailStatusResult result = listener.getMailStatusResult();
                // Update the listener with the total number of messages prepared so that the user can known when
                // all the messages have been processed for the batch. We update here, even in case of failure
                // so that waiting process have a chance to see an end.
                if (result instanceof UpdateableMailStatusResult) {
                    ((UpdateableMailStatusResult) result).setTotalSize(messageCounter);
                }
                listener.onPrepareEnd(Collections.emptyMap());
            }
        }
    }

    protected void prepareContext(ExecutionContext executionContext) throws ExecutionContextException
    {
        try {
            this.execution.setContext(executionContext);
        } catch (Exception e) {
            // If inheritance fails, we will get an unchecked exception here. So we'll wrap it in an
            // ExecutionContextException.
            throw new ExecutionContextException("Failed to set the execution context.", e);
        }
    }

    private void prepareSingleMail(MimeMessage mimeMessage, PrepareMailQueueItem item)
    {
        MailListener listener = item.getListener();

        // Step 1: Try to complete message with From and Bcc from configuration if needed
        completeMessage(mimeMessage);

        // Ensure mimeMessage to be extended
        ExtendedMimeMessage message = ExtendedMimeMessage.wrap(mimeMessage);

        // Step 2: Persist the MimeMessage
        // Note: Message identifier is stabilized at this step by the serialization process
        try {
            this.mailContentStore.save(item.getBatchId(), message);
        } catch (Exception e) {
            // An error occurred, notify the user if a listener has been provided
            if (listener != null) {
                listener.onPrepareMessageError(message, e, Collections.<String, Object>emptyMap());
            }
            return;
        }

        // Step 3: Notify the user that the MimeMessage is prepared
        if (listener != null) {
            listener.onPrepareMessageSuccess(message, Collections.emptyMap());
        }

        // Step 4: Put the MimeMessage id on the Mail Send Queue for sending
        // Extract the wiki id from the context
        this.sendMailQueueManager.addToQueue(new SendMailQueueItem(message.getUniqueMessageId(),
            item.getSession(), listener, item.getBatchId(), extractWikiId(item)));

    }

    private String extractWikiId(PrepareMailQueueItem item)
    {
        XWikiContext xcontext = (XWikiContext) item.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        return xcontext.getWikiId();
    }

    private void completeMessage(MimeMessage mimeMessage)
    {
        // Note: We don't cache the default From and BCC addresses because they can be modified at runtime
        // (from the Admin UI for example) and we need to always get the latest configured values.

        // If the user has not set the From header then try to use the default value from configuration
        tryToEnsureFrom(mimeMessage);
        // Else JavaMail won't be able to send the mail but we'll get the error in the status.

        // If the user has not set the BCC header then use the default value from configuration
        tryToAddDefaultBccIfNeeded(mimeMessage);
    }

    private void tryToEnsureFrom(MimeMessage mimeMessage)
    {
        if (getFrom(mimeMessage) == null) {
            // Try using the From address in the Session
            String from = this.configuration.getFromAddress();
            if (from != null) {
                try {
                    mimeMessage.setFrom(new InternetAddress(from));
                } catch (MessagingException e) {
                    // ignored
                }
            }
        }
    }

    private void tryToAddDefaultBccIfNeeded(MimeMessage mimeMessage)
    {
        Address[] bccAddresses = getBccRecipients(mimeMessage);
        if (bccAddresses == null || bccAddresses.length == 0) {
            for (String address : this.configuration.getBCCAddresses()) {
                try {
                    mimeMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
                } catch (MessagingException e) {
                    // ignored
                }
            }
        }
    }

    private Address[] getFrom(MimeMessage mimeMessage)
    {
        try {
            return mimeMessage.getFrom();
        } catch (MessagingException e) {
            return null;
        }
    }

    private Address[] getBccRecipients(MimeMessage mimeMessage)
    {
        try {
            return mimeMessage.getRecipients(Message.RecipientType.BCC);
        } catch (MessagingException e) {
            return null;
        }
    }
}
