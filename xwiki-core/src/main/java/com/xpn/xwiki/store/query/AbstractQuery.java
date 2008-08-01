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

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractQuery stores all information needed for execute a query.
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractQuery implements Query
{
    /** 
     * field for {@link Query#getStatement()}.
     */
    private String statement;

    /**
     * field for {@link Query#getLanguage()}.
     */
    private String language;

    /**
     * map from query parameters to values.
     */
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     * field for {@link Query#setLimit(int)}.
     */
    private int limit;

    /**
     * field for {@link Query#setOffset(int)}.
     */
    private int offset;

    /**
     * @param statement query statement
     * @param language query language
     */
    public AbstractQuery(String statement, String language)
    {
        this.statement = statement;
        this.language = language;
    }

    /**
     * {@inheritDoc}
     */
    public String getStatement()
    {
        return statement;
    }

    /**
     * {@inheritDoc}
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * {@inheritDoc}
     */
    public Query bindValue(String var, Object val)
    {
        parameters.put(var, val);
        return this;
    }

    /**
     * @return limit of result list
     * @see Query#setLimit(int)
     */
    protected int getLimit()
    {
        return limit;
    }

    /**
     * @return offset of query result
     * @see Query#setOffset(int)
     */
    protected int getOffset()
    {
        return offset;
    }

    /**
     * {@inheritDoc}
     */
    public Query setLimit(int limit)
    {
        this.limit = limit;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query setOffset(int offset)
    {
        this.offset = offset;
        return this;
    }

    /**
     * @return map from query parameters to values.
     */
    protected Map<String, Object> getParameters()
    {
        return parameters;
    }
}
