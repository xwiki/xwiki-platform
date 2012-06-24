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
package com.xpn.xwiki.internal.query;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.query.QueryExecutor;

import com.xpn.xwiki.XWikiContext;

/**
 * A provider of QueryExecutor. Allows providing of a QueryExecutor defined by the configuration.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class ConfiguredQueryExecutorProvider implements Provider<QueryExecutor>
{
    /** A means of getting QueryExecutors. */
    @Inject
    private ComponentManager manager;

    /** The execution, needed to get the main store hint which is used to choose the QueryExecutor. */
    @Inject
    private Execution exec;

    /**
     * The QueryExecutor which we will provide. Start off by injecting the default then later (once the XWiki object is
     * initialized) that will be swapped out for whatever type of storage the system is using as per it's configuration.
     */
    @Inject
    private QueryExecutor queryExecutor;

    /** The man who cuts down trees. */
    @Inject
    private Logger logger;

    /** Set to true once the query executor has been initialized. */
    private boolean initialized;

    /**
     * Switch the queryExecutor based on what main store is being used. This is called lazily because the XWikiContext
     * might not yet exist when this class is instantiated.
     */
    private void init()
    {
        final XWikiContext context;
        try {
            context = (XWikiContext) exec.getContext().getProperty("xwikicontext");
        } catch (NullPointerException e) {
            this.logger.warn("The QueryExecutor was called without an XWikiContext available. "
                + "This means the old core (and likely the storage engine) is probably "
                + "not yet initialized. The default QueryExecutor will be returned.", e);
            return;
        }

        final String storeName = context.getWiki().Param("xwiki.store.main.hint", "default");
        try {
            this.queryExecutor = this.manager.getInstance(QueryExecutor.class, storeName);
        } catch (ComponentLookupException e) {
            this.logger.warn(
                "Could not find a QueryExecutor with hint " + storeName + " which is the hint for the storage engine. "
                    + "the default QueryExecutor will not be used instead.", e);
        }

        this.initialized = true;
    }

    @Override
    public QueryExecutor get()
    {
        if (!this.initialized) {
            this.init();
        }

        return this.queryExecutor;
    }
}
