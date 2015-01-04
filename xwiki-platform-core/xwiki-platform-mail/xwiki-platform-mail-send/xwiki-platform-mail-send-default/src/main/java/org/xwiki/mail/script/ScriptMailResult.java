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

import java.util.UUID;

import org.xwiki.mail.MailResult;
import org.xwiki.mail.MailStatusResult;

public class ScriptMailResult implements MailResult
{
    private MailResult wrappedMailResult;

    private MailStatusResult mailStatusResults;

    public ScriptMailResult(MailResult wrappedMailResult, MailStatusResult mailStatusResults)
    {
        this.wrappedMailResult = wrappedMailResult;
        this.mailStatusResults = mailStatusResults;
    }

    public MailStatusResult getStatusResults()
    {
        return this.mailStatusResults;
    }

    @Override
    public void waitTillSent(long timeout)
    {
        this.wrappedMailResult.waitTillSent(timeout);
    }

    @Override
    public boolean isSent()
    {
        return this.wrappedMailResult.isSent();
    }

    @Override
    public UUID getBatchId()
    {
        return this.wrappedMailResult.getBatchId();
    }
}
