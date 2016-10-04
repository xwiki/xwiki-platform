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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.SecureQuery;

/**
 * Stores all information needed for execute a query.
 *
 * @version $Id$
 * @since 1.6M1
 */
public class DefaultQuery implements SecureQuery
{
    /**
     * Used to log possible warnings.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQuery.class);

    /**
     * field for {@link #isNamed()}.
     */
    protected boolean isNamed;

    /**
     * field for {@link Query#getStatement()}.
     */
    private String statement;

    /**
     * field for {@link Query#getLanguage()}.
     */
    private String language;

    /**
     * virtual wiki to run the query.
     */
    private String wiki;

    /**
     * map from query parameters to values.
     */
    private Map<String, Object> namedParameters = new HashMap<String, Object>();

    /**
     * map from index to positional parameter value.
     */
    private Map<Integer, Object> positionalParameters = new HashMap<Integer, Object>();

    /**
     * field for {@link Query#setLimit(int)}.
     */
    private int limit;

    /**
     * field for {@link Query#setOffset(int)}.
     */
    private int offset;

    /**
     * @see #isCurrentAuthorChecked()
     */
    private boolean checkCurrentAuthor;

    /**
     * @see #isCurrentUserChecked()
     */
    private boolean checkCurrentUser;

    /**
     * field for {@link #getFilters()}.
     */
    private List<QueryFilter> filters = new ArrayList<QueryFilter>();

    /**
     * field for {@link #getExecuter()}.
     */
    private transient QueryExecutor executer;

    /**
     * Create a Query.
     *
     * @param statement query statement
     * @param language query language
     * @param executor QueryExecutor component for execute the query.
     */
    public DefaultQuery(String statement, String language, QueryExecutor executor)
    {
        this.statement = statement;
        this.language = language;
        this.executer = executor;
        this.isNamed = false;
    }

    /**
     * Create a named Query.
     *
     * @param queryName the name of the query.
     * @param executor QueryExecutor component for execute the query.
     */
    public DefaultQuery(String queryName, QueryExecutor executor)
    {
        this.statement = queryName;
        this.executer = executor;
        this.isNamed = true;
    }

    @Override
    public String getStatement()
    {
        return this.statement;
    }

    @Override
    public String getLanguage()
    {
        return this.language;
    }

    @Override
    public boolean isNamed()
    {
        return this.isNamed;
    }

    @Override
    public Query setWiki(String wiki)
    {
        this.wiki = wiki;
        return this;
    }

    @Override
    public String getWiki()
    {
        return this.wiki;
    }

    @Override
    public Query bindValue(String var, Object val)
    {
        this.namedParameters.put(var, val);
        return this;
    }

    @Override
    public Query bindValue(int index, Object val)
    {
        this.positionalParameters.put(index, val);
        return this;
    }

    @Override
    public Query bindValues(List<Object> values)
    {
        for (int i = 0; i < values.size(); i++) {
            // There's a difference in the way positional parameters are handled:
            // - HQL (jdbc-like), the index of positional parameters must start at 0
            // - XWQL (jpql-like), the index of positional parameters must start at 1
            //
            // This difference is also hardcoded in HqlQueryExecutor#populateParameters(), a better solution could
            // be to replace the current DefaultQuery with distinct implementations: XwqlQuery, HqlQuery, NamedQuery.
            if (Query.HQL.equals(getLanguage())) {
                bindValue(i, values.get(i));
            } else {
                bindValue(i + 1, values.get(i));
            }
        }
        return this;
    }

    @Override
    public int getLimit()
    {
        return this.limit;
    }

    @Override
    public int getOffset()
    {
        return this.offset;
    }

    @Override
    public Query setLimit(int limit)
    {
        this.limit = limit;
        return this;
    }

    @Override
    public Query setOffset(int offset)
    {
        this.offset = offset;
        return this;
    }

    @Override
    public boolean isCurrentAuthorChecked()
    {
        return this.checkCurrentAuthor;
    }

    @Override
    public SecureQuery checkCurrentAuthor(boolean checkCurrentAuthor)
    {
        this.checkCurrentAuthor = checkCurrentAuthor;

        return this;
    }

    @Override
    public boolean isCurrentUserChecked()
    {
        return this.checkCurrentUser;
    }

    @Override
    public SecureQuery checkCurrentUser(boolean checkUser)
    {
        this.checkCurrentUser = checkUser;

        return this;
    }

    @Override
    public Map<String, Object> getNamedParameters()
    {
        return this.namedParameters;
    }

    @Override
    public Map<Integer, Object> getPositionalParameters()
    {
        return this.positionalParameters;
    }

    @Override
    public List<QueryFilter> getFilters()
    {
        return this.filters;
    }

    @Override
    public Query addFilter(QueryFilter filter)
    {
        if (!this.filters.contains(filter)) {
            this.filters.add(filter);
        } else {
            LOGGER.warn("QueryFilter [{}] already added to the query [{}]", filter.toString(), this.getStatement());
        }

        return this;
    }

    @Override
    public <T> List<T> execute() throws QueryException
    {
        return getExecuter().execute(this);
    }

    /**
     * @return QueryExecutor interface for execute the query.
     */
    protected QueryExecutor getExecuter()
    {
        return this.executer;
    }

    @Override
    public String toString()
    {
        return getStatement();
    }
}
