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

import org.xwiki.component.annotation.Role;

/**
 * Makes a component out of a Thread.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface MailSenderThread extends Runnable
{
    /**
     * See {@link Thread#setName(String)}.
     *
     * @param threadName see {@link Thread#setName(String)}
     */
    void setName(String threadName);

    /**
     * See {@link Thread#join()}.
     * @throws InterruptedException see {@link Thread#join()}
     */
    void join() throws InterruptedException;

    /**
     * Starts sending mail found on the queue.
     *
     * @param mailQueue the queue where pending emails to send are located
     */
    void startProcessing(Queue<MailSenderQueueItem> mailQueue);

    /**
     * Stops sending mails from the queue.
     */
    void stopProcessing();

    /**
     * Starts sending mail found on the queue.
     *
     * @param mailQueue the queue where pending emails to send are located
     */
    void run(Queue<MailSenderQueueItem> mailQueue);
}
