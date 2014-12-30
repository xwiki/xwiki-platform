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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;

/**
 * Default implementation using the {@link org.xwiki.mail.internal.DefaultMailSenderThread} to send emails
 * asynchronously.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultMailSender implements MailSender, Initializable
{
    @Inject
    private MailSenderThread mailSenderThread;

    @Inject
    private MailSenderConfiguration configuration;

    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    /**
     * The Mail queue that the mail sender thread will use to send mails. We use a separate thread to allow sending
     * mail asynchronously.
     */
    private Queue<MailSenderQueueItem> mailQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.mailSenderThread.setName("Mail Sender Thread");
        this.mailSenderThread.startProcessing(getMailQueue());
    }

    @Override
    public UUID send(Iterable<? extends MimeMessage> messages, Session session) throws MessagingException
    {
        MailListener listener = getListener("memory");
        UUID batchID = sendAsynchronously(messages, session, listener);
        waitTillSent(Long.MAX_VALUE);
        return batchID;
    }

    @Override
    public UUID sendAsynchronously(Iterable<? extends MimeMessage> messages, Session session, MailListener listener)
    {
        UUID batchID = UUID.randomUUID();
        getMailQueue().add(new MailSenderQueueItem(messages, session, listener, batchID));
        return batchID;
    }

    /**
     * @return the mail queue containing all pending mails to be sent
     */
    public Queue<MailSenderQueueItem> getMailQueue()
    {
        return this.mailQueue;
    }

    @Override
    public void waitTillSent(long timeout)
    {
        long startTime = System.currentTimeMillis();
        while (hasMailQueueItemForCurrentThread() && System.currentTimeMillis() - startTime < timeout) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                // Ignore but consider that the mail was sent
                this.logger.warn("Interrupted while waiting for mail to be sent. Reason [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
                break;
            }
        }
    }

    private boolean hasMailQueueItemForCurrentThread()
    {
        Iterator<MailSenderQueueItem> iterator = getMailQueue().iterator();
        while (iterator.hasNext()) {
            MailSenderQueueItem item = iterator.next();
            if (Thread.currentThread().getId() == item.getThreadId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stops the sending thread. Should be called when the application is stopped for a clean shutdown.
     *
     * @throws InterruptedException if the thread failed to be stopped
     */
    public void stopMailSenderThread() throws InterruptedException
    {
        this.mailSenderThread.stopProcessing();
        // Wait till the thread goes away
        this.mailSenderThread.join();
    }

    private MailListener getListener(String hint) throws MessagingException
    {
        MailListener listener;
        try {
            listener = this.componentManager.getInstance(MailListener.class, hint);
        } catch (ComponentLookupException e) {
            throw new MessagingException(String.format("Failed to locate [%s] Event listener. ", hint), e);
        }
        return listener;
    }
}
