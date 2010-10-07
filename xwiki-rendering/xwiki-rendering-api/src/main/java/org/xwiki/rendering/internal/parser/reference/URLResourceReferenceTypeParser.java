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
package org.xwiki.rendering.internal.parser.reference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ResourceType;

/**
 * Parses a resource reference to a URL.
 *
 * @version $Id$
 * @since 2.5RC1
 */
@Component("url")
public class URLResourceReferenceTypeParser extends AbstractURIResourceReferenceTypeParser
{
    /**
     * URL matching pattern.
     */
    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("[a-zA-Z0-9+.-]*://");

    /**
     * {@inheritDoc}
     *
     * @see AbstractURIResourceReferenceTypeParser#getType()
     */
    public ResourceType getType()
    {
        return ResourceType.URL;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractURIResourceReferenceTypeParser#parse(String)
     */
    @Override
    public ResourceReference parse(String reference)
    {
        ResourceReference resultReference = null;
        Matcher matcher = URL_SCHEME_PATTERN.matcher(reference);
        if (matcher.lookingAt()) {
            // We don't parse the URL since it can contain unknown protocol for the JVM but protocols known by the
            // browser (such as skype:// for example).
            resultReference = new ResourceReference(reference, getType());
        }
        return resultReference;
    }
}
