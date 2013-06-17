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
package org.xwiki.search.solr.internal.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service exposing interaction with the {@link SolrIndex}. Queries on the index are performed using XWiki's <a
 * href="http://extensions.xwiki.org/xwiki/bin/view/Extension/Query+Module">Query Module API</a> with query type "solr".
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Unstable
@Component
@Named("solr")
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
    private SolrIndexer solrIndex;

    /**
     * Used to check rights.
     */
    @Inject
    private AuthorizationManager authorization;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

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

            this.solrIndex.index(reference, true);
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
                this.solrIndex.index(reference, true);
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

            this.solrIndex.delete(reference, true);
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
                this.solrIndex.delete(reference, true);
            }
        } catch (Exception e) {
            error(e);
        }
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
     * @throws IllegalAccessException if the user is not allowed or if problems occur.
     */
    private void checkAccessToWikiIndex(EntityReference reference) throws IllegalAccessException
    {
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);

        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference userReference = xcontext.getUserReference();
        DocumentReference programmingUserReference = xcontext.getDoc().getContentAuthorReference();

        if (!this.authorization.hasAccess(Right.ADMIN, userReference, wikiReference)
            || !this.authorization.hasAccess(Right.PROGRAM, programmingUserReference, wikiReference)) {
            throw new IllegalAccessException(String.format(
                "The user '%s' is not allowed to alter the index for the entity '%s'", userReference, reference));
        }
    }

    /**
     * Check the current user's access to alter the index of the wikis owning the given referenced entities. This is an
     * optimized method that only checks one reference for each distinct wiki.
     * 
     * @param references the references whose owning wikis to check.
     * @throws IllegalAccessException if the user is not allowed for at least one of the passed references or if
     *             problems occur.
     */
    private void checkAccessToWikiIndex(List<EntityReference> references) throws IllegalAccessException
    {
        // Build a map of representatives for each wiki to avoid checking every reference.
        Map<EntityReference, EntityReference> representatives = new HashMap<EntityReference, EntityReference>();
        for (EntityReference reference : references) {
            EntityReference wikiReference = reference.extractReference(EntityType.WIKI);
            if (!representatives.containsKey(wikiReference)) {
                representatives.put(wikiReference, reference);
            }
        }

        // Check only the representatives for each wiki.
        for (EntityReference reference : representatives.values()) {
            checkAccessToWikiIndex(reference);
        }
    }
}
