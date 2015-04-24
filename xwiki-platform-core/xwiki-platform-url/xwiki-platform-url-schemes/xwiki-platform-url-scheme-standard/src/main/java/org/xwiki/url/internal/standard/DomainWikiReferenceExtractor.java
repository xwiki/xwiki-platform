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
 * Handles domain-based multiwiki configurations when extracting the wiki reference from the passed URL.
 *
 * @version $Id$
 * @since 6.3M1
 */
@Component
@Named("domain")
@Singleton
public class DomainWikiReferenceExtractor extends AbstractWikiReferenceExtractor
{
    @Override
    public WikiReference extract(ExtendedURL url)
    {
        // Note: we don't use url.getURI().getHost() since URI are more restricted in characters allowed in domain
        // names. For example the "_" character is not supported URIs but it is supported in URLs.
        String wikiId = resolveDomainBasedWikiReference(url.getWrappedURL().getHost());

        if (StringUtils.isEmpty(wikiId)) {
            wikiId = getMainWikiId();
        }

        return new WikiReference(wikiId.toLowerCase());
    }

    private String resolveDomainBasedWikiReference(String alias)
    {
        String wikiId;

        // Look for a Wiki Descriptor
        WikiDescriptor wikiDescriptor = getWikiDescriptorByAlias(alias);
        if (wikiDescriptor != null) {
            // Get the wiki id from the wiki descriptor
            wikiId = wikiDescriptor.getId();
        } else {
            // Fallback: No definition found based on the full domain name, consider the alias as a
            // domain name and try to use the first part of the domain name as the wiki name.
            String domainAlias = StringUtils.substringBefore(alias, ".");

            // As a convenience, we do not require the creation of an XWiki.XWikiServerXwiki page for the main
            // wiki and automatically go to the main wiki in certain cases:
            // - "www.<rest of domain name>"
            // - "localhost"
            // - IP address
            if ("www".equals(domainAlias) || "localhost".equals(alias)
                || alias.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}"))
            {
                wikiId = getMainWikiId();
            } else {
                wikiId = normalizeWikiIdForNonExistentWikiDescriptor(domainAlias);
            }

            // Create a virtual descriptor and save it so that next call will resolve to it directly without needing
            // to query the entity store.
            // this.wikiDescriptorCache.add(new WikiDescriptor(wikiId, alias));
            // TODO: uncomment theses lines, find a solution
        }

        return wikiId;
    }
}
