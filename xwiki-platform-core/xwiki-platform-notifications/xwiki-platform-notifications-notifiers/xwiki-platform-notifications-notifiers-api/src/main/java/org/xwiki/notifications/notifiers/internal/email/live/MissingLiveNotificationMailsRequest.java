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
package org.xwiki.notifications.notifiers.internal.email.live;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;

/**
 * The request used to configure {@link MissingLiveNotificationMailsJob}.
 * 
 * @version $Id$
 * @since 12.6
 */
public class MissingLiveNotificationMailsRequest extends AbstractRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_WIKI = "wiki";

    /**
     * The default constructor.
     */
    public MissingLiveNotificationMailsRequest()
    {
    }

    /**
     * @param request the request to copy
     */
    public MissingLiveNotificationMailsRequest(Request request)
    {
        super(request);
    }

    /**
     * @param wiki the id of the wiki in which to search for user having enabled live notification mails
     */
    public MissingLiveNotificationMailsRequest(String wiki)
    {
        setWiki(wiki);

        // This job's log is not isolated so we only want important log
        setVerbose(false);
    }

    /**
     * @param wikiId the wikiId to set
     */
    public void setWiki(String wikiId)
    {
        setProperty(PROPERTY_WIKI, wikiId);
    }

    /**
     * @return the wikiId
     */
    public String getWiki()
    {
        return getProperty(PROPERTY_WIKI);
    }
}
