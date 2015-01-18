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
package org.xwiki.mail.internal.thread;

import javax.mail.Session;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.mail.MailListener;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a Mail messages placed on the queue for processing. Extending classes define how to reference the mail
 * message itself.
 *
 * @version $Id$
 * @since 6.4
 */
public abstract class AbstractMailQueueItem implements MailQueueItem
{
    private Session session;

    private MailListener listener;

    private String batchId;

    private String wikiId;

    /**
     * @param session see {@link #getSession()}
     * @param listener see {@link #getListener()}
     * @param batchId see {@link #getBatchId()}
     * @param wikiId see {@link #getWikiId()}
     */
    public AbstractMailQueueItem(Session session, MailListener listener, String batchId, String wikiId)
    {
        this.session = session;
        this.listener = listener;
        this.batchId = batchId;
        this.wikiId = wikiId;
    }

    @Override
    public Session getSession()
    {
        return this.session;
    }

    @Override
    public MailListener getListener()
    {
        return this.listener;
    }

    @Override
    public String toString()
    {
        return prepareToString().toString();
    }

    protected ToStringBuilder prepareToString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("batchId", getBatchId());
        builder.append("wikiId", getWikiId());
        return builder;
    }

    @Override
    public String getBatchId()
    {
        return this.batchId;
    }

    @Override
    public String getWikiId()
    {
        return this.wikiId;
    }
}
