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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

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
     * To find out how what type of multiwiki is configured (path-based or domain-based).
     */
    @Inject
    private StandardURLConfiguration configuration;

    /**
     * Used to get the main wiki name.
     * @todo replace that with a proper API to get the main wiki reference
     */
    @Inject
    private EntityReferenceValueProvider entityReferenceValueProvider;

    /**
     * Used to get wiki descriptors based on alias or wiki id.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

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

        String wikiId = null;
        if (this.configuration.isPathBasedMultiWiki()) {
            List<String> segments = url.getSegments();
            // If the first path element isn't the value of the wikiPathPrefix configuration value then we fall back
            // to the host name. This also allows the main wiki URL to be domain-based even for a path-based multiwiki.
            if (segments.get(0).equalsIgnoreCase(this.configuration.getWikiPathPrefix())) {
                wikiId = resolvePathBasedWikiReference(segments.get(1));
                isActuallyPathBased = true;
            }
        }
        if (wikiId == null) {
            wikiId = resolveDomainBasedWikiReference(url.getURI().getHost());
        }

        if (StringUtils.isEmpty(wikiId)) {
            wikiId = getMainWikiId();
        }

        return new ImmutablePair(new WikiReference(wikiId.toLowerCase()), isActuallyPathBased);
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

    /**
     * Check if there's a descriptor for the passed wiki and if not and the configuration option to redirect to
     * the main wiki is enabled then return the main wiki.
     */
    private String normalizeWikiIdForNonExistentWikiDescriptor(String wikiId)
    {
        String normalizedWikiId = wikiId;
        String mainWiki = getMainWikiId();
        if (!mainWiki.equals(normalizedWikiId)
            && this.configuration.getWikiNotFoundBehavior() == WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI)
        {
            if (getWikiDescriptorById(normalizedWikiId) == null) {
                // Fallback on main wiki
                normalizedWikiId = mainWiki;
            }
        }
        return normalizedWikiId;
    }

    private WikiDescriptor getWikiDescriptorByAlias(String alias)
    {
        try {
            return this.wikiDescriptorManager.getByAlias(alias);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to located wiki descriptor for alias [%s]", alias), e);
        }
    }

    private WikiDescriptor getWikiDescriptorById(String wikiId)
    {
        try {
            return this.wikiDescriptorManager.getById(wikiId);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to located wiki descriptor for wiki [%s]", wikiId), e);
        }
    }

    private String getMainWikiId()
    {
        return this.entityReferenceValueProvider.getDefaultValue(EntityType.WIKI);
    }
}
