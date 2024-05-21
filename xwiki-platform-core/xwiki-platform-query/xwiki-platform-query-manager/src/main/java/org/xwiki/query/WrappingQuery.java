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
package org.xwiki.query;

import java.util.List;
import java.util.Map;

/**
 * Make it simple to change the behavior of a {@link Query} by extending this class and overriding only the behavior
 * you wish to alter.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class WrappingQuery implements Query
{
    private Query wrappedQuery;

    /**
     * @param wrappedQuery the query being wrapped
     */
    public WrappingQuery(Query wrappedQuery)
    {
        this.wrappedQuery = wrappedQuery;
    }

    /**
     * @return the wrapped query
     */
    public Query getWrappedQuery()
    {
        return this.wrappedQuery;
    }

    @Override
    public String getStatement()
    {
        return getWrappedQuery().getStatement();
    }

    @Override
    public String getLanguage()
    {
        return getWrappedQuery().getLanguage();
    }

    @Override
    public boolean isNamed()
    {
        return getWrappedQuery().isNamed();
    }

    @Override
    public Query setWiki(String wiki)
    {
        return getWrappedQuery().setWiki(wiki);
    }

    @Override
    public String getWiki()
    {
        return getWrappedQuery().getWiki();
    }

    @Override
    public Query bindValue(String variable, Object val)
    {
        getWrappedQuery().bindValue(variable, val);
        return this;
    }

    @Override
    public Query bindValue(int index, Object val)
    {
        getWrappedQuery().bindValue(index, val);
        return this;
    }

    @Override
    public Query bindValues(List<Object> values)
    {
        getWrappedQuery().bindValues(values);
        return this;
    }

    @Override
    public Query bindValues(Map<String, ?> values)
    {
        getWrappedQuery().bindValues(values);
        return this;
    }

    @Override
    public Map<String, Object> getNamedParameters()
    {
        return getWrappedQuery().getNamedParameters();
    }

    @Override
    public QueryParameter bindValue(String variable)
    {
        return getWrappedQuery().bindValue(variable);
    }

    @Override
    public Map<Integer, Object> getPositionalParameters()
    {
        return getWrappedQuery().getPositionalParameters();
    }

    @Override
    public Query addFilter(QueryFilter filter)
    {
        return getWrappedQuery().addFilter(filter);
    }

    @Override
    public List<QueryFilter> getFilters()
    {
        return getWrappedQuery().getFilters();
    }

    @Override
    public Query setLimit(int limit)
    {
        return getWrappedQuery().setLimit(limit);
    }

    @Override
    public Query setOffset(int offset)
    {
        return getWrappedQuery().setOffset(offset);
    }

    @Override
    public int getLimit()
    {
        return getWrappedQuery().getLimit();
    }

    @Override
    public int getOffset()
    {
        return getWrappedQuery().getOffset();
    }

    @Override
    public <T> List<T> execute() throws QueryException
    {
        return getWrappedQuery().execute();
    }
}
