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
package com.xpn.xwiki.store.query;

import java.util.List;

import com.xpn.xwiki.XWikiException;

/**
 * This is a Query interface, representing all queries in various languages for various stores.
 * @see javax.jcr.query.Query
 * @version $Id$
 * @since 1.6M1   
 */
public interface Query
{
    /**
     * Indicator for Hibernate Query Language.
     */
    String HQL   = "hql";

    /**
     * Indicator for XPath language.
     */
    String XPATH = "xpath";

    /**
     * @return Query statement.
     */
    String getStatement();

    /**
     * @return Query language. See {@link Query#HQL} and others.
     */
    String getLanguage();

    /**
     * @param wiki virtual wiki to run the query. null is current wiki.
     */
    void setWiki(String wiki);

    /**
     * Bind named parameter var with value val in query statement.
     * @param var variable in query statement (:var).
     * @param val value of the variable.
     * @return this query.
     */
    Query bindValue(String var, Object val);

    /**
     * @param limit limit of result list to set (so {@link #execute()}.size() will be <= limit).
     * @return this query.
     */
    Query setLimit(int limit);

    /**
     * @param offset offset of query result to set (skip first "offset" rows).
     * @return this query.
     */
    Query setOffset(int offset);
    
    /**
     * @param <T> expected type of elements in result list.
     * @return result of the query. If several fields selected then T=Object[].
     * @throws XWikiException if something wrong.
     */
    <T> List<T> execute() throws XWikiException;
}
