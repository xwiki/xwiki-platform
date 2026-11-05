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
package org.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.hibernate.query.ParameterMetadata;
import org.hibernate.type.Type;
import org.xwiki.store.hibernate.internal.legacy.LegacyQueries;

/**
 * Hibernate 5 legacy query type, restored so that code compiled against Hibernate 5 keeps working.
 * <p>
 * {@link org.hibernate.query.Query} extends it in the patched Hibernate.
 *
 * @param <R> the query result type
 * @version $Id$
 * @since 18.10.0RC1
 * @deprecated use {@link org.hibernate.query.Query} instead
 */
@Deprecated(since = "18.10.0RC1")
public interface Query<R> extends TypedQuery<R>
{
    private org.hibernate.query.Query<R> hibernateQuery()
    {
        return (org.hibernate.query.Query<R>) this;
    }

    /**
     * @return the query string
     */
    String getQueryString();

    /**
     * @return the query results as a list
     */
    List<R> list();

    /**
     * @return the single result or {@code null}
     */
    R uniqueResult();

    /**
     * @return the results as {@link ScrollableResults}
     */
    ScrollableResults<R> scroll();

    /**
     * @param scrollMode the scroll mode
     * @return the results as {@link ScrollableResults}
     */
    ScrollableResults<R> scroll(ScrollMode scrollMode);

    /**
     * @return the parameters metadata
     */
    ParameterMetadata getParameterMetadata();

    /**
     * @return the flush mode
     */
    FlushMode getHibernateFlushMode();

    /**
     * @return the cache mode
     */
    CacheMode getCacheMode();

    /**
     * @return the cache region
     */
    String getCacheRegion();

    /**
     * @return true if the query results are cacheable
     */
    boolean isCacheable();

    /**
     * @return the timeout
     */
    Integer getTimeout();

    /**
     * @return the fetch size
     */
    Integer getFetchSize();

    /**
     * @return true if the entities and proxies loaded by the query are read-only
     */
    boolean isReadOnly();

    /**
     * @return the lock options
     */
    LockOptions getLockOptions();

    /**
     * @return the comment
     */
    String getComment();

    @Override
    default Query<R> setParameter(String name, Object value)
    {
        hibernateQuery().setParameter(name, value);

        return this;
    }

