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
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.query.Query;
import org.xwiki.query.QueryParameter;

/**
 * Default implementation for {@link QueryParameter}.
 *
 * @version $Id$
 * @since 8.4.5
 * @since 9.3RC1
 */
public class DefaultQueryParameter implements QueryParameter
{
    private List<ParameterPart> parts;

    private Query query;

    /**
     * @param query the associated Query, used to return it when {@link #query()} is called, to keep the API fluent
     */
    public DefaultQueryParameter(Query query)
    {
        this.parts = new ArrayList<>();
        setQuery(query);
    }

    /**
     * @return the various parts for the parameter, see {@link ParameterPart}
     */
    public List<ParameterPart> getParts()
    {
        return this.parts;
    }

    /**
     * @param query the associated query to set. Used to override the query defined by the constructor, when needed
     */
    public void setQuery(Query query)
    {
        this.query = query;
    }

    @Override
    public QueryParameter literal(String literal)
    {
        this.parts.add(new LiteralParameterPart(literal));
        return this;
    }

    @Override
    public QueryParameter like(String like)
    {
        this.parts.add(new LikeParameterPart(like));
        return this;
    }

    @Override
    public QueryParameter anychar()
    {
        this.parts.add(new AnyCharParameterPart());
        return this;
    }

    @Override
    public QueryParameter anyChars()
    {
        this.parts.add(new AnyCharsParameterPart());
        return this;
    }

    @Override
    public Query query()
    {
        return this.query;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultQueryParameter that = (DefaultQueryParameter) o;

        return new EqualsBuilder().append(parts, that.parts).append(query, that.query)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 95).append(parts).append(query).toHashCode();
    }
}
