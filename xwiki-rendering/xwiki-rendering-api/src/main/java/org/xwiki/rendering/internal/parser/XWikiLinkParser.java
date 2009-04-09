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
package org.xwiki.rendering.internal.parser;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Parses the content of XWiki links. Allowed URIs are URLs of the form {@code http://}, {@code mailto:}, 
 * {@code image:} and {@code attach:}.
 * 
 * @version $Id$
 * @since 1.5M2
 * @see AbstractLinkParser
 */
@Component("xwiki/2.0")
public class XWikiLinkParser extends AbstractLinkParser
{
    /**
     * The list of recognized URL prefixes.
     */
    private static final List<String> URI_PREFIXES = Arrays.asList("mailto", "image", "attach");

    /**
     * {@inheritDoc}
     * @see AbstractLinkParser#getAllowedURIPrefixes()
     */
    @Override
    protected List<String> getAllowedURIPrefixes()
    {
        return URI_PREFIXES;
    }
}
