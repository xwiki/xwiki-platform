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

import org.xwiki.query.Query;
import org.xwiki.query.QueryParameter;

/**
 * Wraps a {@link QueryParameter} to return an object that can be used in scripts.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class ScriptQueryParameter implements QueryParameter
{
    private ScriptQuery scriptQuery;

    private QueryParameter parameter;

    /**
     * @param scriptQuery the associated Script Query
     * @param parameter the wrapped query parameter
     */
    public ScriptQueryParameter(ScriptQuery scriptQuery, QueryParameter parameter)
    {
        this.scriptQuery = scriptQuery;
        this.parameter = parameter;
    }

    @Override
    public QueryParameter literal(String literal)
    {
        this.parameter.literal(literal);
        return this;
    }

    @Override
    public QueryParameter like(String like)
    {
        this.parameter.like(like);
        return this;
    }

    @Override
    public QueryParameter anychar()
    {
        this.parameter.anychar();
        return this;
    }

    @Override
    public QueryParameter anyChars()
    {
        this.parameter.anyChars();
        return this;
    }

    @Override
    public Query query()
    {
        return this.scriptQuery;
    }
}
