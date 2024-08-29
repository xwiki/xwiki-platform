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

import org.xwiki.context.Execution;
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
    private static final String XWIKI_CONTEXT = "xwikicontext";

    @Inject
    private StandardURLConfiguration configuration;

    /**
     * Used to get wiki descriptors based on alias or wiki id.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Execution execution;

    /**
     * Check if there's a descriptor for the passed wiki and if not and the configuration option to redirect to
     * the main wiki is enabled then return the main wiki.
     */
    protected String normalizeWikiIdForNonExistentWikiDescriptor(String alias)
    {
        String normalizedWikiId = alias;
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
        // Note: We also support not having an Execution Context available. This allows this code to work at request
        // initialization time, when no Context has been set up yet. In the future, we need to move the Context init
        // as the first thing along with Database initialization.
        if (this.execution.getContext() == null || this.execution.getContext().getProperty(XWIKI_CONTEXT) == null) {
            return null;
        }

        try {
            return this.wikiDescriptorManager.getByAlias(alias);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to locate wiki descriptor for alias [%s]", alias), e);
        }
    }

    protected WikiDescriptor getWikiDescriptorById(String wikiId)
    {
        // Note: We also support not having an Execution Context available. This allows this code to work at request
        // initialization time, when no Context has been set up yet. In the future, we need to move the Context init
        // as the first thing along with Database initialization.
        if (this.execution.getContext() == null || this.execution.getContext().getProperty(XWIKI_CONTEXT) == null) {
            return null;
        }

        try {
            return this.wikiDescriptorManager.getById(wikiId);
        } catch (WikiManagerException e) {
            throw new RuntimeException(String.format("Failed to locate wiki descriptor for wiki [%s]", wikiId), e);
        }
    }

    protected String getMainWikiId()
    {
        return this.wikiDescriptorManager.getMainWikiId();
    }
}
