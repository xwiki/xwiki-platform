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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryExecutorManager;

/**
 * Default implementation of {@link QueryExecutorManager}.
 * 
 * @version $Id$
 */
// Note that we force the Component annotation so that this component is only registered as a
// QueryExecutorManager and not a QueryExecutor too since we don't want this manager to be visible
// to users as a valid QueryExecutor component.
@Component(roles = { QueryExecutorManager.class })
@Singleton
public class DefaultQueryExecutorManager implements QueryExecutorManager
{
    /**
     * Map from language to its executor.
     */
    @Inject
    private Map<String, QueryExecutor> executors;

    /**
     * Executor provider for named queries.
     * This provider will give us an executor which is native to the type of storage engine used.
     */
    @Inject
    private Provider<QueryExecutor> namedQueryExecutorProvider;

    @Override
    public <T> List<T> execute(Query query) throws QueryException
    {
        if (query.isNamed()) {
            return this.namedQueryExecutorProvider.get().execute(query);
        } else {
            return this.executors.get(query.getLanguage()).execute(query);
        }
    }

    @Override
    public Set<String> getLanguages()
    {
        return Collections.unmodifiableSet(this.executors.keySet());
    }
}
