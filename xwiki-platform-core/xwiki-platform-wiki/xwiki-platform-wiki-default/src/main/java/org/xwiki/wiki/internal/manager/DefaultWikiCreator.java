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
package org.xwiki.wiki.internal.manager;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.wiki.configuration.WikiConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilderException;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link WikiCreator}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWikiCreator implements WikiCreator
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiConfiguration wikiConfiguration;

    @Override
    public WikiDescriptor create(String wikiId, String wikiAlias) throws WikiManagerException
    {
        return create(wikiId, wikiAlias, null);
    }

    @Override
    public WikiDescriptor create(String wikiId, String wikiAlias, String ownerId) throws WikiManagerException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        // Create database/schema/user (unless configured otherwise)
        if (this.wikiConfiguration.shouldCreateDatabase()) {
            try {
                xwiki.getStore().createWiki(wikiId, context);
            } catch (Exception e) {
                throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databasecreation", wikiId),
                    e);
            }
        }

        // Create the descriptor
        // Since XWiki#updateDatabase is generating documents it needs the wiki to actually exist
        // from XWiki point of view as otherwise various codes are going to be lost
        WikiDescriptor descriptor = createDescriptor(wikiId, wikiAlias, ownerId);

        // Init database/schema
        try {
            xwiki.initializeWiki(wikiId, true, context);
        } catch (Exception e) {
            throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databaseupdate", wikiId), e);
        }

        return descriptor;
    }

    private WikiDescriptor createDescriptor(String wikiId, String wikiAlias, String ownerId) throws WikiManagerException
    {
        // Create the descriptor
        WikiDescriptor descriptor = new DefaultWikiDescriptor(wikiId, wikiAlias, ownerId);

        try {
            // Build the document
            wikiDescriptorBuilder.save(descriptor);
            // Reload the descriptor from the cache because it should have been seen by the DescriptorListener.
            descriptor = wikiDescriptorManager.getById(wikiId);
        } catch (WikiDescriptorBuilderException e) {
            throw new WikiManagerException("Failed to build the descriptor document.", e);
        }

        return descriptor;
    }
}
