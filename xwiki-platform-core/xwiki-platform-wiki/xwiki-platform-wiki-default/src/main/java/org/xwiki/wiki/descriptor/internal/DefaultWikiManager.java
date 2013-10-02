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
package org.xwiki.wiki.descriptor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorAlias;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;

@Component
@Singleton
public class DefaultWikiManager implements WikiManager, Initializable
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

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorBuilder builder;

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

    private WikiDescriptor createDescriptor(String wikiId, String alias) throws WikiManagerException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        WikiDescriptor wikiDescriptor = null;

        try {
            // Create the descriptor document
            DocumentReference wikiDescriptorReference =
                    new DocumentReference(context.getMainXWiki(), XWiki.SYSTEM_SPACE, String.format("XWikiServer%s",
                            StringUtils.capitalize(wikiId.toLowerCase())));
            XWikiDocument wikiDescriptorDocument = xwiki.getDocument(wikiDescriptorReference, context);
            BaseObject obj = wikiDescriptorDocument.newXObject(DefaultWikiDescriptor.SERVER_CLASS, context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_SERVER, alias, context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE, "Main.WebHome", context);

            // Save the document
            xwiki.saveDocument(wikiDescriptorDocument, context);

            // Create the descriptor object
            wikiDescriptor = setDescriptor(wikiDescriptorDocument);

        } catch (XWikiException e) {
            logger.error(e.getMessage());
            throw new WikiManagerException("Unable to create the descriptor");
        }

        // The Wiki Descriptor Listener should have created the new wiki descriptor
        return wikiDescriptor;
    }

    @Override
    public WikiDescriptor createWiki(String wikiId, String wikiAlias) throws WikiManagerException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Check that the wiki does not already exist
        if (wikiIdExists(wikiId)) {
            throw new WikiManagerException("wiki already exists");
        }

        // Check that the wiki ID is correct
        String wikiForbiddenList = xwiki.Param("xwiki.virtual.reserved_wikis");
        if (Util.contains(wikiId, wikiForbiddenList, ", ") || wikiId.equals(context.getMainXWiki())) {
            throw new WikiManagerException("wikiId reserved");
        }

        // Create database/schema
        try {
            xwiki.getStore().createWiki(wikiId, context);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databasecreation"));
        }

        // Init database/schema
        try {
            xwiki.updateDatabase(wikiId, true, true, context);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new WikiManagerException(localizationManager.getTranslationPlain("wiki.databaseupdate"));
        }

        // Create the descriptor
        WikiDescriptor descriptor = createDescriptor(wikiId, wikiAlias);

        return descriptor;
    }

    @Override
    public void deleteWiki(WikiDescriptor descriptor) throws WikiManagerException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        String wikiId = descriptor.getWikiId();

        // Check if we try to delete the main wiki
        if (wikiId.equals(context.getMainXWiki())) {
            throw new WikiManagerException("can't delete main wiki");
        }

        // Delete the database
        try {
            xwiki.getStore().deleteWiki(wikiId, context);
        } catch (XWikiException e) {
            throw new WikiManagerException("can't delete database");
        }

        // Delete the descriptor document
        if (descriptor instanceof DefaultWikiDescriptor) {
            DefaultWikiDescriptor desc = (DefaultWikiDescriptor) descriptor;
            try {
                xwiki.deleteDocument(desc.getDocument(), context);
            } catch (XWikiException e) {
                throw new WikiManagerException("can't delete descriptor document");
            }
        }

        // Send an event
        observationManager.notify(new WikiDeletedEvent(wikiId), descriptor);
    }

    @Override
    public WikiDescriptor getByWikiAlias(String wikiAlias) throws WikiManagerException
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
                descriptor = setDescriptor(getDocument(reference));
            }
        }

        return descriptor;
    }

    @Override
    public WikiDescriptor getByWikiId(String wikiId) throws WikiManagerException
    {
        WikiDescriptor descriptor = this.wikiIdCache.get(wikiId);

        if (descriptor == null) {
            // Try to load a page named XWiki.XWikiServer<wikiId>
            XWikiDocument document = getDocument(new DocumentReference(getXWikiContext().getMainXWiki(),"XWiki",
                String.format("XWikiServer%s", StringUtils.capitalize(wikiId))));
            if (!document.isNew()) {
                descriptor = setDescriptor(document);
            }
        }

        return descriptor;
    }

    private WikiDescriptor setDescriptor(XWikiDocument document)
    {
        WikiDescriptor descriptor = this.wikiDescriptorBuilder.build(
            document.getXObjects(DefaultWikiDescriptor.SERVER_CLASS), document, getXWikiContext());
        // Add to the cache
        if (descriptor != null) {
            setDescriptor(descriptor);
        }
        return descriptor;
    }

    private DocumentReference findXWikiServerClassDocumentReference(String wikiAlias)
        throws WikiManagerException
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
            throw new WikiManagerException(String.format(
                "Failed to locate XWiki.XWikiServerClass document for wiki alias [%s]", wikiAlias), e);
        }

        return result;
    }

    @Override
    public void setDescriptor(WikiDescriptor descriptor)
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
    public void removeDescriptor(WikiDescriptor descriptor)
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
    public Collection<WikiDescriptor> getAll() throws WikiManagerException
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
                    result.add(setDescriptor(getDocument(this.documentReferenceResolver.resolve(documentName))));
                }
            }
        } catch (Exception e) {
            throw new WikiManagerException("Failed to locate XWiki.XWikiServerClass documents", e);
        }

        return result;
    }

    @Override
    public boolean wikiIdExists(String wikiId) throws WikiManagerException {
        return getByWikiId(wikiId) != null;
    }

    @Override
    public boolean isWikiIdAvailable(String wikiId) throws WikiManagerException {
        return !wikiIdExists(wikiId);
    }

    private XWikiDocument getDocument(DocumentReference reference) throws WikiManagerException
    {
        XWikiContext context = getXWikiContext();
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        try {
            return xwiki.getDocument(reference, context);
        } catch (XWikiException e) {
            throw new WikiManagerException(String.format(
                "Failed to get document [%s] containing a XWiki.XWikiServerClass object", reference), e);
        }
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
