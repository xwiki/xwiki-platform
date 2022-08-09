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
package org.xwiki.eventstream.store.internal;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;

/**
 * Request for the creation of a {@link EventStreamWikiCleanerJob}.
 *
 * @since 11.3RC1
 * @since 10.11.4
 * @since 10.8.4
 * @version $Id$
 */
public class EventStreamWikiCleanerJobRequest extends AbstractRequest
{
    private static final String WIKI_ID = "wikiId";

    /**
     * Default constructor.
     */
    public EventStreamWikiCleanerJobRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public EventStreamWikiCleanerJobRequest(Request request)
    {
        super(request);
    }

    /**
     * Create a request for the given wiki.
     * @param wikiId the id of the wiki for which the event stream need to clean events
     */
    public EventStreamWikiCleanerJobRequest(String wikiId)
    {
        setWikiId(wikiId);
    }

    /**
     * @param wikiId the id of the wiki for which the event stream need to clean events
     */
    public void setWikiId(String wikiId)
    {
        setProperty(WIKI_ID, wikiId);
    }

    /**
     * @return the id of the wiki for which the event stream need to clean events
     */
    public String getWikiId()
    {
        return getProperty(WIKI_ID);
    }
}
