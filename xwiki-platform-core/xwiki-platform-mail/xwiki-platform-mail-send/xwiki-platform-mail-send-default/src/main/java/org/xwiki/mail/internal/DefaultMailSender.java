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
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.internal.thread.MailQueueManager;
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
public class DefaultMailSender implements MailSender
{
    private static final String SESSION_BATCHID_KEY = "xwiki.batchId";

    private static final int TIMEOUT = 60;

    @Inject
    private Execution execution;

    @Inject
    private MailQueueManager<PrepareMailQueueItem> prepareMailQueueManager;

    @Inject
    private Copier<ExecutionContext> executionContextCloner;

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

        PrepareMailQueueItem pmqi = new PrepareMailQueueItem(messages, session, listener, batchId,
            clonedExecutionContext);
        try {
            // Note: addMessageToQueue() will throw an InterruptedException exception if the add is interrupted or
            // will return false if the message couldn't be added before the timeout expired. This means that the
            // message is lost, so we throw an exception to fail the calling code so that the user can know what
            // happened.
            boolean result = this.prepareMailQueueManager.addMessageToQueue(pmqi, TIMEOUT, TimeUnit.SECONDS);
            if (!result) {
                // Since the prepare queue is still full after waiting the timeout time (it shouldn't take that time to
                // process a message and thus to remove one message from the prepare queue; and if it takes more than
                // that there's something seriously wrong going on), report a critical error in the form of a runtime
                // exception so that the calling code fails hard, and so that the error bubbles up to the user, as
                // something is seriously wrong.
                throw new RuntimeException(String.format("The mail prepare queue is still full after waiting [%s] "
                    + "[%s]. The following messages will be lost: [%s]...", TIMEOUT, TimeUnit.SECONDS, pmqi));
            }
        } catch (InterruptedException e) {
            // The add was interrupted, restore the interrupted state for the current thread, to be a good citizen, and
            // to propagate the interrupted state.
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Mail items couldn't be added to the prepare queue as it was "
                + "interrupted. The following messages will be lost: [%s]...", pmqi), e);
        }

        return new DefaultMailResult(batchId);
    }
}
