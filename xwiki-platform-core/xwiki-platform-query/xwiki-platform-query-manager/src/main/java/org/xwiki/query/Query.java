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
 * This is a Query interface, representing all queries in various languages for various stores.
 *
 * <p>Note that it was modeled after the JCR Query interface.</p>
 *
 * @version $Id$
 * @since 1.6M1
 */
public interface Query
{
    /**
     * Indicator for Hibernate Query Language.
     */
    String HQL = "hql";

    /**
     * Indicator for XPath language.
     */
    String XPATH = "xpath";

    /**
     * Indicator for XWiki Query Language.
     */
    String XWQL = "xwql";

    /**
     * @return Query statement or query name depends on {@link #isNamed()}
     */
    String getStatement();

    /**
     * @return Query language. See {@link Query#HQL} and others.
     */
    String getLanguage();

    /**
     * if the query is named, then {@link #getStatement()} returns a name of the query, else - a query statement.
     *
     * @return is the query named.
     */
    boolean isNamed();

    /**
     * @param wiki virtual wiki to run the query. null is a current wiki.
     * @return this query
     */
    Query setWiki(String wiki);

    /**
     * @return virtual wiki to run the query. null is a current wiki.
     * @see #setWiki(String)
     */
    String getWiki();

    /**
     * Bind named parameter var with value val in query statement.
     *
     * @param variable variable in query statement (:var).
     * @param val value of the variable.
     * @return this query
     */
    Query bindValue(String variable, Object val);

    /**
     * Bind a positional parameter present in the statement (?index in XWQL) with a value. It is recommended to use
     * named parameters if it acceptable, see {@link #bindValue(String, Object)}.
     *
     * @param index index of positional parameter. Index starting number depends on the query language. According to the
     * JPQL standard index should start from 1.
     * @param val value of the variable.
     * @return this query
     */
    Query bindValue(int index, Object val);

    /**
     * Bind a list of positional parameters values. This method is a convenience method allowing passing a list of
     * values in one call instead of multiple calls to {@link #bindValue(int, Object)}.
     *
     * @param values list of positional parameters values.
     * @return this query
     * @see #bindValue(int, Object)
     */
    Query bindValues(List<Object> values);

    /**
     * Bind a map of named parameters values. This method is a convenience method allowing passing a map of values in
     * one call instead of multiple calls to {@link #bindValue(String, Object)}.
     *
     * @param values list of positional parameters values.
     * @return this query
     * @see #bindValue(String, Object)
     * @since 11.5RC1
     */
    default Query bindValues(Map<String, ?> values)
    {
        values.forEach(this::bindValue);
        return this;
    }

    /**
     * Bind named parameter variable with a value that will be constructed using calls to
     * {@link QueryParameter#literal(String)}, {@link QueryParameter#anychar()} and {@link QueryParameter#anyChars()}.
     * In order to perserve the fluent API, it's also possible to call {@link QueryParameter#query()} to get back the
     * {@link Query}.
     *
     * @param variable the variable in the query statement ({@code :variable}).
     * @return an empty {@link QueryParameter} that needs to be populated by calling
     *         {@link QueryParameter#literal(String)}, {@link QueryParameter#anychar()} and
     *         {@link QueryParameter#anyChars()}
     * @since 8.4.5
     * @since 9.3RC1
     */
    default QueryParameter bindValue(String variable)
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @return map from parameter name to value.
     * @see #bindValue(String, Object)
     */
    Map<String, Object> getNamedParameters();

    /**
     * @return list of positional parameters values.
     * @see #bindValue(int, Object)
     */
    Map<Integer, Object> getPositionalParameters();

    /**
     * @param filter the {@link QueryFilter} to add to this query
     * @return this query
     */
    Query addFilter(QueryFilter filter);

    /**
     * @return the list of {@link QueryFilter}s that will be applied to this query
     */
    List<QueryFilter> getFilters();

    /**
     * @param limit see {@link #getLimit()}
     * @return this query
     */
    Query setLimit(int limit);

    /**
     * @param offset offset of query result to set (skip first "offset" rows)
     * @return this query
     */
    Query setOffset(int offset);

    /**
     * @return limit the limit of result list to set ({@code execute().size() <= limit})
     * @see #setLimit(int)
     */
    int getLimit();

    /**
     * @return offset offset of query result.
     * @see #setOffset(int)
     */
    int getOffset();

    /**
     * @param <T> expected type of elements in the result list.
     * @return result list of the query. If several fields are selected then T=Object[].
     * @throws QueryException if something goes wrong.
     */
    <T> List<T> execute() throws QueryException;
}
