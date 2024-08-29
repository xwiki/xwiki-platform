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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
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
    @Named("readonly")
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorCache cache;

    @Inject
    private Provider<WikiDescriptorDocumentHelper> descriptorDocumentHelperProvider;

    @Inject
    private Provider<WikiDescriptorBuilder> wikiDescriptorBuilderProvider;

    @Override
    public Collection<WikiDescriptor> getAll() throws WikiManagerException
    {
        // Note: Ideally to improve performance we could imagine loading all XWikiServerClasses at initialization time
        // (in initialize()) and thereafter only use the cache. The problem with this approach is that our Cache will
        // need to be unbounded which is not the case right now. This would mean being able to put all descriptors in
        // the cache, and thus it might not scale if there were a very large number of wikis.
        // Note that the full list of ids is cached since it takes a lot less memory that descriptors.

        Collection<String> wikiIds = getAllIds();

        List<WikiDescriptor> result = new ArrayList<>(wikiIds.size());

        for (String wikiId : wikiIds) {
            // Get the descriptor
            WikiDescriptor descriptor = getById(wikiId);

            // Add it to the result list
            if (descriptor != null) {
                result.add(descriptor);
            }
        }

        // Sort by wiki pretty name (with a fallback on wiki id).
        Collections.sort(result, (alice, bob) -> getDisplayName(alice).compareToIgnoreCase(getDisplayName(bob)));

        return result;
    }

    private String getDisplayName(WikiDescriptor descriptor)
    {
        String displayName = descriptor.getPrettyName();
        if (StringUtils.isEmpty(displayName)) {
            displayName = descriptor.getId();
        }
        return displayName;
    }

    @Override
    public Collection<String> getAllIds() throws WikiManagerException
    {
        Collection<String> wikiIds = this.cache.getWikiIds();

        if (wikiIds == null) {
            List<String> documentNames;
            try {
                documentNames = this.descriptorDocumentHelperProvider.get().getAllXWikiServerClassDocumentNames();
            } catch (Exception e) {
                throw new WikiManagerException("Failed to get wiki ids", e);
            }

            wikiIds = new HashSet<>(documentNames.size());

            boolean foundMainWiki = false;

            XWikiContext xcontext = this.xcontextProvider.get();

            for (String documentName : documentNames) {
                String wikId = this.descriptorDocumentHelperProvider.get().getWikiIdFromDocumentFullname(documentName);

                wikiIds.add(wikId);

                foundMainWiki |= xcontext.isMainWiki(wikId);
            }

            // Make sure we always return a descriptor for main wiki, even a virtual one
            if (!foundMainWiki) {
                wikiIds.add(getMainWikiId());
            }

            this.cache.setWikiIds(Collections.unmodifiableCollection(wikiIds));
        }

        return wikiIds;
    }

    @Override
    public WikiDescriptor getByAlias(String wikiAlias) throws WikiManagerException
    {
        WikiDescriptor descriptor = this.cache.getFromAlias(wikiAlias);

        // If not found in the cache then query the wiki and add to the cache if found.
        //
        // Note that an alternative implementation would have been to find all Wiki Descriptors at startup but this
        // would have meant keeping them all in memory at once. Since we want to be able to scale to any number of
        // subwikis we only cache the most used one. This allows inactive wikis to not take up any memory for example.
        // Note that In order for performance to be maximum it also means we need to have a cache size at least as
        // large as the max # of wikis being used at once.
        if (descriptor == null) {
            XWikiDocument document =
                this.descriptorDocumentHelperProvider.get().findXWikiServerClassDocument(wikiAlias);
            if (document != null) {
                // Build the descriptor
                descriptor = buildDescriptorFromDocument(document);
            }

            if (descriptor == null) {
                // Cache the fact that no descriptor is available for this alias
                this.cache.addFromAlias(wikiAlias, DefaultWikiDescriptor.VOID);
            }
        }

        return descriptor != DefaultWikiDescriptor.VOID && descriptor != null ? descriptor.clone() : null;
    }

    @Override
    public WikiDescriptor getById(String wikiId) throws WikiManagerException
    {
        WikiDescriptor descriptor = this.cache.getFromId(wikiId);

        if (descriptor == null) {
            // Try to load a page named XWiki.XWikiServer<wikiId>
            XWikiDocument document = this.descriptorDocumentHelperProvider.get().getDocumentFromWikiId(wikiId);

            if (!document.isNew()) {
                // Build the descriptor
                descriptor = buildDescriptorFromDocument(document);
            } else if (getMainWikiId().equals(wikiId)) {
                // Return a "virtual" descriptor if main wiki does not yet have a descriptor document
                descriptor = new WikiDescriptor(wikiId, "localhost");
            }

            if (descriptor == null) {
                // Cache the fact that no descriptor is available for this alias
                this.cache.addFromId(wikiId, DefaultWikiDescriptor.VOID);
            }
        }

        return descriptor != DefaultWikiDescriptor.VOID && descriptor != null ? descriptor.clone() : null;
    }

    @Override
    public boolean exists(String wikiId) throws WikiManagerException
    {
        return getAllIds().contains(wikiId);
    }

    @Override
    public void saveDescriptor(WikiDescriptor descriptor) throws WikiManagerException
    {
        try {
            this.wikiDescriptorBuilderProvider.get().save(descriptor);
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
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.getMainXWiki() : "xwiki";
    }

    @Override
    public boolean isMainWiki(String wikiId)
    {
        if (StringUtils.isEmpty(wikiId)) {
            return true;
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.isMainWiki(wikiId) : true;
    }

    @Override
    public String getCurrentWikiId()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.getWikiId() : null;
    }

    @Override
    public WikiReference getCurrentWikiReference()
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        return xcontext != null ? xcontext.getWikiReference() : null;
    }

    @Override
    public WikiDescriptor getCurrentWikiDescriptor() throws WikiManagerException
    {
        return getById(getCurrentWikiId());
    }

    private DefaultWikiDescriptor buildDescriptorFromDocument(XWikiDocument document)
    {
        DefaultWikiDescriptor descriptor = null;
        List<BaseObject> serverClassObjects = document.getXObjects(DefaultWikiDescriptor.SERVER_CLASS);
        if (serverClassObjects != null && !serverClassObjects.isEmpty()) {
            descriptor = this.wikiDescriptorBuilderProvider.get().buildDescriptorObject(serverClassObjects, document);
            // Add to the cache
            if (descriptor != null) {
                this.cache.add(descriptor);
            }
        }

        return descriptor;
    }
}
