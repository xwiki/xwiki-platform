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
 * when the Resource Type is {@code wiki}, i.e. when XWiki is configured for path-based multiwiki
 * (e.g. {@code http://server/(ignorePrefix)/wiki/wikiname/action/space/page/attachment}).
 *
 * @version $Id$
 * @since 6.3M1
 */
public class WikiEntityResourceReferenceResolver extends AbstractEntityResourceReferenceResolver
{
    /**
     * Used to extract the wiki reference from the URL.
     */
    private WikiReferenceExtractor wikiExtractor;

    @Override
    protected WikiReference extractWikiReference(ExtendedURL extendedURL)
    {
        WikiReference wikiReference = this.wikiExtractor.extract(extendedURL);

        // Remove the first path segment since it contains the wiki name and we need the first segment to be the
        // action name
        extendedURL.getSegments().remove(0);

        return wikiReference;
    }
}
