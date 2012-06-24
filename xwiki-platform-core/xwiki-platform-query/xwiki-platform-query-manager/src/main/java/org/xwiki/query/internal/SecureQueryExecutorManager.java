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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;

/**
 * {@link QueryExecutorManager} with access rights checking.
 *
 * @version $Id$
 */
// Note that we force the Component annotation so that this component is only registered as a QueryExecutorManager
// and not a QueryExecutor too since we don't want this manager to be visible to users as a valid QueryExecutor
// component.
@Component(roles = { QueryExecutorManager.class })
@Named("secure")
@Singleton
public class SecureQueryExecutorManager implements QueryExecutorManager
{
    /**
     * Nested {@link QueryExecutorManager}.
     */
    @Inject
    private QueryExecutorManager nestedQueryExecutorManager;

    /**
     * Bridge to xwiki-core for checking programming right.
     */
    @Inject
    private DocumentAccessBridge bridge;

    /**
     * @param statement the statement to evaluate.
     * @return true if the statement is complete, false otherwise.
     */
    private boolean isShortFormStatement(String statement)
    {
        boolean isShortStatement = false;
        String lcStatement = statement.trim().toLowerCase();

        isShortStatement |= lcStatement.startsWith(", ");
        isShortStatement |= lcStatement.startsWith("from");
        isShortStatement |= lcStatement.startsWith("where");
        isShortStatement |= lcStatement.startsWith("order");

        return isShortStatement;
    }

    @Override
    public <T> List<T> execute(Query query) throws QueryException
    {
        if (query.isNamed() && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Named queries requires programming right", query, null);
        }
        if (!Query.XWQL.equals(query.getLanguage()) && !Query.HQL.equals(query.getLanguage())
                && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Query languages others than XWQL or HQL require programming right", query, null);
        }
        if (!isShortFormStatement(query.getStatement()) && !getBridge().hasProgrammingRights()) {
            throw new QueryException("Full form statements requires programming right", query, null);
        }
        return getNestedQueryExecutorManager().execute(query);
    }

    @Override
    public Set<String> getLanguages()
    {
        return getNestedQueryExecutorManager().getLanguages();
    }

    /**
     * @return {@link DocumentAccessBridge}
     */
    protected DocumentAccessBridge getBridge()
    {
        return this.bridge;
    }

    /**
     * @return nested {@link QueryExecutorManager}
     */
    protected QueryExecutorManager getNestedQueryExecutorManager()
    {
        return this.nestedQueryExecutorManager;
    }
}
