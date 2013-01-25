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
package org.xwiki.search.solr.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrIndex;
import org.xwiki.search.solr.internal.api.SolrIndexException;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service exposing interaction with the Solr index. Queries on the index are performed using XWiki's <a
 * href="http://extensions.xwiki.org/xwiki/bin/view/Extension/Query+Module">Query Module API</a> with query type "solr".
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("solr")
public class SolrIndexScriptService implements ScriptService
{
    /**
     * Field name of the last API exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * The Solr configuration.
     */
    @Inject
    protected SolrConfiguration configuration;

    /**
     * Execution context.
     */
    @Inject
    protected Execution execution;

    /**
     * Logging framework.
     */
    @Inject
    protected Logger logger;

    /**
     * Wrapped {@link SolrIndex} component.
     */
    @Inject
    protected SolrIndex solrIndex;

    /**
     * TODO DOCUMENT ME!
     * 
     * @param reference the reference to index.
     */
    public void index(EntityReference reference)
    {
        clearException();

        try {
            solrIndex.index(reference);
        } catch (SolrIndexException e) {
            error(e);
        }
    }

    /**
     * TODO DOCUMENT ME!
     * 
     * @param references the references to index.
     */
    public void index(List<EntityReference> references)
    {
        clearException();

        try {
            solrIndex.index(references);
        } catch (SolrIndexException e) {
            error(e);
        }
    }

    /**
     * TODO DOCUMENT ME!
     * 
     * @param reference the reference to delete from the index.
     */
    public void delete(EntityReference reference)
    {
        clearException();

        try {
            solrIndex.delete(reference);
        } catch (SolrIndexException e) {
            error(e);
        }
    }

    /**
     * TODO DOCUMENT ME!
     * 
     * @param references the references to delete from the index.
     */
    public void delete(List<EntityReference> references)
    {
        clearException();

        try {
            solrIndex.delete(references);
        } catch (SolrIndexException e) {
            error(e);
        }
    }

    /**
     * @see SolrConfiguration#getOptimizableLanguages()
     * @return the list of supported language codes for which optimized indexing can be performed.
     */
    public List<String> getOptimizableLanguages()
    {
        clearException();

        return this.configuration.getOptimizableLanguages();
    }

    /**
     * @see SolrConfiguration#getOptimizedLanguages()
     * @return the list of language codes for which to perform optimized indexing.
     */
    public List<String> getOptimizedLanguages()
    {
        clearException();

        return this.configuration.getOptimizedLanguages();
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

        logger.error(errorMessageToLog, e);

        getXWikiContext().put(CONTEXT_LASTEXCEPTION, e);
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
        getXWikiContext().remove(CONTEXT_LASTEXCEPTION);
    }

    /**
     * @return the XWikiContext
     */
    protected XWikiContext getXWikiContext()
    {
        ExecutionContext executionContext = this.execution.getContext();
        XWikiContext context = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        // FIXME: Do we need this? Maybe when running an index Thread?
        // if (context == null) {
        // context = this.contextProvider.createStubContext();
        // executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);
        // }
        return context;
    }
}