    @Override
    default Query<R> setParameter(int position, Object value)
    {
        hibernateQuery().setParameter(position, value);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     */
    default Query<R> setParameter(String name, Object value, Type type)
    {
        LegacyQueries.setParameter(hibernateQuery(), name, value, type);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     */
    default Query<R> setParameter(int position, Object value, Type type)
    {
        LegacyQueries.setParameter(hibernateQuery(), position, value, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default Query<R> setParameterList(String name, Collection values)
    {
        hibernateQuery().setParameterList(name, values);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default Query<R> setParameterList(String name, Collection values, Type type)
    {
        LegacyQueries.setParameterList(hibernateQuery(), name, values, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @return this query
     */
    default Query<R> setParameterList(String name, Object[] values)
    {
        hibernateQuery().setParameterList(name, values);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     */
    default Query<R> setParameterList(String name, Object[] values, Type type)
    {
        LegacyQueries.setParameterList(hibernateQuery(), name, List.of(values), type);

        return this;
    }

    /**
     * @param values the parameter values
     * @param types the Hibernate types of the parameters
     * @return this query
     */
    default Query<R> setParameters(Object[] values, Type[] types)
    {
        for (int i = 0; i < values.length; i++) {
            setParameter(i, values[i], types[i]);
        }

        return this;
    }

    /**
     * @param bean the bean holding the parameter values
     * @return this query
     */
    default Query<R> setProperties(Object bean)
    {
        hibernateQuery().setProperties(bean);

        return this;
    }

    /**
     * @param bean the map holding the parameter values
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default Query<R> setProperties(Map bean)
    {
        hibernateQuery().setProperties(bean);

        return this;
    }

    @Override
    default Query<R> setFirstResult(int startPosition)
    {
        hibernateQuery().setFirstResult(startPosition);

        return this;
    }

    @Override
    default Query<R> setMaxResults(int maxResult)
    {
        hibernateQuery().setMaxResults(maxResult);

        return this;
    }

    /**
     * @param flushMode the flush mode
     * @return this query
     */
    default Query<R> setHibernateFlushMode(FlushMode flushMode)
    {
        hibernateQuery().setHibernateFlushMode(flushMode);

        return this;
    }

    /**
     * @param flushMode the flush mode
     * @return this query
     */
    default Query<R> setFlushMode(FlushMode flushMode)
    {
        return setHibernateFlushMode(flushMode);
    }

    /**
     * @param cacheMode the cache mode
     * @return this query
     */
    default Query<R> setCacheMode(CacheMode cacheMode)
    {
        hibernateQuery().setCacheMode(cacheMode);

        return this;
    }

    /**
     * @param cacheable true if the query results are cacheable
     * @return this query
     */
    default Query<R> setCacheable(boolean cacheable)
    {
        hibernateQuery().setCacheable(cacheable);

        return this;
    }

    /**
     * @param cacheRegion the cache region
     * @return this query
     */
    default Query<R> setCacheRegion(String cacheRegion)
    {
        hibernateQuery().setCacheRegion(cacheRegion);

        return this;
    }

    /**
     * @param timeout the timeout
     * @return this query
     */
    default Query<R> setTimeout(int timeout)
    {
        hibernateQuery().setTimeout(timeout);

        return this;
    }

    /**
     * @param fetchSize the fetch size
     * @return this query
     */
    default Query<R> setFetchSize(int fetchSize)
    {
        hibernateQuery().setFetchSize(fetchSize);

        return this;
    }

    /**
     * @param readOnly true if the entities and proxies loaded by the query should be read-only
     * @return this query
     */
    default Query<R> setReadOnly(boolean readOnly)
    {
        hibernateQuery().setReadOnly(readOnly);

        return this;
    }

    /**
     * @param lockOptions the lock options
     * @return this query
     */
    default Query<R> setLockOptions(LockOptions lockOptions)
    {
        hibernateQuery().setLockOptions(lockOptions);

        return this;
    }

    /**
     * @param alias the alias
     * @param lockMode the lock mode
     * @return this query
     */
    default Query<R> setLockMode(String alias, LockMode lockMode)
    {
        ((org.hibernate.query.SelectionQuery<R>) hibernateQuery()).setLockMode(alias, lockMode);

        return this;
    }

    /**
     * @param comment the comment
     * @return this query
     */
    default Query<R> setComment(String comment)
    {
        hibernateQuery().setComment(comment);

        return this;
    }

    /**
     * @param hint the hint
     * @return this query
     */
    default Query<R> addQueryHint(String hint)
    {
        hibernateQuery().addQueryHint(hint);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the entity
     * @return this query
     */
    default Query<R> setEntity(int position, Object value)
    {
        return setParameter(position, value);
    }

    /**
     * @param name the parameter name
     * @param value the entity
     * @return this query
     */
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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value);

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
        hibernateQuery().setParameter(name, value);

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
        hibernateQuery().setParameter(position, value, TemporalType.DATE);

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
        hibernateQuery().setParameter(name, value, TemporalType.DATE);

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
        hibernateQuery().setParameter(position, value, TemporalType.TIME);

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
        hibernateQuery().setParameter(name, value, TemporalType.TIME);

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
        hibernateQuery().setParameter(position, value, TemporalType.TIMESTAMP);

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
        hibernateQuery().setParameter(name, value, TemporalType.TIMESTAMP);

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
        hibernateQuery().setParameter(position, value, TemporalType.TIMESTAMP);

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
        hibernateQuery().setParameter(name, value, TemporalType.TIMESTAMP);

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
        hibernateQuery().setParameter(position, value, TemporalType.DATE);

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
        hibernateQuery().setParameter(name, value, TemporalType.DATE);

        return this;
    }

}
