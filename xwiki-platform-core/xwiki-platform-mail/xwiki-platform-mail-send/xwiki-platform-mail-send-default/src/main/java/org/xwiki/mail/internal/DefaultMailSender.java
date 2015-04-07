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

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.internal.thread.MailQueueManager;
import org.xwiki.mail.internal.thread.PrepareMailQueueItem;
import org.xwiki.mail.internal.thread.MailRunnable;
import org.xwiki.mail.internal.thread.SendMailQueueItem;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;

/**
 * Default implementation using the {@link org.xwiki.mail.internal.thread.SendMailRunnable} to send emails asynchronously.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultMailSender implements MailSender, Initializable
{
    private static final String SESSION_BATCHID_KEY = "xwiki.batchId";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    @Named("prepare")
    private MailRunnable prepareMailRunnable;

    @Inject
    @Named("send")
    private MailRunnable sendMailRunnable;

    @Inject
    private MailQueueManager<PrepareMailQueueItem> prepareMailQueueManager;

    @Inject
    private MailQueueManager<SendMailQueueItem> sendMailQueueManager;

    private Thread prepareMailThread;

    private Thread sendMailThread;

    @Override
    public void initialize() throws InitializationException
    {
        // Step 1: Start the Mail Prepare Thread
        this.prepareMailThread = new Thread(this.prepareMailRunnable);
        this.prepareMailThread.setName("Mail Prepare Thread");
        this.prepareMailThread.start();

        // Step 2: Start the Mail Sender Thread
        this.sendMailThread = new Thread(this.sendMailRunnable);
        this.sendMailThread.setName("Mail Sender Thread");
        this.sendMailThread.start();
    }

    @Override
    public MailResult sendAsynchronously(Iterable<? extends MimeMessage> messages, Session session,
        MailListener listener)
    {
        // If the session has specified a batch id, then use it! This can be used for example when resending email.
        String batchId = session.getProperty(SESSION_BATCHID_KEY);
        if (batchId == null) {
            batchId = UUID.randomUUID().toString();
        }

        // Pass the current wiki so that the mail message will be prepared and later sent in the context of that wiki.
        String wikiId = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();

        this.prepareMailQueueManager.addToQueue(new PrepareMailQueueItem(messages, session, listener, batchId, wikiId));

        return new DefaultMailResult(batchId, this.sendMailQueueManager);
    }

    /**
     * Stops the Mail Prepare and Sender threads. Should be called when the application is stopped for a clean shutdown.
     *
     * @throws InterruptedException if a thread fails to be stopped
     */
    public void stopMailThreads() throws InterruptedException
    {
        // Step 1: Stop the Mail Sender Thread

        this.sendMailRunnable.stopProcessing();
        // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
        this.sendMailThread.interrupt();
        // Wait till the thread goes away
        this.sendMailThread.join();

        // Step 2: Stop the Mail Prepare Thread

        this.prepareMailRunnable.stopProcessing();
        // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
        this.prepareMailThread.interrupt();
        // Wait till the thread goes away
        this.prepareMailThread.join();
    }

    private MailListener getListener(String hint) throws MessagingException
    {
        MailListener listener;
        try {
            listener = this.componentManager.getInstance(MailListener.class, hint);
        } catch (ComponentLookupException e) {
            throw new MessagingException(String.format("Failed to locate Mail listener [%s].", hint), e);
        }
        return listener;
    }
}
