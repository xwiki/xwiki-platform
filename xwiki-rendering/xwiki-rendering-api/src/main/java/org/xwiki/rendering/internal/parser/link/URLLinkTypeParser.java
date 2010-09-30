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
package org.xwiki.rendering.internal.parser.link;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;

/**
 * Parses a link reference to a URL.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("url")
public class URLLinkTypeParser extends AbstractURILinkTypeParser
{
    /**
     * URL matching pattern.
     */
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("[a-zA-Z0-9+.-]*://");

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.parser.link.AbstractURILinkTypeParser#getType()
     */
    public LinkType getType()
    {
        return LinkType.URL;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractURILinkTypeParser#parse(String)
     */
    @Override
    public Link parse(String reference)
    {
        Link resultLink = null;
        Matcher matcher = URL_SCHEME_PATTERN.matcher(reference);
        if (matcher.lookingAt()) {
            // We don't parse the URL since it can contain unknown protocol for the JVM but protocols known by the
            // browser (such as skype:// for example).
            resultLink = new Link();
            resultLink.setType(getType());
            resultLink.setReference(reference);
        }
        return resultLink;
    }
}
