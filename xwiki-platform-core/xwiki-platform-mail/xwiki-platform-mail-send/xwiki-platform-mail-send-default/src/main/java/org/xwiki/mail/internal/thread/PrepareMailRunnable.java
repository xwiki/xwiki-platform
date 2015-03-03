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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.script.MimeMessageWrapper;

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

    private static final String WIKI_PARAMETER_KEY = "wikiId";

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
    protected void prepareMail(PrepareMailQueueItem item) throws ExecutionContextException
    {
        Iterator<? extends MimeMessage> messages = item.getMessages().iterator();

        // We prepare a new Execution Context for each mail so that one mail doesn't interfere with another
        // Note that we need to have the hasNext() call after the context is ready since the implementation can need
        // a valid XWiki Context.
        boolean shouldStop = false;
        while (!shouldStop) {
            prepareContext(item.getWikiId());
            try {
                if (messages.hasNext()) {
                    MimeMessage mimeMessage = messages.next();
                    // Skip message is message has failed to be created.
                    if (mimeMessage != null) {
                        prepareSingleMail(mimeMessage, item);
                    } else {
                        // We can't call a listener here because the message is null. Thus we simply log an error.
                        this.logger.error("Failed to prepare message for [{}]", item);
                    }
                } else {
                    shouldStop = true;
                }
            } finally {
                removeContext();
            }
        }
    }

    private void prepareSingleMail(MimeMessage mimeMessage, PrepareMailQueueItem item)
    {
        MimeMessage message = mimeMessage;
        MailListener listener = item.getListener();
        try {
            // Step 1: Create the MimeMessage
            message = initializeMessage(mimeMessage, listener, item.getBatchId(), item.getWikiId());
            if (message != null) {
                // Step 2: Persist the MimeMessage
                this.mailContentStore.save(message);
                // Step 3: Put the MimeMessage id on the Mail Send Queue for sending
                this.sendMailQueueManager.addToQueue(new SendMailQueueItem(message.getHeader(HEADER_MAIL_ID, null),
                    item.getSession(), listener, item.getBatchId(), item.getWikiId()));
                // Step 4: Notify the user that the MimeMessage is prepared
                if (listener != null) {
                    listener.onPrepare(message,
                        Collections.singletonMap(WIKI_PARAMETER_KEY, (Object) item.getWikiId()));
                }
            }
        } catch (Exception e) {
            // An error occurred, notify the user if a listener has been provided
            if (listener != null) {
                listener.onError(message, e, Collections.singletonMap(WIKI_PARAMETER_KEY, (Object) item.getWikiId()));
            }
        }
    }

    private MimeMessage initializeMessage(MimeMessage mimeMessage, MailListener listener, String batchId, String wikiId)
        throws Exception
    {
        MimeMessage message;

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
                // JavaMail won't be able to send the mail but we'll get the error in the status.
            }
        }

        // If the user has not set the BCC header then use the default value from configuration
        Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
        if (bccAddresses == null || bccAddresses.length == 0) {
            for (String address : this.configuration.getBCCAddresses()) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(address));
            }
        }

        return message;
    }

    private void setCustomHeaders(MimeMessage message, String batchId) throws MessagingException
    {
        // Set custom XWiki mail headers.
        // See #HEADER_MAIL_ID and #HEADER_BATCH_ID
        // If the Batch Id is already set, then don't generate one. This is what happens for example when a serialized
        // MimeMessage is loaded to be resent. This allows for example to remove the serialized messages after it's
        // resent.
        if (message.getHeader(HEADER_BATCH_ID, null) == null) {
            message.setHeader(HEADER_BATCH_ID, batchId);
        }

        // If the message Id is already set, then don't generate an id. This is what happens for example when a
        // serialized MimeMessage is loaded to be resent.
        if (message.getHeader(HEADER_MAIL_ID, null) == null)
        {
            message.setHeader(HEADER_MAIL_ID, UUID.randomUUID().toString());
        }
    }
}
