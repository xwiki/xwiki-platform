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

import org.hibernate.query.spi.QueryImplementor;

/**
 * Wrap a {@link QueryImplementor}.
 *
 * @param <R> query result type
 * @version $Id$
 * @since 11.5RC1
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
    public org.hibernate.query.spi.QueryImplementor<R> setProperties(java.lang.Object arg0)
    {
        return this.delegate.setProperties(arg0);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setProperties(java.util.Map arg0)
    {
        return this.delegate.setProperties(arg0);
    }

    @Override
    public org.hibernate.engine.spi.SharedSessionContractImplementor getSession()
    {
        return this.delegate.getSession();
    }

    @Override
    public void setOptionalId(java.io.Serializable arg0)
    {
        this.delegate.setOptionalId(arg0);
    }

    @Override
    public void setOptionalEntityName(java.lang.String arg0)
    {
        this.delegate.setOptionalEntityName(arg0);
    }

    @Override
    public void setOptionalObject(java.lang.Object arg0)
    {
        this.delegate.setOptionalObject(arg0);
    }

    @Override
    public org.hibernate.query.spi.QueryParameterBindings getParameterBindings()
    {
        return this.delegate.getParameterBindings();
    }

    @Override
    public org.hibernate.query.spi.ScrollableResultsImplementor<R> scroll()
    {
        return this.delegate.scroll();
    }

    @Override
    public org.hibernate.query.spi.ScrollableResultsImplementor<R> scroll(org.hibernate.ScrollMode arg0)
    {
        return this.delegate.scroll(arg0);
    }

    @Override
    public <T> org.hibernate.query.spi.QueryImplementor<T> setTupleTransformer(org.hibernate.query.TupleTransformer<T> arg0)
    {
        return this.delegate.setTupleTransformer(arg0);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setResultListTransformer(org.hibernate.query.ResultListTransformer<R> arg0)
    {
        return this.delegate.setResultListTransformer(arg0);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(org.hibernate.query.QueryParameter<P> arg0, P arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(org.hibernate.query.QueryParameter<P> arg0, P arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <T> org.hibernate.query.spi.QueryImplementor<R> setParameter(org.hibernate.query.QueryParameter<T> arg0, T arg1)
    {
        return this.delegate.setParameter(arg0, arg1);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(jakarta.persistence.Parameter<java.util.Calendar> arg0, java.util.Calendar arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <T> org.hibernate.query.spi.QueryImplementor<R> setParameter(jakarta.persistence.Parameter<T> arg0, T arg1)
    {
        return this.delegate.setParameter(arg0, arg1);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(jakarta.persistence.Parameter<java.util.Date> arg0, java.util.Date arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, P arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, P arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, java.lang.Object arg1)
    {
        return this.delegate.setParameter(arg0, arg1);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, java.util.Date arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, java.util.Calendar arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, java.util.Date arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(int arg0, java.time.Instant arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, java.util.Calendar arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, java.time.Instant arg1, jakarta.persistence.TemporalType arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, P arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, P arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameter(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameter(java.lang.String arg0, java.lang.Object arg1)
    {
        return this.delegate.setParameter(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, P[] arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, java.lang.Object[] arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, java.util.Collection<? extends P> arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, P[] arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, java.util.Collection arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, P[] arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, java.util.Collection arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(java.lang.String arg0, java.util.Collection<? extends P> arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, java.util.Collection<? extends P> arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, java.util.Collection<? extends P> arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, java.util.Collection<? extends P> arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, P[] arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(org.hibernate.query.QueryParameter<P> arg0, P[] arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, java.util.Collection<? extends P> arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, java.util.Collection<? extends P> arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, java.lang.Object[] arg1)
    {
        return this.delegate.setParameterList(arg0, arg1);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, P[] arg1, java.lang.Class<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public <P> org.hibernate.query.spi.QueryImplementor<R> setParameterList(int arg0, P[] arg1, org.hibernate.query.BindableType<P> arg2)
    {
        return this.delegate.setParameterList(arg0, arg1, arg2);
    }

    @Override
    public java.util.List<R> list()
    {
        return this.delegate.list();
    }

    @Override
    public org.hibernate.query.Query<R> setReadOnly(boolean arg0)
    {
        return this.delegate.setReadOnly(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setComment(java.lang.String arg0)
    {
        return this.delegate.setComment(arg0);
    }

    @Override
    public java.lang.String getComment()
    {
        return this.delegate.getComment();
    }

    @Override
    public R uniqueResult()
    {
        return this.delegate.uniqueResult();
    }

    @Override
    public R getSingleResult()
    {
        return this.delegate.getSingleResult();
    }

    @Override
    public java.util.Optional<R> uniqueResultOptional()
    {
        return this.delegate.uniqueResultOptional();
    }

    @Override
    public int executeUpdate()
    {
        return this.delegate.executeUpdate();
    }

    @Override
    public java.lang.String getQueryString()
    {
        return this.delegate.getQueryString();
    }

    @Override
    public org.hibernate.query.Query<R> applyGraph(org.hibernate.graph.RootGraph arg0, org.hibernate.graph.GraphSemantic arg1)
    {
        return this.delegate.applyGraph(arg0, arg1);
    }

    @Override
    public org.hibernate.query.Query<R> addQueryHint(java.lang.String arg0)
    {
        return this.delegate.addQueryHint(arg0);
    }

    @Override
    public org.hibernate.LockOptions getLockOptions()
    {
        return this.delegate.getLockOptions();
    }

    @Override
    public org.hibernate.query.Query<R> setLockOptions(org.hibernate.LockOptions arg0)
    {
        return this.delegate.setLockOptions(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setLockMode(jakarta.persistence.LockModeType arg0)
    {
        return this.delegate.setLockMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setLockMode(java.lang.String arg0, org.hibernate.LockMode arg1)
    {
        return this.delegate.setLockMode(arg0, arg1);
    }

    @Override
    public org.hibernate.query.spi.QueryOptions getQueryOptions()
    {
        return this.delegate.getQueryOptions();
    }

    @Override
    public org.hibernate.query.ParameterMetadata getParameterMetadata()
    {
        return this.delegate.getParameterMetadata();
    }

    @Override
    public org.hibernate.query.Query<R> setHibernateFlushMode(org.hibernate.FlushMode arg0)
    {
        return this.delegate.setHibernateFlushMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setCacheable(boolean arg0)
    {
        return this.delegate.setCacheable(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setCacheRegion(java.lang.String arg0)
    {
        return this.delegate.setCacheRegion(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setCacheMode(org.hibernate.CacheMode arg0)
    {
        return this.delegate.setCacheMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setCacheStoreMode(jakarta.persistence.CacheStoreMode arg0)
    {
        return this.delegate.setCacheStoreMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode arg0)
    {
        return this.delegate.setCacheRetrieveMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setTimeout(int arg0)
    {
        return this.delegate.setTimeout(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setFetchSize(int arg0)
    {
        return this.delegate.setFetchSize(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setMaxResults(int arg0)
    {
        return this.delegate.setMaxResults(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setFirstResult(int arg0)
    {
        return this.delegate.setFirstResult(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setHint(java.lang.String arg0, java.lang.Object arg1)
    {
        return this.delegate.setHint(arg0, arg1);
    }

    @Override
    public org.hibernate.query.Query<R> setEntityGraph(jakarta.persistence.EntityGraph<R> arg0, org.hibernate.graph.GraphSemantic arg1)
    {
        return this.delegate.setEntityGraph(arg0, arg1);
    }

    @Override
    public org.hibernate.query.Query<R> enableFetchProfile(java.lang.String arg0)
    {
        return this.delegate.enableFetchProfile(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> disableFetchProfile(java.lang.String arg0)
    {
        return this.delegate.disableFetchProfile(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setFlushMode(jakarta.persistence.FlushModeType arg0)
    {
        return this.delegate.setFlushMode(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setOrder(org.hibernate.query.Order<? super R> arg0)
    {
        return this.delegate.setOrder(arg0);
    }

    @Override
    public org.hibernate.query.Query<R> setOrder(java.util.List<org.hibernate.query.Order<? super R>> arg0)
    {
        return this.delegate.setOrder(arg0);
    }

    @Override
    public boolean isReadOnly()
    {
        return this.delegate.isReadOnly();
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
    public R getSingleResultOrNull()
    {
        return this.delegate.getSingleResultOrNull();
    }

    @Override
    public long getResultCount()
    {
        return this.delegate.getResultCount();
    }

    @Override
    public org.hibernate.query.KeyedResultList<R> getKeyedResultList(org.hibernate.query.KeyedPage<R> arg0)
    {
        return this.delegate.getKeyedResultList(arg0);
    }

    @Override
    public java.lang.Integer getFetchSize()
    {
        return this.delegate.getFetchSize();
    }

    @Override
    public org.hibernate.CacheMode getCacheMode()
    {
        return this.delegate.getCacheMode();
    }

    @Override
    public jakarta.persistence.CacheStoreMode getCacheStoreMode()
    {
        return this.delegate.getCacheStoreMode();
    }

    @Override
    public jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode()
    {
        return this.delegate.getCacheRetrieveMode();
    }

    @Override
    public boolean isCacheable()
    {
        return this.delegate.isCacheable();
    }

    @Override
    public boolean isQueryPlanCacheable()
    {
        return this.delegate.isQueryPlanCacheable();
    }

    @Override
    public org.hibernate.query.SelectionQuery<R> setQueryPlanCacheable(boolean arg0)
    {
        return this.delegate.setQueryPlanCacheable(arg0);
    }

    @Override
    public java.lang.String getCacheRegion()
    {
        return this.delegate.getCacheRegion();
    }

    @Override
    public jakarta.persistence.LockModeType getLockMode()
    {
        return this.delegate.getLockMode();
    }

    @Override
    public org.hibernate.LockMode getHibernateLockMode()
    {
        return this.delegate.getHibernateLockMode();
    }

    @Override
    public org.hibernate.query.SelectionQuery<R> setHibernateLockMode(org.hibernate.LockMode arg0)
    {
        return this.delegate.setHibernateLockMode(arg0);
    }

    @Override
    public org.hibernate.query.SelectionQuery<R> setAliasSpecificLockMode(java.lang.String arg0, org.hibernate.LockMode arg1)
    {
        return this.delegate.setAliasSpecificLockMode(arg0, arg1);
    }

    @Override
    public org.hibernate.query.SelectionQuery<R> setFollowOnLocking(boolean arg0)
    {
        return this.delegate.setFollowOnLocking(arg0);
    }

    @Override
    public jakarta.persistence.FlushModeType getFlushMode()
    {
        return this.delegate.getFlushMode();
    }

    @Override
    public org.hibernate.FlushMode getHibernateFlushMode()
    {
        return this.delegate.getHibernateFlushMode();
    }

    @Override
    public java.lang.Integer getTimeout()
    {
        return this.delegate.getTimeout();
    }

    @Override
    public java.util.Set<jakarta.persistence.Parameter<?>> getParameters()
    {
        return this.delegate.getParameters();
    }

    @Override
    public <T> T unwrap(java.lang.Class<T> arg0)
    {
        return this.delegate.unwrap(arg0);
    }

    @Override
    public java.util.Map<java.lang.String, java.lang.Object> getHints()
    {
        return this.delegate.getHints();
    }

    @Override
    public jakarta.persistence.Parameter<?> getParameter(int arg0)
    {
        return this.delegate.getParameter(arg0);
    }

    @Override
    public jakarta.persistence.Parameter<?> getParameter(java.lang.String arg0)
    {
        return this.delegate.getParameter(arg0);
    }

    @Override
    public <T> jakarta.persistence.Parameter<T> getParameter(java.lang.String arg0, java.lang.Class<T> arg1)
    {
        return this.delegate.getParameter(arg0, arg1);
    }

    @Override
    public <T> jakarta.persistence.Parameter<T> getParameter(int arg0, java.lang.Class<T> arg1)
    {
        return this.delegate.getParameter(arg0, arg1);
    }

    @Override
    public boolean isBound(jakarta.persistence.Parameter<?> arg0)
    {
        return this.delegate.isBound(arg0);
    }

    @Override
    public java.lang.Object getParameterValue(java.lang.String arg0)
    {
        return this.delegate.getParameterValue(arg0);
    }

    @Override
    public java.lang.Object getParameterValue(int arg0)
    {
        return this.delegate.getParameterValue(arg0);
    }

    @Override
    public <T> T getParameterValue(jakarta.persistence.Parameter<T> arg0)
    {
        return this.delegate.getParameterValue(arg0);
    }

}
