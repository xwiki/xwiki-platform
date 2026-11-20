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
package org.xwiki.store.hibernate.internal.mixin;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.TemporalType;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.type.Type;
import org.xwiki.store.hibernate.internal.legacy.LegacyQueries;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link Query} APIs restored in the patched Hibernate.
 * <p>
 * The type parameter must have the same name as the one of {@link Query}.
 *
 * @param <R> the query result type
 * @version $Id$
 */
@Mixin(interfaces = "Lorg/hibernate/Query<TR;>;")
@SuppressWarnings({"rawtypes", "unchecked", "deprecation", "removal"})
public interface QueryMixin<R> extends Query<R>
{
    /**
     * @param name the parameter name
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     * @deprecated use {@link #setParameter(String, Object, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default Query<R> setParameter(String name, Object value, Type type)
    {
        LegacyQueries.setParameter(this, name, value, type);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     * @deprecated use {@link #setParameter(int, Object, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default Query<R> setParameter(int position, Object value, Type type)
    {
        LegacyQueries.setParameter(this, position, value, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     * @deprecated use {@link #setParameterList(String, Collection, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default Query<R> setParameterList(String name, Collection values, Type type)
    {
        LegacyQueries.setParameterList(this, name, values, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     * @deprecated use {@link #setParameterList(String, Object[], jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default Query<R> setParameterList(String name, Object[] values, Type type)
    {
        LegacyQueries.setParameterList(this, name, List.of(values), type);

        return this;
    }

    /**
     * @param values the parameter values
     * @param types the Hibernate types of the parameters
     * @return this query
     * @deprecated use {@link #setParameter(int, Object)} instead
     */
    @Deprecated
    default Query<R> setParameters(Object[] values, Type[] types)
    {
        for (int i = 0; i < values.length; i++) {
            setParameter(i, values[i], types[i]);
        }

        return this;
    }

    /**
     * @param flushMode the flush mode
     * @return this query
     * @deprecated use {@link #setHibernateFlushMode(FlushMode)} instead
     */
    @Deprecated
    default Query<R> setFlushMode(FlushMode flushMode)
    {
        return setHibernateFlushMode(flushMode);
    }

    /**
     * @param alias the alias
     * @param lockMode the lock mode
     * @return this query
     */
    default Query<R> setLockMode(String alias, LockMode lockMode)
    {
        ((SelectionQuery<R>) this).setLockMode(alias, lockMode);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the entity
     * @return this query
     * @deprecated use {@link #setParameter(int, Object)} instead
     */
    @Deprecated
    default Query<R> setEntity(int position, Object value)
    {
        return setParameter(position, value);
    }

    /**
     * @param name the parameter name
     * @param value the entity
     * @return this query
     * @deprecated use {@link #setParameter(String, Object)} instead
     */
    @Deprecated
    default Query<R> setEntity(String name, Object value)
    {
        return setParameter(name, value);
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setString(int position, String value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setString(String name, String value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCharacter(int position, char value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCharacter(String name, char value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBoolean(int position, boolean value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBoolean(String name, boolean value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setByte(int position, byte value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setByte(String name, byte value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setShort(int position, short value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setShort(String name, short value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setInteger(int position, int value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setInteger(String name, int value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setLong(int position, long value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setLong(String name, long value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setFloat(int position, float value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setFloat(String name, float value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setDouble(int position, double value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setDouble(String name, double value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBinary(int position, byte[] value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBinary(String name, byte[] value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setText(int position, String value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setText(String name, String value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setSerializable(int position, Serializable value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setSerializable(String name, Serializable value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setLocale(int position, Locale value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setLocale(String name, Locale value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBigDecimal(int position, BigDecimal value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBigDecimal(String name, BigDecimal value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBigInteger(int position, BigInteger value)
    {
        this.setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setBigInteger(String name, BigInteger value)
    {
        this.setParameter(name, value);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setDate(int position, Date value)
    {
        this.setParameter(position, value, TemporalType.DATE);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setDate(String name, Date value)
    {
        this.setParameter(name, value, TemporalType.DATE);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setTime(int position, Date value)
    {
        this.setParameter(position, value, TemporalType.TIME);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setTime(String name, Date value)
    {
        this.setParameter(name, value, TemporalType.TIME);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setTimestamp(int position, Date value)
    {
        this.setParameter(position, value, TemporalType.TIMESTAMP);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setTimestamp(String name, Date value)
    {
        this.setParameter(name, value, TemporalType.TIMESTAMP);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCalendar(int position, Calendar value)
    {
        this.setParameter(position, value, TemporalType.TIMESTAMP);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCalendar(String name, Calendar value)
    {
        this.setParameter(name, value, TemporalType.TIMESTAMP);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCalendarDate(int position, Calendar value)
    {
        this.setParameter(position, value, TemporalType.DATE);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @return this query
     * @deprecated use {@code setParameter} instead
     */
    @Deprecated
    default Query<R> setCalendarDate(String name, Calendar value)
    {
        this.setParameter(name, value, TemporalType.DATE);

        return this;
    }

}
