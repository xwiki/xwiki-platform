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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.wiki.descriptor.WikiDescriptor;

/**
 * Handles path-based multiwiki configurations when extracting the wiki reference from the passed URL.
 *
 * @version $Id$
 * @since 6.3M1
 */
@Component
@Named("path")
@Singleton
public class PathWikiReferenceExtractor extends AbstractWikiReferenceExtractor
{
    @Override
    public WikiReference extract(ExtendedURL url)
    {
        // The first segment is the name of the wiki.
        String wikiId = resolvePathBasedWikiReference(url.getSegments().get(0));

        if (StringUtils.isEmpty(wikiId)) {
            wikiId = getMainWikiId();
        }

        return new WikiReference(wikiId.toLowerCase());
    }

    private String resolvePathBasedWikiReference(String alias)
    {
        String wikiId;

        // Look for a Wiki Descriptor
        WikiDescriptor wikiDescriptor = getWikiDescriptorByAlias(alias);
        if (wikiDescriptor != null) {
            // Get the wiki id from the wiki descriptor
            wikiId = wikiDescriptor.getId();
        } else {
            wikiId = normalizeWikiIdForNonExistentWikiDescriptor(alias);
        }

        return wikiId;
    }
}
