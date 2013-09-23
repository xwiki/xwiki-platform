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

import java.util.Set;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutorManager;
import org.xwiki.query.QueryManager;

/**
 * Base QueryManager implementation.
 *
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractQueryManager implements QueryManager
{
    /**
     * @return the query executor manager to use. This allows extending classes to provide their version of it (for
     *         example using the Default manager or the Secure one or any other)
     */
    protected abstract QueryExecutorManager getQueryExecutorManager();

    @Override
    public Set<String> getLanguages()
    {
        return getQueryExecutorManager().getLanguages();
    }

    @Override
    public boolean hasLanguage(String language)
    {
        return getLanguages().contains(language);
    }

    @Override
    public Query createQuery(String statement, String language) throws QueryException
    {
        if (hasLanguage(language)) {
            return new DefaultQuery(statement, language, getQueryExecutorManager());
        } else {
            throw new QueryException("Language [" + language + "] is not supported", null, null);
        }
    }

    @Override
    public Query getNamedQuery(String queryName) throws QueryException
    {
        return new DefaultQuery(queryName, getQueryExecutorManager());
    }
}
