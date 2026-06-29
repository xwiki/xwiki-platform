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

import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.WrappingQuery;

/**
 * Base class for query executors.
 *
 * @version $Id$
 * @since 17.8.0RC1
 */
public abstract class AbstractQueryExecutor implements QueryExecutor
{
    protected Query filterQuery(Query query, String language)
    {
        Query filteredQuery = query;

        if (hasFilters(query)) {
            for (QueryFilter filter : query.getFilters()) {
                // Step 1: For backward-compatibility reasons call #filterStatement() first
                String filteredStatement = filter.filterStatement(filteredQuery.getStatement(), language);
                // Prevent unnecessary creation of WrappingQuery objects when the QueryFilter doesn't modify the
                // statement.
                if (!filteredStatement.equals(filteredQuery.getStatement())) {
                    filteredQuery = new WrappingQuery(filteredQuery)
                    {
                        @Override
                        public String getStatement()
                        {
                            return filteredStatement;
                        }
                    };
                }
                // Step 2: Run #filterQuery()
                filteredQuery = filter.filterQuery(filteredQuery);
            }
        }
        return filteredQuery;
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> filterResults(Query query, List<T> results)
    {
        List<T> filteredResults = results;
        if (hasFilters(query)) {
            for (QueryFilter filter : query.getFilters()) {
                filteredResults = filter.filterResults(filteredResults);
            }
        }
        return filteredResults;
    }

    protected boolean hasFilters(Query query)
    {
        return query.getFilters() != null && !query.getFilters().isEmpty();
    }
}
