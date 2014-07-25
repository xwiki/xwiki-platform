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
package org.xwiki.url.internal.standard;

import org.xwiki.stability.Unstable;
import org.xwiki.resource.ResourceType;
import org.xwiki.url.internal.ExtendedURL;

/**
 * Represents a URL parsing state. This class is used as a holder that gets modified by the various steps during the
 * URL parsing.
 *
 * @version $Id$
 * @since 5.1M1
 */
@Unstable
public class URLParsingState
{
    /**
     * @see #getURL()
     */
    private ExtendedURL url;

    /**
     * @see 1#getURLType()
     */
    private ResourceType urlType;

    /**
     * @return the URL that we're parsing (its state is changed during the parsing process)
     */
    public ExtendedURL getURL()
    {
        return this.url;
    }

    /**
     * @param url see {@link #getURL()}
     */
    public void setURL(ExtendedURL url)
    {
        this.url = url;
    }

    /**
     * @return the type of URL being parsed (Entity URL, Resource URL, etc) or null if the type has not been recognized
     *         yet
     */
    public ResourceType getURLType()
    {
        return this.urlType;
    }

    /**
     * @param resourceType see {@link #getURLType()}
     */
    public void setURLType(ResourceType resourceType)
    {
        this.urlType = resourceType;
    }
}
