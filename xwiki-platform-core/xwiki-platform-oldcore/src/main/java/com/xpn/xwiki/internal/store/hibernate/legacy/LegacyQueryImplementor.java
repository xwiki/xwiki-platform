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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.type.Type;

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
    public Query<R> setParameter(int position, Calendar value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Date value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Instant value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, LocalDateTime value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Object val, TemporalType temporalType)
    {
        return super.setParameter(position + 1, val, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Object val, Type type)
    {
        return super.setParameter(position + 1, val, type);
    }

    @Override
    public Query<R> setParameter(int position, Object value)
    {
        return super.setParameter(position + 1, value);
    }

    @Override
    public Query<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType)
    {
        return super.setParameter(position + 1, value, temporalType);
    }

    @Override
    @Deprecated
    public Query<R> setBigDecimal(int position, BigDecimal val)
    {
        return super.setBigDecimal(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setBigInteger(int position, BigInteger val)
    {
        return super.setBigInteger(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setBinary(int position, byte[] val)
    {
        return super.setBinary(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setBoolean(int position, boolean val)
    {
        return super.setBoolean(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setByte(int position, byte val)
    {
        return super.setByte(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setCalendar(int position, Calendar val)
    {
        return super.setCalendar(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setCalendarDate(int position, Calendar val)
    {
        return super.setCalendarDate(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setCharacter(int position, char val)
    {
        return super.setCharacter(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setDate(int position, Date val)
    {
        return super.setDate(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setDouble(int position, double val)
    {
        return super.setDouble(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setEntity(int position, Object val)
    {
        return super.setEntity(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setFloat(int position, float val)
    {
        return super.setFloat(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setInteger(int position, int val)
    {
        return super.setInteger(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setLocale(int position, Locale val)
    {
        return super.setLocale(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setLong(int position, long val)
    {
        return super.setLong(position + 1, val);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Collection values)
    {
        return super.setParameterList(position + 1, values);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Collection values, Type type)
    {
        return super.setParameterList(position + 1, values, type);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Object[] values)
    {
        return super.setParameterList(position + 1, values);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Object[] values, Type type)
    {
        return super.setParameterList(position + 1, values, type);
    }

    @Override
    @Deprecated
    public Query<R> setSerializable(int position, Serializable val)
    {
        return super.setSerializable(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setShort(int position, short val)
    {
        return super.setShort(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setString(int position, String val)
    {
        return super.setString(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setText(int position, String val)
    {
        return super.setText(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setTime(int position, Date val)
    {
        return super.setTime(position + 1, val);
    }

    @Override
    @Deprecated
    public Query<R> setTimestamp(int position, Date val)
    {
        return super.setTimestamp(position + 1, val);
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

    @Override
    @Deprecated
    public Type determineProperBooleanType(int position, Object value, Type defaultType)
    {
        return super.determineProperBooleanType(position + 1, value, defaultType);
    }
}
