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
package com.xpn.xwiki.internal.store.hibernate;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryProducerImplementor;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * Wrap a QueryImplementor.
 * 
 * @param <R> query result type
 * @version $Id$
 * @since 11.2RC1
 */
public class QueryImplementorDelegate<R> implements QueryImplementor<R>
{
    private final QueryImplementor<R> delegate;

    /**
     * @param delegate the actual query
     */
    public QueryImplementorDelegate(QueryImplementor<R> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public Optional<R> uniqueResultOptional()
    {
        return this.delegate.uniqueResultOptional();
    }

    @Override
    public Stream<R> stream()
    {
        return this.delegate.stream();
    }

    @Override
    public Query<R> applyGraph(RootGraph graph, GraphSemantic semantic)
    {
        return this.delegate.applyGraph(graph, semantic);
    }

    @Override
    public Query<R> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(Parameter<LocalDateTime> param, LocalDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(Parameter<ZonedDateTime> param, ZonedDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(Parameter<OffsetDateTime> param, OffsetDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, Instant value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, LocalDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, ZonedDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, OffsetDateTime value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Instant value, TemporalType temporalType)
    {
        // Convert from legacy Hibernate positional parameter to JPA positional parameter
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, LocalDateTime value, TemporalType temporalType)
    {
        // Convert from legacy Hibernate positional parameter to JPA positional parameter
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType)
    {
        // Convert from legacy Hibernate positional parameter to JPA positional parameter
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType)
    {
        // Convert from legacy Hibernate positional parameter to JPA positional parameter
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public ScrollableResults scroll()
    {
        return this.delegate.scroll();
    }

    @Override
    public ScrollableResults scroll(ScrollMode scrollMode)
    {
        return this.delegate.scroll(scrollMode);
    }

    @Override
    public List<R> list()
    {
        return this.delegate.list();
    }

    @Override
    public R uniqueResult()
    {
        return this.delegate.uniqueResult();
    }

    @Override
    public FlushMode getHibernateFlushMode()
    {
        return this.delegate.getHibernateFlushMode();
    }

    @Override
    public CacheMode getCacheMode()
    {
        return this.delegate.getCacheMode();
    }

    @Override
    public String getCacheRegion()
    {
        return this.delegate.getCacheRegion();
    }

    @Override
    public Integer getFetchSize()
    {
        return this.delegate.getFetchSize();
    }

    @Override
    public LockOptions getLockOptions()
    {
        return this.delegate.getLockOptions();
    }

    @Override
    public String getComment()
    {
        return this.delegate.getComment();
    }

    @Override
    public String getQueryString()
    {
        return this.delegate.getQueryString();
    }

    @Override
    public ParameterMetadata getParameterMetadata()
    {
        return this.delegate.getParameterMetadata();
    }

    @Override
    public Query<R> setMaxResults(int maxResult)
    {
        return this.delegate.setMaxResults(maxResult);
    }

    @Override
    public Query<R> setFirstResult(int startPosition)
    {
        return this.delegate.setFirstResult(startPosition);
    }

    @Override
    public Query<R> setHint(String hintName, Object value)
    {
        return this.delegate.setHint(hintName, value);
    }

    @Override
    public <T> Query<R> setParameter(Parameter<T> param, T value)
    {
        return this.delegate.setParameter(param, value);
    }

    @Override
    public Query<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType)
    {
        return this.delegate.setParameter(param, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, Object value)
    {
        return this.delegate.setParameter(name, value);
    }

    @Override
    public Query<R> setParameter(String name, Object value, Type type)
    {
        return this.delegate.setParameter(name, value, type);
    }

    @Override
    public Query<R> setParameter(String name, Calendar value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, Date value, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Object value)
    {
        return this.delegate.setParameter(position, value);
    }

    @Override
    public Query<R> setParameter(int position, Calendar value, TemporalType temporalType)
    {
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public Query<R> setParameter(int position, Date value, TemporalType temporalType)
    {
        return this.delegate.setParameter(position, value, temporalType);
    }

    @Override
    public <T> Query<R> setParameter(QueryParameter<T> parameter, T val)
    {
        return this.delegate.setParameter(parameter, val);
    }

    @Override
    public Query<R> setParameter(int position, Object val, TemporalType temporalType)
    {
        return this.delegate.setParameter(position, val, temporalType);
    }

    @Override
    public <T> Query<R> setParameter(QueryParameter<T> parameter, T val, Type type)
    {
        return this.delegate.setParameter(parameter, val, type);
    }

    @Override
    public Query<R> setParameter(int position, Object val, Type type)
    {
        return this.delegate.setParameter(position, val, type);
    }

    @Override
    public <T> Query<R> setParameter(QueryParameter<T> parameter, T val, TemporalType temporalType)
    {
        return this.delegate.setParameter(parameter, val, temporalType);
    }

    @Override
    public Query<R> setParameter(String name, Object val, TemporalType temporalType)
    {
        return this.delegate.setParameter(name, val, temporalType);
    }

    @Override
    public Query<R> setFlushMode(FlushModeType flushMode)
    {
        return this.delegate.setFlushMode(flushMode);
    }

    @Override
    public Query<R> setLockMode(LockModeType lockMode)
    {
        return this.delegate.setLockMode(lockMode);
    }

    @Override
    public Query<R> setReadOnly(boolean readOnly)
    {
        return this.delegate.setReadOnly(readOnly);
    }

    @Override
    public Query<R> setHibernateFlushMode(FlushMode flushMode)
    {
        return this.delegate.setHibernateFlushMode(flushMode);
    }

    @Override
    public Query<R> setCacheMode(CacheMode cacheMode)
    {
        return this.delegate.setCacheMode(cacheMode);
    }

    @Override
    public Query<R> setCacheable(boolean cacheable)
    {
        return this.delegate.setCacheable(cacheable);
    }

    @Override
    public Query<R> setCacheRegion(String cacheRegion)
    {
        return this.delegate.setCacheRegion(cacheRegion);
    }

    @Override
    public Query<R> setTimeout(int timeout)
    {
        return this.delegate.setTimeout(timeout);
    }

    @Override
    public Query<R> setFetchSize(int fetchSize)
    {
        return this.delegate.setFetchSize(fetchSize);
    }

    @Override
    public Query<R> setLockOptions(LockOptions lockOptions)
    {
        return this.delegate.setLockOptions(lockOptions);
    }

    @Override
    public Query<R> setLockMode(String alias, LockMode lockMode)
    {
        return this.delegate.setLockMode(alias, lockMode);
    }

    @Override
    public Query<R> setComment(String comment)
    {
        return this.delegate.setComment(comment);
    }

    @Override
    public Query<R> addQueryHint(String hint)
    {
        return this.delegate.addQueryHint(hint);
    }

    @Override
    public <T> Query<R> setParameterList(QueryParameter<T> parameter, Collection<T> values)
    {
        return this.delegate.setParameterList(parameter, values);
    }

    @Override
    public Query<R> setParameterList(String name, Collection values)
    {
        return this.delegate.setParameterList(name, values);
    }

    @Override
    public Query<R> setParameterList(String name, Collection values, Type type)
    {
        return this.delegate.setParameterList(name, values, type);
    }

    @Override
    public Query<R> setParameterList(String name, Object[] values, Type type)
    {
        return this.delegate.setParameterList(name, values, type);
    }

    @Override
    public Query<R> setParameterList(String name, Object[] values)
    {
        return this.delegate.setParameterList(name, values);
    }

    @Override
    public Query<R> setProperties(Object bean)
    {
        return this.delegate.setProperties(bean);
    }

    @Override
    public Query<R> setProperties(Map bean)
    {
        return this.delegate.setProperties(bean);
    }

    @Override
    @Deprecated
    public Query<R> setEntity(int position, Object val)
    {
        return this.delegate.setEntity(position, val);
    }

    @Override
    @Deprecated
    public Query<R> setEntity(String name, Object val)
    {
        return this.delegate.setEntity(name, val);
    }

    @Override
    @Deprecated
    public Query<R> setResultTransformer(ResultTransformer transformer)
    {
        return this.delegate.setResultTransformer(transformer);
    }

    @Override
    public int executeUpdate()
    {
        return this.delegate.executeUpdate();
    }

    @Override
    public int getMaxResults()
    {
        return this.delegate.getMaxResults();
    }

    @Override
    public int getFirstResult()
    {
        return this.delegate.getFirstResult();
    }

    @Override
    public Map<String, Object> getHints()
    {
        return this.delegate.getHints();
    }

    @Override
    public Set<Parameter<?>> getParameters()
    {
        return this.delegate.getParameters();
    }

    @Override
    public Parameter<?> getParameter(String name)
    {
        return this.delegate.getParameter(name);
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type)
    {
        return this.delegate.getParameter(name, type);
    }

    @Override
    public Parameter<?> getParameter(int position)
    {
        return this.delegate.getParameter(position);
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type)
    {
        return this.delegate.getParameter(position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param)
    {
        return this.delegate.isBound(param);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param)
    {
        return this.delegate.getParameterValue(param);
    }

    @Override
    public Object getParameterValue(String name)
    {
        return this.delegate.getParameterValue(name);
    }

    @Override
    public Object getParameterValue(int position)
    {
        return this.delegate.getParameterValue(position);
    }

    @Override
    @Deprecated
    public FlushModeType getFlushMode()
    {
        return this.delegate.getFlushMode();
    }

    @Override
    public LockModeType getLockMode()
    {
        return this.delegate.getLockMode();
    }

    @Override
    public <T> T unwrap(Class<T> cls)
    {
        return this.delegate.unwrap(cls);
    }

    @Override
    @Deprecated
    public RowSelection getQueryOptions()
    {
        return this.delegate.getQueryOptions();
    }

    @Override
    @Deprecated
    public boolean isCacheable()
    {
        return this.delegate.isCacheable();
    }

    @Override
    @Deprecated
    public Integer getTimeout()
    {
        return this.delegate.getTimeout();
    }

    @Override
    @Deprecated
    public boolean isReadOnly()
    {
        return this.delegate.isReadOnly();
    }

    @Override
    @Deprecated
    public Type[] getReturnTypes()
    {
        return this.delegate.getReturnTypes();
    }

    @Override
    @Deprecated
    public Iterator<R> iterate()
    {
        return this.delegate.iterate();
    }

    @Override
    @Deprecated
    public String[] getNamedParameters()
    {
        return this.delegate.getNamedParameters();
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Collection values)
    {
        return this.delegate.setParameterList(position, values);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Collection values, Type type)
    {
        return this.delegate.setParameterList(position, values, type);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Object[] values, Type type)
    {
        return this.delegate.setParameterList(position, values, type);
    }

    @Override
    @Deprecated
    public org.hibernate.Query<R> setParameterList(int position, Object[] values)
    {
        return this.delegate.setParameterList(position, values);
    }

    @Override
    @Deprecated
    public Type determineProperBooleanType(int position, Object value, Type defaultType)
    {
        return this.delegate.determineProperBooleanType(position, value, defaultType);
    }

    @Override
    @Deprecated
    public Type determineProperBooleanType(String name, Object value, Type defaultType)
    {
        return this.delegate.determineProperBooleanType(name, value, defaultType);
    }

    @Override
    @Deprecated
    public String[] getReturnAliases()
    {
        return this.delegate.getReturnAliases();
    }

    @Override
    public QueryProducerImplementor getProducer()
    {
        return this.delegate.getProducer();
    }

    @Override
    public void setOptionalId(Serializable id)
    {
        this.delegate.setOptionalId(id);
    }

    @Override
    public void setOptionalEntityName(String entityName)
    {
        this.delegate.setOptionalEntityName(entityName);
    }

    @Override
    public void setOptionalObject(Object optionalObject)
    {
        this.delegate.setOptionalObject(optionalObject);
    }
}
