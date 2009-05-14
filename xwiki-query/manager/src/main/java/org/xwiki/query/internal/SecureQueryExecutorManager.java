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

import java.util.List;
import java.util.Set;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;

/**
 * {@link QueryExecutorManager} with access rights checking.
 * @version $Id$
 */
//Note that we force the Component annotation so that this component is only registered as a QueryExecutorManager
//and not a QueryExecutor too since we don't want this manager to be visible to users as a valid QueryExecutor
//component.
@Component(value = "secure", roles = { QueryExecutorManager.class })
public class SecureQueryExecutorManager implements QueryExecutorManager
{
    /**
     * Nested {@link QueryExecutorManager}.
     */
    @Requirement
    private QueryExecutorManager nestedQueryExecutorManager;

    /**
     * Bridge to xwiki-core for check programming right.
     */
    @Requirement
    private DocumentAccessBridge bridge;

    /**
     * {@inheritDoc}
     */
    public <T> List<T> execute(Query query) throws QueryException
    {
        if (query.getWiki() != null && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Query#setWiki requires programming right", query, null);
        }
        if (query.isNamed() && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Named queries requires programming right", query, null);
        }
        if (!Query.XWQL.equals(query.getLanguage()) && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Query languages others than XWQL requires programming right", query, null);
        }
        if (query.getStatement().toLowerCase().startsWith("select") && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Full form XWQL statements requires programming right", query, null);
        }
        return getNestedQueryExecutorManager().execute(query);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getLanguages()
    {
        return getNestedQueryExecutorManager().getLanguages();
    }

    /**
     * @return {@link DocumentAccessBridge}
     */
    protected DocumentAccessBridge getBridge()
    {
        return bridge;
    }

    /**
     * @return nested {@link QueryExecutorManager}
     */
    protected QueryExecutorManager getNestedQueryExecutorManager()
    {
        return nestedQueryExecutorManager;
    }
}
