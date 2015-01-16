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
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;

/**
 * Default implementation using the {@link DefaultMailSenderRunnable} to send emails asynchronously.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultMailSender implements MailSender, Initializable
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    private MailSenderRunnable mailSenderRunnable;

    @Inject
    private MailQueueManager mailQueueManager;

    private Thread mailSenderThread;

    @Override
    public void initialize() throws InitializationException
    {
        // Start the Mail Sending Thread
        this.mailSenderThread = new Thread(this.mailSenderRunnable);
        this.mailSenderThread.setName("Mail Sender Thread");
        this.mailSenderThread.start();
    }

    @Override
    public MailResult sendAsynchronously(Iterable<? extends MimeMessage> messages, Session session,
        MailListener listener)
    {
        UUID batchId = UUID.randomUUID();

        // Pass the current wiki so that the mail message will be prepared and sent in the context of that wiki.
        String wikiId = this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();

        this.mailQueueManager.addToQueue(new MailSenderQueueItem(messages, session, listener, batchId, wikiId));

        return new DefaultMailResult(batchId, this.mailQueueManager);
    }

    /**
     * Stops the sending thread. Should be called when the application is stopped for a clean shutdown.
     *
     * @throws InterruptedException if the thread failed to be stopped
     */
    public void stopMailSenderThread() throws InterruptedException
    {
        this.mailSenderRunnable.stopProcessing();
        // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
        this.mailSenderThread.interrupt();
        // Wait till the thread goes away
        this.mailSenderThread.join();
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
