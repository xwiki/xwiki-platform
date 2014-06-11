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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.mail.MailResultListener;
import org.xwiki.mail.MailSender;

@Component
@Singleton
public class DefaultMailSender implements MailSender, Initializable
{
    @Inject
    private MailSenderThread mailSenderThread;

    @Inject
    private Logger logger;

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
    public void send(MimeMessage message, Session session) throws MessagingException
    {
        send(message, session, null);
    }

    @Override
    public void send(MimeMessage message, Session session, MailResultListener listener) throws MessagingException
    {
        // Push new mail message on the queue
        getMailQueue().add(new MailSenderQueueItem(message, session, listener));
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
        while (!getMailQueue().isEmpty() || System.currentTimeMillis() - startTime < timeout) {
            System.out.println("check " + (System.currentTimeMillis() - startTime));
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

    /**
     * Stops the sending thread. Should be called when the application is stopped for a clean shutdown.
     * @throws InterruptedException if the thread failed to be stopped
     */
    public void stopMailSenderThread() throws InterruptedException
    {
        this.mailSenderThread.stopProcessing();
        // Wait till the thread goes away
        this.mailSenderThread.join();
    }
}
