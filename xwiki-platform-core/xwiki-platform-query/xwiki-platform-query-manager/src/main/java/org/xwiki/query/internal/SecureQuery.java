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
package org.xwiki.query.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;

/**
 * Query implementation that allows to easily choose if hidden documents must be displayed in query results.
 *
 * @version $Id$
 * @since 4.1M1
 */
public class  SecureQuery extends DefaultQuery
{
    /**
     * Used to retrieve {@link org.xwiki.query.QueryFilter} implementations.
     */
    private ComponentManager componentManager;

    /**
     * Create a secure Query.
     *
     * @param statement query statement
     * @param language query language
     * @param executor QueryExecutor component for execute the query.
     * @param cm component manager.
     */
    public SecureQuery(String statement, String language, QueryExecutor executor, ComponentManager cm)
    {
        super(statement, language, executor);
        this.componentManager = cm;
        setHidden(true);
    }

    /**
     * Create a named secured Query.
     *
     * @param queryName name of the query.
     * @param executor QueryExecutor component for execute the query.
     * @param cm component manager.
     */
    public SecureQuery(String queryName, QueryExecutor executor,  ComponentManager cm)
    {
        super(queryName, executor);
        this.componentManager = cm;
        setHidden(true);
    }

    /**
     * Controls if hidden documents must be included in the query results.
     *
     * @param hidden <code>true</code> to include hidden documents in query results, <code>false</code> to exclude them.
     * @return this query object.
     */
    public Query setHidden(boolean hidden)
    {
        if (hidden) {
            try {
                setFilter(componentManager.lookup(QueryFilter.class, "hiddenDocument"));
            } catch (ComponentLookupException e) {
                // We need to avoid throwing exceptions in the wiki if the hidden document filter is missing.
            }
        } else {
            setFilter(null);
        }

        return this;
    }
}