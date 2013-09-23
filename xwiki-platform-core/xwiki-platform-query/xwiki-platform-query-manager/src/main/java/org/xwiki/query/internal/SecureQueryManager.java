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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;

/**
 * QueryManager implementation to use in scripts (uses a secure {@link org.xwiki.query.QueryExecutorManager} which
 * performs checks for rights).
 * 
 * @version $Id$
 */
@Component
@Named("secure")
@Singleton
public class SecureQueryManager extends AbstractQueryManager
{
    /**
     * {@link QueryExecutorManager} for execute Queries.
     */
    @Inject
    @Named("secure")
    protected QueryExecutorManager queryExecutorManager;

    @Override
    protected QueryExecutorManager getQueryExecutorManager()
    {
        return this.queryExecutorManager;
    }

    /**
     * @param statement XWQL statement
     * @return Query
     * @throws QueryException if any errors
     * @see #createQuery(String, String)
     * @deprecated it's now available from {@link org.xwiki.query.internal.QueryManagerScriptService} since 2.4M2
     */
    @Deprecated
    public Query xwql(String statement) throws QueryException
    {
        return createQuery(statement, Query.XWQL);
    }
}