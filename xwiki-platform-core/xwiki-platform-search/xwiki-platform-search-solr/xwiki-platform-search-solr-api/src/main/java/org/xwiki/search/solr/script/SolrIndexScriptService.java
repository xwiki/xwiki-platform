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
package org.xwiki.search.solr.script;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service exposing interaction with the {@link SolrIndexer}. Queries on the index are performed using XWiki's <a
 * href="http://extensions.xwiki.org/xwiki/bin/view/Extension/Query+Module">Query Module API</a> with query type "solr".
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("solr")
@Singleton
public class SolrIndexScriptService implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Wrapped {@link SolrIndex} component.
     */
    @Inject
    private SolrIndexer solrIndexer;

    /**
     * Used to check rights.
     */
    @Inject
    private AuthorizationManager authorization;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to extract a {@link DocumentReference} from a {@link SolrDocument}.
     */
    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    /**
     * Used to extract an {@link EntityReference} from a {@link SolrDocument}.
     */
    @Inject
    private EntityReferenceResolver<SolrDocument> solrEntityReferenceResolver;

    /**
     * Index an entity and all it's contained entities recursively.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param reference the reference to index.
     */
    public void index(EntityReference reference)
    {
        clearException();

        try {
            checkAccessToWikiIndex(reference);

            this.solrIndexer.index(reference, true);
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * Index multiple entities and all their contained entities recursively. This is a batch operation.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param references the references to index.
     */
    public void index(List<EntityReference> references)
    {
        clearException();

        try {
            checkAccessToWikiIndex(references);

            for (EntityReference reference : references) {
                this.solrIndexer.index(reference, true);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * Delete an indexed entity and all its contained entities recursively.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param reference the reference to delete from the index.
     */
    public void delete(EntityReference reference)
    {
        clearException();

        try {
            checkAccessToWikiIndex(reference);

            this.solrIndexer.delete(reference, true);
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * Delete multiple entities and all their contained entities recursively. This is a batch operation.
     * <p>
     * Null reference means the whole farm.
     * 
     * @param references the references to delete from the index.
     */
    public void delete(List<EntityReference> references)
    {
        clearException();

        try {
            checkAccessToWikiIndex(references);

            for (EntityReference reference : references) {
                this.solrIndexer.delete(reference, true);
            }
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * @return the size of the index/delete queue
     * @since 5.1RC1
     */
    public int getQueueSize()
    {
        return this.solrIndexer.getQueueSize();
    }

    /**
     * Extract a {@link DocumentReference} from the given {@link SolrDocument} (e.g. search result).
     * 
     * @param document the {@link SolrDocument} to extract the {@link DocumentReference} from
     * @param parameters the parameters to pass to the reference resolver (e.g. in case some reference components are
     *            missing)
     * @return the reference to the document associated with the given {@link SolrDocument}
     * @since 7.2RC1
     */
    public DocumentReference resolveDocument(SolrDocument document, Object... parameters)
    {
        return this.solrDocumentReferenceResolver.resolve(document, parameters);
    }

    /**
     * Extract an {@link EntityReference} from the given {@link SolrDocument} (e.g. search result). The entity type is
     * inferred from the "type" field which must be specified and must have a valid value (that corresponds to an
     * existing {@link EntityType}).
     * 
     * @param document a {@link SolrDocument} to extract the {@link EntityReference} from (the "type" field must be
     *            specified)
     * @param parameters the parameters to pass to the reference resolver (e.g. in case some reference components are
     *            missing)
     * @return the reference to the entity associated with the given {@link SolrDocument}
     * @since 7.2RC1
     */
    public EntityReference resolve(SolrDocument document, Object... parameters)
    {
        EntityType type;
        try {
            type = EntityType.valueOf((String) document.get(FieldUtils.TYPE));
        } catch (IllegalArgumentException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }

        return resolve(document, type, parameters);
    }

    /**
     * Extract an {@link EntityReference} of the specified type from the given {@link SolrDocument} (e.g. search
     * result).
     * 
     * @param document a {@link SolrDocument} to extract the {@link EntityReference} from
     * @param type the entity type
     * @param parameters the parameters to pass to the reference resolver (e.g. in case some reference components are
     *            missing)
     * @return the reference to the entity associated with the given {@link SolrDocument}
     * @since 7.2RC1
     */
    public EntityReference resolve(SolrDocument document, EntityType type, Object... parameters)
    {
        return this.solrEntityReferenceResolver.resolve(document, type, parameters);
    }

    /**
     * Log exception and store the exception in the context.
     * 
     * @param errorMessage the error message to log.
     * @param e the caught exception.
     * @see #CONTEXT_LASTEXCEPTION
     */
    private void error(String errorMessage, Exception e)
    {
        String errorMessageToLog = errorMessage;
        if (errorMessageToLog == null) {
            errorMessageToLog = e.getMessage();
        }

        this.logger.error(errorMessageToLog, e);

        this.xcontextProvider.get().put(CONTEXT_LASTEXCEPTION, e);
    }

    /**
     * Log exception and store it in the context. The logged message is the exception's message. This allows the
     * underlying component to define it's messages and removes duplication.
     * 
     * @param e the caught exception
     */
    private void error(Exception e)
    {
        error(null, e);
    }

    /**
     * Clear the last exception from the context.
     */
    private void clearException()
    {
        this.xcontextProvider.get().remove(CONTEXT_LASTEXCEPTION);
    }

    /**
     * Check the current user's access to alter the index of the wiki owning the given referenced entity.
     * 
     * @param reference the reference whose owning wiki to check.
     * @throws AccessDeniedException if the current user or author is not allowed
     */
    private void checkAccessToWikiIndex(EntityReference reference) throws AccessDeniedException
    {
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);

        XWikiContext xcontext = this.xcontextProvider.get();

        this.authorization.checkAccess(Right.ADMIN, xcontext.getUserReference(), wikiReference);
        this.contextualAuthorizationManager.checkAccess(Right.PROGRAM);
    }

    /**
     * Check the current user's access to alter the index of the wikis owning the given referenced entities.
     * 
     * @param references the references whose owning wikis to check.
     * @throws AccessDeniedException if the current user or author is not allowed
     */
    private void checkAccessToWikiIndex(List<EntityReference> references) throws AccessDeniedException
    {
        Set<EntityReference> representatives = new HashSet<EntityReference>();
        for (EntityReference reference : references) {
            EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
            if (!representatives.contains(wikiReference)) {
                checkAccessToWikiIndex(wikiReference);
                representatives.add(wikiReference);
            }
        }
    }
}
