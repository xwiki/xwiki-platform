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

import java.net.URL;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.Resource;
import org.xwiki.resource.ResourceSerializer;

/**
 * Transforms a XWiki Resource instance into a URL object. Note that the serialization performs URL-encoding
 * wherever necessary to generate a valid URL (see http://www.ietf.org/rfc/rfc2396.txt).
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component("standard")
public class URLResourceSerializer implements ResourceSerializer<URL>
{
    /**
     * Transforms a XWiki Resource instance into a URL object. Note that the serialization performs URL-encoding
     * wherever necessary to generate a valid URL (see http://www.ietf.org/rfc/rfc2396.txt).
     * 
     * @param resource the XWiki URL to transform
     * @return the standard URL instance 
     */
    public URL serialize(Resource resource)
    {
        throw new RuntimeException("Not implemented yet");
    }
}
