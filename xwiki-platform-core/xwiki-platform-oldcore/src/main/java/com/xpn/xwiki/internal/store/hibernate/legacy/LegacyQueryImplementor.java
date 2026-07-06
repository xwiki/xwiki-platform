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
package com.xpn.xwiki.internal.store.hibernate.legacy;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryImplementor;

import com.xpn.xwiki.internal.store.hibernate.QueryImplementorDelegate;

/**
 * Wrap a QueryImplementor to convert the positional parameters from legacy 0-based Hibernate parameters to 1-based JPA
 * parameters.
 *
 * @param <R> query result type
 * @version $Id$
 * @since 11.5RC1
 * @deprecated
 */
@Deprecated
public class LegacyQueryImplementor<R> extends QueryImplementorDelegate<R>
{
    /**
     * @param delegate the actual query
     */
    public LegacyQueryImplementor(QueryImplementor<R> delegate)
    {
        super(delegate);
    }

    @Override
    public QueryImplementor<R> setParameter(int position, Object value)
    {
        return super.setParameter(position + 1, value);
    }

    @Override
    public <P> QueryImplementor<R> setParameter(int position, P value, Class<P> type)
    {
        return super.setParameter(position + 1, value, type);
    }

    @Override
    public <P> QueryImplementor<R> setParameter(int position, P value, BindableType<P> type)
    {
        return super.setParameter(position + 1, value, type);
    }

    @Override
    public QueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public QueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public QueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public QueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values)
    {
        return super.setParameterList(position + 1, values);
    }

    @Override
    public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType)
    {
        return super.setParameterList(position + 1, values, javaType);
    }

    @Override
    public <P> QueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type)
    {
        return super.setParameterList(position + 1, values, type);
    }

    @Override
    public QueryImplementor<R> setParameterList(int position, Object[] values)
    {
        return super.setParameterList(position + 1, values);
    }

    @Override
    public <P> QueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType)
    {
        return super.setParameterList(position + 1, values, javaType);
    }

    @Override
    public <P> QueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type)
    {
        return super.setParameterList(position + 1, values, type);
    }

    @Override
    public Parameter<?> getParameter(int position)
    {
        return super.getParameter(position + 1);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type)
    {
        return super.getParameter(position + 1, type);
    }

    @Override
    public Object getParameterValue(int position)
    {
        return super.getParameterValue(position + 1);
    }
}
