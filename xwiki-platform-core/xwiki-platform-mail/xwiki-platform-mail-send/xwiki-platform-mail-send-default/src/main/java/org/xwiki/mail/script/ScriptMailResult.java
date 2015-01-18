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
package org.xwiki.mail.script;

import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailStatusResult;

/**
 * Implementation used by the Mail Sender Script Service. It wraps the Java {@link org.xwiki.mail.MailResult} class to
 * add a new {@link #getStatusResult()} method since in the Scripting API the {@link org.xwiki.mail.MailListener} is
 * passed as a hint and thus the user cannot call {@link org.xwiki.mail.MailListener#getMailStatusResult()} on it.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class ScriptMailResult implements MailResult
{
    private MailResult wrappedMailResult;

    private MailStatusResult mailStatusResult;

    /**
     * @param wrappedMailResult the {@link org.xwiki.mail.MailResult} instance to wrap
     * @param mailStatusResult see {@link #getStatusResult()}
     */
    public ScriptMailResult(MailResult wrappedMailResult, MailStatusResult mailStatusResult)
    {
        this.wrappedMailResult = wrappedMailResult;
        this.mailStatusResult = mailStatusResult;
    }

    /**
     * @return the {@link org.xwiki.mail.MailListener}'s {@link org.xwiki.mail.MailStatusResult} object which is useful
     *         for script users to get the status of each mail from the batch
     */
    public MailStatusResult getStatusResult()
    {
        return this.mailStatusResult;
    }

    @Override
    public void waitTillProcessed(long timeout)
    {
        this.wrappedMailResult.waitTillProcessed(timeout);
    }

    @Override
    public boolean isProcessed()
    {
        return this.wrappedMailResult.isProcessed();
    }

    @Override
    public String getBatchId()
    {
        return this.wrappedMailResult.getBatchId();
    }
}
