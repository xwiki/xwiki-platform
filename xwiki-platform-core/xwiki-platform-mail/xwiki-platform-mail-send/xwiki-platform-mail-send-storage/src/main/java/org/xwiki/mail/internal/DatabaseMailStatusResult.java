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

import java.util.Iterator;
import java.util.UUID;

import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;

public class DatabaseMailStatusResult implements MailStatusResult
{
    private MailStatusStore mailStatusStore;

    private UUID batchId;

    public DatabaseMailStatusResult(UUID batchId,  MailStatusStore mailStatusStore)
    {
        this.batchId = batchId;
        this.mailStatusStore = mailStatusStore;
    }

    @Override
    public long getSize()
    {
        return this.mailStatusStore.count(this.batchId);
    }

    @Override
    public Iterator<MailStatus> getAll()
    {
        return this.mailStatusStore.lo
    }

    @Override
    public Iterator<MailStatus> getByState(MailState state)
    {
        return null;
    }
}
