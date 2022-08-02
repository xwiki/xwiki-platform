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
package org.xwiki.url.internal;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Ensures Serialized URL are relative (i.e. without leading "/").
 *
 * @version $Id$
 * @since 7.2M1
 */
public class RelativeExtendedURL extends ExtendedURL
{
    /**
     * Populate the Extended URL with a list of path segments.
     *
     * @param segments the path segments of the URL
     */
    public RelativeExtendedURL(List<String> segments)
    {
        super(segments);
    }

    /**
     * Populate the Extended URL with a list of path segments.
     *
     * @param segments the path segments of the URL
     * @param parameters the query string parameters of the URL
     */
    public RelativeExtendedURL(List<String> segments, Map<String, List<String>> parameters)
    {
        super(segments, parameters);
    }

    /**
     * @param url the URL being wrapped
     * @param ignorePrefix the ignore prefix must start with "/" (eg "/xwiki"). It can be empty or null too in which
     *        case it's not used
     * @throws CreateResourceReferenceException if the passed URL is invalid which can happen if it has incorrect
     *         encoding
     */
    public RelativeExtendedURL(URL url, String ignorePrefix) throws CreateResourceReferenceException
    {
        super(url, ignorePrefix);
    }

    @Override
    public String serialize()
    {
        // Make sure the serialized URL is relative
        return StringUtils.stripStart(super.serialize(), "/");
    }
}
