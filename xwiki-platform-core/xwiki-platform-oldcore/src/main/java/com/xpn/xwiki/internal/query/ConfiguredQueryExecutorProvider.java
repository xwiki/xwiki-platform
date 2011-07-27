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

import com.xpn.xwiki.XWikiContext;
import javax.inject.Inject;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryExecutorProvider;

/**
 * A provider of QueryExecutor.
 * Allows providing of a QueryExecutor defined by the configuration.
 *
 * @version $Id$
 * @since 3.2M2
 */
@Component
public class ConfiguredQueryExecutorProvider implements QueryExecutorProvider, Initializable
{
    /** A means of getting QueryExecutors. */
    @Inject
    private ComponentManager manager;

    /** The execution, needed to get the main store hint which is used to choose the QueryExecutor. */
    @Inject
    private Execution exec;

    /** The QueryExecutor which we will provide. */
    private QueryExecutor queryExecutor;

    /**
     * {@inheritDoc}
     *
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        final XWikiContext context = (XWikiContext) exec.getContext().getProperty("xwikicontext");
        final String storeName = context.getWiki().Param("xwiki.store.main.hint", "default");
        try {
            this.queryExecutor = this.manager.lookup(QueryExecutor.class, storeName);
        } catch (ComponentLookupException e) {
            throw new InitializationException("Could not find a QueryExecutor with hint " + storeName
                                              + " which is the hint for the storage engine. "
                                              + "a QueryExecutor will not be available", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see QueryExecutorProvider#get()
     */
    public QueryExecutor get()
    {
        return this.queryExecutor;
    }
}
