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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.internal.thread.MailQueueManager;
import org.xwiki.mail.internal.thread.MailRunnable;
import org.xwiki.mail.internal.thread.PrepareMailQueueItem;
import org.xwiki.mail.internal.thread.context.Copier;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation using the {@link org.xwiki.mail.internal.thread.SendMailRunnable} to send emails
 * asynchronously.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultMailSender implements MailSender, Initializable, Disposable
{
    /**
     * Logger to use to log shutdown information (opposite of initialization).
     */
    private static final Logger SHUTDOWN_LOGGER = LoggerFactory.getLogger("org.xwiki.shutdown");

    private static final String SESSION_BATCHID_KEY = "xwiki.batchId";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Execution execution;

    @Inject
    @Named("prepare")
    private MailRunnable prepareMailRunnable;

    @Inject
    @Named("send")
    private MailRunnable sendMailRunnable;

    @Inject
    private MailQueueManager<PrepareMailQueueItem> prepareMailQueueManager;

    @Inject
    private Copier<ExecutionContext> executionContextCloner;

    private Thread prepareMailThread;

    private Thread sendMailThread;

    @Override
    public void initialize() throws InitializationException
    {
        // Step 1: Start the Mail Prepare Thread
        this.prepareMailThread = new Thread(this.prepareMailRunnable);
        this.prepareMailThread.setName("Mail Prepare Thread");
        this.prepareMailThread.setDaemon(true);
        this.prepareMailThread.start();

        // Step 2: Start the Mail Sender Thread
        this.sendMailThread = new Thread(this.sendMailRunnable);
        this.sendMailThread.setName("Mail Sender Thread");
        this.sendMailThread.setDaemon(true);
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

        // Pass a clone of the current execution context so that the mail message will be prepared and later sent in the
        // same context, but in read-only mode (i.e. the preparation of the mail will not impact the current thread's
        // context).
        ExecutionContext executionContext = this.execution.getContext();
        ExecutionContext clonedExecutionContext = this.executionContextCloner.copy(executionContext);

        // TODO: Remove once we've found the reason for the functional Mail test flicker: from time to time the wiki
        // is not set in the Mail Status LT
        XWikiContext xcontext = (XWikiContext) clonedExecutionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        if (xcontext.getWikiId() == null) {
            throw new RuntimeException(String.format("Aborting Mail Sending: the Wiki Id must not be null in the "
                + "XWiki Context. Got [%s] in the original context.",
                ((XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).getWikiId()));
        }

        this.prepareMailQueueManager.addToQueue(new PrepareMailQueueItem(messages, session, listener, batchId,
            clonedExecutionContext));

        return new DefaultMailResult(batchId);
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
        SHUTDOWN_LOGGER.debug(String.format("Mail Prepare Thread has been stopped"));

        // Step 2: Stop the Mail Prepare Thread

        this.prepareMailRunnable.stopProcessing();
        // Make sure the Thread goes out of sleep if it's sleeping so that it stops immediately.
        this.prepareMailThread.interrupt();
        // Wait till the thread goes away
        this.prepareMailThread.join();
        SHUTDOWN_LOGGER.debug(String.format("Mail Sender Thread has been stopped"));
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

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            stopMailThreads();
        } catch (InterruptedException e) {
            SHUTDOWN_LOGGER.debug("Mail threads shutdown has been interruped", e);
        }
    }
}
