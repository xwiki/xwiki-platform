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

import org.xwiki.mail.MailResult;
import org.xwiki.mail.internal.thread.MailQueueManager;
import org.xwiki.mail.internal.thread.SendMailQueueItem;

/**
 * Default implementation used when using the Mail Sender Java API.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DefaultMailResult implements MailResult
{
    private String batchId;

    private MailQueueManager<SendMailQueueItem> sendMailQueueManager;

    /**
     * @param batchId the unique id for the batch of emails being sent together, used to verify when they've all been
     *        sent
     * @param sendMailQueueManager the class we used to check when the emails have been sent
     */
    public DefaultMailResult(String batchId, MailQueueManager<SendMailQueueItem> sendMailQueueManager)
    {
        this.batchId = batchId;
        this.sendMailQueueManager = sendMailQueueManager;
    }

    @Override
    public void waitTillProcessed(long timeout)
    {
        this.sendMailQueueManager.waitTillProcessed(getBatchId(), timeout);
    }

    @Override
    public boolean isProcessed()
    {
        return this.sendMailQueueManager.isProcessed(getBatchId());
    }

    @Override
    public String getBatchId()
    {
        return this.batchId;
    }
}
