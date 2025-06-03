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
package org.xwiki.query.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.SecureQuery;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;

/**
 * Provides Query Manager-specific Scripting APIs.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@Component
@Named(QueryManagerScriptService.ROLEHINT)
@Singleton
public class QueryManagerScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "query";

    /**
     * Secure query manager that performs checks on rights depending on the query being executed.
     */
    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    /**
     * Used to create {@link org.xwiki.query.internal.ScriptQuery}.
     */
    @Inject
    private ComponentManager componentManager;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(QueryManagerScriptService.ROLEHINT + '.' + serviceName);
    }

    /**
     * Shortcut for writing a XWQL query.
     * 
     * @param statement the XWQL statement for the query
     * @return the Query object, ready to be executed
     * @throws org.xwiki.query.QueryException if any errors
     */
    public Query xwql(String statement) throws QueryException
    {
        return createQuery(statement, Query.XWQL, false);
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
        return createQuery(statement, Query.HQL, false);
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
        return createQuery(statement, language, true);
    }

    /**
     * Allow creating query parameters. For example:
     * <pre>{@code
     * #set ($queryParams = [])
     * #set ($whereQueryPart = "${whereQueryPart} AND doc.space = ?")
     * #set ($discard = $queryParams.add($services.query.parameter().literal($request.space)))
     * }</pre>
     *
     * @return the Query parameter object, see {@link QueryParameter} for more details on how to create the parameter
     *         content
     *
     * @since 8.4.5
     * @since 9.3RC1
     */
    public QueryParameter parameter()
    {
        return new DefaultQueryParameter(null);
    }

    private Query createQuery(String statement, String language, boolean checkCurrentUser) throws QueryException
    {
        Query query = this.secureQueryManager.createQuery(statement, language);
        if (query instanceof SecureQuery) {
            ((SecureQuery) query).checkCurrentAuthor(true);
            ((SecureQuery) query).checkCurrentUser(checkCurrentUser);
        }

        return new ScriptQuery(query, this.componentManager);
    }
}
