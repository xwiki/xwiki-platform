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
package org.xwiki.wiki.internal.descriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilderException;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link WikiDescriptorManager}.
 * 
 * @version $Id$
 * @since 6.0M1
 */
@Component
@Singleton
public class DefaultWikiDescriptorManager implements WikiDescriptorManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorCache cache;

    @Inject
    private WikiDescriptorDocumentHelper descriptorDocumentHelper;

    @Inject
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @Override
    public Collection<WikiDescriptor> getAll() throws WikiManagerException
    {
        // Note: Ideally to improve performance we could imagine loading all XWikiServerClasses at initialization time
        // (in initialize()) and thereafter only use the cache. The problem with this approach is that our Cache will
        // need to be unbounded which is not the case right now. This would mean being able to put all descriptors in
        // the cache and thus it might not scale if there were a very large number of wikis.

        List<WikiDescriptor> result = new ArrayList<WikiDescriptor>();
        try {
            List<String> documentNames = descriptorDocumentHelper.getAllXWikiServerClassDocumentNames();

            boolean foundMainWiki = false;

            XWikiContext xcontext = xcontextProvider.get();

            for (String documentName : documentNames) {
                // Get the id
                String wikiId = descriptorDocumentHelper.getWikiIdFromDocumentFullname(documentName);

                // Get the descriptor
                WikiDescriptor descriptor = getById(wikiId);

                // Add it to the result list
                if (descriptor != null) {
                    result.add(descriptor);

                    foundMainWiki |= xcontext.isMainWiki(wikiId);
                }
            }

            // Make sure we always return a descriptor for main wiki, even a virtual one
            if (!foundMainWiki) {
                result.add(getMainWikiDescriptor());
            }
        } catch (Exception e) {
            throw new WikiManagerException("Failed to locate XWiki.XWikiServerClass documents", e);
        }

        return result;
    }

    @Override
    public WikiDescriptor getByAlias(String wikiAlias) throws WikiManagerException
    {
        WikiDescriptor descriptor = cache.getFromAlias(wikiAlias);

        // If not found in the cache then query the wiki and add to the cache if found.
        //
        // Note that an alternative implementation would have been to find all Wiki Descriptors at startup but this
        // would have meant keeping them all in memory at once. Since we want to be able to scale to any number of
        // subwikis we only cache the most used one. This allows inactive wikis to not take up any memory for example.
        // Note that In order for performance to be maximum it also means we need to have a cache size at least as
        // large as the max # of wikis being used at once.
        if (descriptor == null) {
            XWikiDocument document = descriptorDocumentHelper.findXWikiServerClassDocument(wikiAlias);
            if (document != null) {
                // Build the descriptor
                descriptor = buildDescriptorFromDocument(document);
            }
        }

        return descriptor;
    }

    @Override
    public WikiDescriptor getById(String wikiId) throws WikiManagerException
    {
        WikiDescriptor descriptor = cache.getFromId(wikiId);

        if (descriptor == null) {
            // Try to load a page named XWiki.XWikiServer<wikiId>
            XWikiDocument document = descriptorDocumentHelper.getDocumentFromWikiId(wikiId);

            if (!document.isNew()) {
                // Build the descriptor
                descriptor = buildDescriptorFromDocument(document);
            } else if (getMainWikiId().equals(wikiId)) {
                // Return a "virtual" descriptor if main wiki does not yet have a descriptor document
                descriptor = new WikiDescriptor(wikiId, "localhost");
            }
        }

        return descriptor;
    }

    @Override
    public boolean exists(String wikiId) throws WikiManagerException
    {
        return getById(wikiId) != null;
    }

    @Override
    public void saveDescriptor(WikiDescriptor descriptor) throws WikiManagerException
    {
        try {
            wikiDescriptorBuilder.save(descriptor);
        } catch (WikiDescriptorBuilderException e) {
            throw new WikiManagerException(
                String.format("Unable to save wiki descriptor for [%s].", descriptor.getId()), e);
        }
    }

    @Override
    public WikiDescriptor getMainWikiDescriptor() throws WikiManagerException
    {
        return getById(getMainWikiId());
    }

    @Override
    public String getMainWikiId()
    {
        return xcontextProvider.get().getMainXWiki();
    }

    @Override
    public String getCurrentWikiId()
    {
        return xcontextProvider.get().getWikiId();
    }

    private DefaultWikiDescriptor buildDescriptorFromDocument(XWikiDocument document)
    {
        DefaultWikiDescriptor descriptor = null;
        List<BaseObject> serverClassObjects = document.getXObjects(DefaultWikiDescriptor.SERVER_CLASS);
        if (serverClassObjects != null && !serverClassObjects.isEmpty()) {
            descriptor = wikiDescriptorBuilder.buildDescriptorObject(serverClassObjects, document);
            // Add to the cache
            if (descriptor != null) {
                cache.add(descriptor);
            }
        }
        return descriptor;
    }

}
