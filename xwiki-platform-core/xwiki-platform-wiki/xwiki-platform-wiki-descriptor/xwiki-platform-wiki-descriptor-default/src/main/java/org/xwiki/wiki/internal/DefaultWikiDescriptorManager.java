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
package org.xwiki.wiki.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.WikiDescriptor;
import org.xwiki.wiki.WikiDescriptorAlias;
import org.xwiki.wiki.WikiDescriptorException;
import org.xwiki.wiki.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
public class DefaultWikiDescriptorManager implements WikiDescriptorManager, Initializable
{
    @Inject
    private CacheFactory cacheFactory;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorBuilder wikiDescriptorBuilder;

    @Inject
    private Execution execution;

    private Cache<WikiDescriptor> wikiAliasCache;

    private Cache<WikiDescriptor> wikiIdCache;

    @Override
    public void initialize() throws InitializationException
    {
        this.wikiAliasCache = createCache("wiki.descriptor.cache.wikiAlias");
        this.wikiIdCache = createCache("wiki.descriptor.cache.wikiId");
    }

    private Cache<WikiDescriptor> createCache(String cacheId) throws InitializationException
    {
        Cache<WikiDescriptor> cache;

        CacheConfiguration configuration = new CacheConfiguration(cacheId);

        try {
            cache = this.cacheFactory.newCache(configuration);
        } catch (CacheException e) {
            throw new InitializationException(String.format("Failed to initialize wiki descriptor caches [%s]",
                configuration.getConfigurationId()), e);
        }

        return cache;
    }

    @Override
    public WikiDescriptor getByWikiAlias(String wikiAlias) throws WikiDescriptorException
    {
        WikiDescriptor descriptor = this.wikiAliasCache.get(wikiAlias);

        // If not found in the cache then query the wiki and add to the cache if found.
        //
        // Note that an alternative implementation would have been to find all Wiki Descriptors at startup but this
        // would have meant keeping them all in memory at once. Since we want to be able to scale to any number of
        // subwikis we only cache the most used one. This allows inactive wikis to not take up any memory for example.
        // Note that In order for performance to be maximum it also means we need to have a cache size at least as
        // large as the max # of wikis being used at once.
        if (descriptor == null) {
            DocumentReference reference = findXWikiServerClassDocumentReference(wikiAlias);
            if (reference != null) {
                descriptor = set(getDocument(reference));
            }
        }

        return descriptor;
    }

    @Override
    public WikiDescriptor getByWikiId(String wikiId) throws WikiDescriptorException
    {
        WikiDescriptor descriptor = this.wikiIdCache.get(wikiId);

        if (descriptor == null) {
            // Try to load a page named XWiki.XWikiServer<wikiId>
            XWikiDocument document = getDocument(new DocumentReference(getXWikiContext().getMainXWiki(),"XWiki",
                String.format("XWikiServer%s", StringUtils.capitalize(wikiId))));
            if (!document.isNew()) {
                descriptor = set(document);
            }
        }

        return descriptor;
    }

    private WikiDescriptor set(XWikiDocument document)
    {
        WikiDescriptor descriptor = this.wikiDescriptorBuilder.build(
            document.getXObjects(WikiDescriptorListener.SERVER_CLASS), document, getXWikiContext());
        // Add to the cache
        if (descriptor != null) {
            set(descriptor);
        }
        return descriptor;
    }

    private DocumentReference findXWikiServerClassDocumentReference(String wikiAlias)
        throws WikiDescriptorException
    {
        DocumentReference result = null;

        try {
            Query query = this.queryManager.createQuery(
                "where doc.object(XWiki.XWikiServerClass).server = :wikiAlias and doc.name like 'XWikiServer%'",
                Query.XWQL);
            query.bindValue("wikiAlias", wikiAlias);
            query.setWiki(getXWikiContext().getMainXWiki());
            List<String> documentNames = query.execute();

            // Resolve the document name into a references
            if (documentNames != null && !documentNames.isEmpty()) {
                result = this.documentReferenceResolver.resolve(documentNames.get(0));
            }

        } catch (QueryException e) {
            throw new WikiDescriptorException(String.format(
                "Failed to locate XWiki.XWikiServerClass document for wiki alias [%s]", wikiAlias), e);
        }

        return result;
    }

    @Override
    public void set(WikiDescriptor descriptor)
    {
        // Update the wiki name cache
        this.wikiIdCache.set(descriptor.getWikiId(), descriptor);

        // Update the wiki alias cache
        this.wikiAliasCache.set(descriptor.getWikiAlias(), descriptor);
        for (WikiDescriptorAlias alias : descriptor.getDescriptorAliases()) {
            this.wikiAliasCache.set(alias.getWikiAlias(), descriptor);
        }
    }

    @Override
    public void remove(WikiDescriptor descriptor)
    {
        // Remove from the wiki name cache
        this.wikiIdCache.remove(descriptor.getWikiId());

        // Remove from the wiki alias cache
        this.wikiAliasCache.remove(descriptor.getWikiAlias());
        for (WikiDescriptorAlias alias : descriptor.getDescriptorAliases()) {
            this.wikiAliasCache.remove(alias.getWikiAlias());
        }
    }

    @Override
    public Collection<WikiDescriptor> getAll() throws WikiDescriptorException
    {
        // Note: Ideally to improve performance we could imagine loading all XWikiServerClasses at initialization time
        // (in initialize()) and thereafter only use the cache. The problem with this approach is that our Cache will
        // need to be unbounded which is not the case right now. This would mean being able to put all descriptors in
        // the cache and thus it might not scale if there were a very large number of wikis.

        List<WikiDescriptor> result = new ArrayList<WikiDescriptor>();

        try {
            Query query = this.queryManager.createQuery(
                "from doc.object(XWiki.XWikiServerClass) as descriptor where doc.name like 'XWikiServer%'",
                Query.XWQL);
            query.setWiki(getXWikiContext().getMainXWiki());
            List<String> documentNames = query.execute();

            if (documentNames != null && !documentNames.isEmpty()) {
                for (String documentName : documentNames) {
                    // Resolve the document names into references and for each one extract the WikiDescriptor
                    result.add(set(getDocument(this.documentReferenceResolver.resolve(documentName))));
                }
            }
        } catch (Exception e) {
            throw new WikiDescriptorException("Failed to locate XWiki.XWikiServerClass documents", e);
        }

        return result;
    }

    private XWikiDocument getDocument(DocumentReference reference) throws WikiDescriptorException
    {
        XWikiContext context = getXWikiContext();
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        try {
            return xwiki.getDocument(reference, context);
        } catch (XWikiException e) {
            throw new WikiDescriptorException(String.format(
                "Failed to get document [%s] containing a XWiki.XWikiServerClass object", reference), e);
        }
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
