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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.mail.internet.MimeMessage;

import org.xwiki.mail.MailResultListener;

/**
 * Saves errors when sending messages, in a local variable.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class DefaultMailResultListener implements MailResultListener
{
    private static final String CONTEXT_KEY = "mailsenderExceptions";

    private BlockingQueue<Exception> errorQueue = new LinkedBlockingQueue<>(100);

    @Override
    public void onSuccess(MimeMessage message)
    {
        // We're only interested in errors in the scripting API.
    }

    @Override
    public void onError(MimeMessage message, Exception e)
    {
        // Add the error to the queue
        this.errorQueue.add(e);
    }

    /**
     * @return the list of exceptions raised when sending mails in the current thread
     */
    public BlockingQueue<Exception> getExceptionQueue()
    {
        return this.errorQueue;
    }
}
