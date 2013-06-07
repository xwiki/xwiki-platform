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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.internal.ExtendedURL;

/**
 * Handles both path-based and domain-based multiwiki configurations when extracting the wiki reference from the passed
 * URL.
 *
 * @version $Id$
 * @since 5.1M1
 */
@Component
@Singleton
public class DefaultWikiReferenceExtractor implements WikiReferenceExtractor
{
    /**
     * Host resolver to generate {@link WikiReference} for path-based configurations.
     */
    @Inject
    @Named("path")
    private WikiReferenceResolver pathBasedWikiReferenceResolver;

    /**
     * Host resolver to generate {@link WikiReference} for domain-based configurations.
     */
    @Inject
    @Named("domain")
    private WikiReferenceResolver domainWikiReferenceResolver;

    /**
     * To find out how what type of multiwiki is configured (path-based or domain-based).
     */
    @Inject
    private StandardURLConfiguration configuration;

    /**
     * {@inheritDoc}
     * <p/>
     * For domain-based multiwiki setups we ask a resolver to resolve the URL's host name.
     * For path-based multiwiki setup we get the path segment after the first segment, if this first segment has the
     * predefined {@link StandardURLConfiguration#getWikiPathPrefix()} value. If not then we
     * fall-back to domain-based multiwiki setups and resolve with the URL's host name.
     *
     * @return the wiki the URL is pointing to, returned as a {@link WikiReference}.
     */
    @Override
    public Pair<WikiReference, Boolean> extract(ExtendedURL url)
    {
        boolean isActuallyPathBased = false;

        WikiReference wikiReference = null;
        if (this.configuration.isPathBasedMultiWiki()) {
            List<String> segments = url.getSegments();
            // If the first path element isn't the value of the wikiPathPrefix configuration value then we fall back
            // to the host name. This also allows the main wiki URL to be domain-based even for a path-based multiwiki.
            if (segments.get(0).equalsIgnoreCase(this.configuration.getWikiPathPrefix())) {
                wikiReference = this.pathBasedWikiReferenceResolver.resolve(segments.get(1));
                isActuallyPathBased = true;
            }
        }
        if (wikiReference == null) {
            wikiReference = this.domainWikiReferenceResolver.resolve(url.getURI().getHost());
        }

        return new ImmutablePair(wikiReference, isActuallyPathBased);
    }
}
