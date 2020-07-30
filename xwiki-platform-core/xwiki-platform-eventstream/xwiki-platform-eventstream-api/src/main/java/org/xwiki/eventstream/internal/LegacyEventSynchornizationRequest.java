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
package org.xwiki.eventstream.internal;

import java.util.Date;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;

/**
 * The request used to configure {@link LegacyEventSynchornizationJob}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
public class LegacyEventSynchornizationRequest extends AbstractRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private static final String AFTER_ID = "after";

    /**
     * The default constructor.
     */
    public LegacyEventSynchornizationRequest()
    {
    }

    /**
     * @param request the request to copy
     */
    public LegacyEventSynchornizationRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the date after which to copy the events
     */
    public Date getAfter()
    {
        return this.getProperty(AFTER_ID);
    }

    /**
     * @param after the date after which to copy the events
     */
    public void setAfter(Date after)
    {
        setProperty(AFTER_ID, after);
    }
}
