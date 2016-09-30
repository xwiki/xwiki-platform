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
package org.xwiki.url.internal.standard.entity;

import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.WikiReferenceExtractor;

/**
 * Resolver that generates {@link org.xwiki.resource.entity.EntityResourceReference} out of {@link ExtendedURL} URLs
 * when the Resource Type is Entity (usually {@code bin} but it's controlled through XWiki configuration, see
 * {@link org.xwiki.url.internal.standard.DefaultStandardURLConfiguration#getEntityPathPrefix()}). For example:
 * {@code http://server/(ignorePrefix)/bin/action/space/page/attachment}.
 * <p>
 * Use cases:
 * <ul>
 *   <li>URLs for the main wiki in path-based multiwiki configs</li>
 *   <li>URLs for domain-based multiwiki configs</li>
 * </ul>
 *
 * @version $Id$
 * @since 6.1M2
 */
public class BinEntityResourceReferenceResolver extends AbstractEntityResourceReferenceResolver
{
    /**
     * Used to extract the wiki reference from the URL.
     */
    private WikiReferenceExtractor wikiExtractor;

    @Override
    protected WikiReference extractWikiReference(ExtendedURL extendedURL)
    {
        return this.wikiExtractor.extract(extendedURL);
    }
}
