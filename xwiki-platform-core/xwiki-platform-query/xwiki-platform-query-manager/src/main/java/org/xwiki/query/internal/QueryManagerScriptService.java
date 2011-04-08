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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Query Manager-specific Scripting APIs.
 *
 * @version $Id$
 * @since 2.4M2
 */
@Component("query")
public class QueryManagerScriptService implements ScriptService
{
    /**
     * Secure query manager that performs checks on rights depending on the query being executed.
     */
    @Requirement("secure")
    private QueryManager secureQueryManager;

    /**
     * Shortcut for writing a XWQL query.
     *
     * @param statement the XWQL statement for the query
     * @return the Query object, ready to be executed
     * @throws org.xwiki.query.QueryException if any errors
     */
    public Query xwql(String statement) throws QueryException
    {
        return this.secureQueryManager.createQuery(statement, Query.XWQL);
    }

    /**
     * Shortcut for writing a HQL query.
     *
     * @param statement the HQL statement for the query
     * @return the Query object, ready to be executed
     * @throws org.xwiki.query.QueryException if any errors
     */
    public Query hql(String statement) throws QueryException
    {
        return this.secureQueryManager.createQuery(statement, Query.HQL);
    }

    /**
     * Create a Query for the given statement and language.
     *
     * @param statement the query statement
     * @param language language of the query. Must be a supported language.
     * @return the Query object
     * @throws QueryException if language is not supported
     */
    public Query createQuery(String statement, String language) throws QueryException
    {
        return this.secureQueryManager.createQuery(statement, language);
    }
}
