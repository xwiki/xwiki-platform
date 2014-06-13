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
package org.xwiki.mail.internal.script;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.internet.MimeMessage;

import org.xwiki.context.Execution;
import org.xwiki.mail.MailResultListener;

/**
 * Saves errors when sending messages in the Execution Context so that it's Thread safe.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ScriptMailSenderListener implements MailResultListener
{
    private static final String CONTEXT_KEY = "mailsenderExceptions";

    private Execution execution;

    public ScriptMailSenderListener(Execution execution)
    {
        this.execution = execution;
    }

    @Override
    public void onSuccess(MimeMessage message)
    {
        // We're only interested in errors in the scripting API.
    }

    @Override
    public void onError(MimeMessage message, Throwable t)
    {
        // Create the queue on the first error (that saves memory and there shouldn't be any threading issue since
        // we're using the Execution Context which is a ThreadLocal.
        BlockingQueue<Throwable> queue = getExceptionQueue();
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(100);
            this.execution.getContext().setProperty(CONTEXT_KEY, queue);
        }

        // Add the error to the queue
        queue.add(t);
    }

    public BlockingQueue<Throwable> getExceptionQueue()
    {
        BlockingQueue<Throwable> queue =
            (BlockingQueue<Throwable>) this.execution.getContext().getProperty(CONTEXT_KEY);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>(1);
        }
        return queue;
    }
}
