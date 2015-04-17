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

import javax.inject.Inject;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Commons code for Wiki Reference Extractors.
 *
 * @version $Id$
 * @since 6.3M1
 */
public abstract class AbstractWikiReferenceExtractor implements WikiReferenceExtractor
{
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
     * Check if there's a descriptor for the passed wiki and if not and the configuration option to redirect to
     * the main wiki is enabled then return the main wiki.
     */
    protected String normalizeWikiIdForNonExistentWikiDescriptor(String wikiId)
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

    protected WikiDescriptor getWikiDescriptorByAlias(String alias)
    {
        try {
            return this.wikiDescriptorManager.getByAlias(alias);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to located wiki descriptor for alias [%s]", alias), e);
        }
    }

    protected WikiDescriptor getWikiDescriptorById(String wikiId)
    {
        try {
            return this.wikiDescriptorManager.getById(wikiId);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to located wiki descriptor for wiki [%s]", wikiId), e);
        }
    }

    protected String getMainWikiId()
    {
        return this.entityReferenceValueProvider.getDefaultValue(EntityType.WIKI);
    }
}
