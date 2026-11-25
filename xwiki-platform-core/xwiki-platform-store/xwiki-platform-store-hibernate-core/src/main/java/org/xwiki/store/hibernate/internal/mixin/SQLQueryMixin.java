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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SQLQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;
import org.xwiki.store.hibernate.internal.bridge.PackageBridgeUtils;
import org.xwiki.store.hibernate.internal.legacy.LegacyQueries;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Restore the methods of the Hibernate 5 {@link SQLQuery} returning a {@link NativeQuery}, called by the code
 * compiled against Hibernate 5 using a variable of type {@link SQLQuery} (which is what
 * {@code Session#createSQLQuery} returned).
 * They can't be declared in {@link SQLQuery} itself since {@link NativeQuery} only extends it in the patched
 * Hibernate.
 *
 * @param <T> the query result type
 * @version $Id$
 */
@Mixin
@SuppressWarnings({ "unchecked", "rawtypes", "checkstyle:ClassFanOutComplexity" })
public interface SQLQueryMixin<T> extends SQLQuery<T>
{
    private NativeQuery<T> asNativeQuery()
    {
        return (NativeQuery<T>) this;
    }

    /**
     * Hibernate 5 version of {@code setHibernateFlushMode} returning a {@link NativeQuery}.
     *
     * @param flushMode see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setHibernateFlushMode")
    default NativeQuery<T> setHibernateFlushModeFlushModeNativeQuery(FlushMode flushMode)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setHibernateFlushMode(flushMode);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setFlushMode} returning a {@link NativeQuery}.
     *
     * @param flushMode see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setFlushMode")
    default NativeQuery<T> setFlushModeFlushModeNativeQuery(FlushMode flushMode)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setHibernateFlushMode(flushMode);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setFlushMode} returning a {@link NativeQuery}.
     *
     * @param flushMode see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setFlushMode")
    default NativeQuery<T> setFlushModeFlushModeTypeNativeQuery(javax.persistence.FlushModeType flushMode)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setFlushMode((FlushModeType) PackageBridgeUtils.toEnum(flushMode, FlushModeType.class));

        return q;
    }

    /**
     * Hibernate 5 version of {@code setCacheMode} returning a {@link NativeQuery}.
     *
     * @param cacheMode see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setCacheMode")
    default NativeQuery<T> setCacheModeCacheModeNativeQuery(CacheMode cacheMode)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setCacheMode(cacheMode);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setCacheable} returning a {@link NativeQuery}.
     *
     * @param cacheable see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setCacheable")
    default NativeQuery<T> setCacheableBooleanNativeQuery(boolean cacheable)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setCacheable(cacheable);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setCacheRegion} returning a {@link NativeQuery}.
     *
     * @param cacheRegion see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setCacheRegion")
    default NativeQuery<T> setCacheRegionStringNativeQuery(String cacheRegion)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setCacheRegion(cacheRegion);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setTimeout} returning a {@link NativeQuery}.
     *
     * @param timeout see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setTimeout")
    default NativeQuery<T> setTimeoutIntNativeQuery(int timeout)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setTimeout(timeout);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setFetchSize} returning a {@link NativeQuery}.
     *
     * @param fetchSize see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setFetchSize")
    default NativeQuery<T> setFetchSizeIntNativeQuery(int fetchSize)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setFetchSize(fetchSize);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setReadOnly} returning a {@link NativeQuery}.
     *
     * @param readOnly see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setReadOnly")
    default NativeQuery<T> setReadOnlyBooleanNativeQuery(boolean readOnly)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setReadOnly(readOnly);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setLockOptions} returning a {@link NativeQuery}.
     *
     * @param lockOptions see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setLockOptions")
    default NativeQuery<T> setLockOptionsLockOptionsNativeQuery(LockOptions lockOptions)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setLockOptions(lockOptions);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setLockMode} returning a {@link NativeQuery}.
     *
     * @param alias see the Hibernate 7 method
     * @param lockMode see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setLockMode")
    default NativeQuery<T> setLockModeStringLockModeNativeQuery(String alias, LockMode lockMode)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setLockMode(alias, lockMode);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setComment} returning a {@link NativeQuery}.
     *
     * @param comment see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setComment")
    default NativeQuery<T> setCommentStringNativeQuery(String comment)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setComment(comment);

        return q;
    }

    /**
     * Hibernate 5 version of {@code addQueryHint} returning a {@link NativeQuery}.
     *
     * @param hint see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("addQueryHint")
    default NativeQuery<T> addQueryHintStringNativeQuery(String hint)
    {
        NativeQuery<T> q = asNativeQuery();
        q.addQueryHint(hint);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param parameter see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterQueryParameterObjectNativeQuery(QueryParameter<Object> parameter, Object value)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameter(parameter, value);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param parameter see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterParameterObjectNativeQuery(
        javax.persistence.Parameter<Object> parameter, Object value)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameter((Parameter<Object>) (Object) parameter, value);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterStringObjectNativeQuery(String name, Object value)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameter(name, value);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param position see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterIntObjectNativeQuery(int position, Object value)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameter(position, value);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param parameter see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @param type see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterQueryParameterObjectTypeNativeQuery(
        QueryParameter<Object> parameter, Object value, Type type)
    {
        NativeQuery<T> q = asNativeQuery();
        LegacyQueries.setParameter(q, parameter, value, type);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @param type see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterStringObjectTypeNativeQuery(String name, Object value, Type type)
    {
        NativeQuery<T> q = asNativeQuery();
        LegacyQueries.setParameter(q, name, value, type);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameter} returning a {@link NativeQuery}.
     *
     * @param position see the Hibernate 7 method
     * @param value see the Hibernate 7 method
     * @param type see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameter")
    default NativeQuery<T> setParameterIntObjectTypeNativeQuery(int position, Object value, Type type)
    {
        NativeQuery<T> q = asNativeQuery();
        LegacyQueries.setParameter(q, position, value, type);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameterList} returning a {@link NativeQuery}.
     *
     * @param parameter see the Hibernate 7 method
     * @param values see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameterList")
    default NativeQuery<T> setParameterListQueryParameterCollectionNativeQuery(
        QueryParameter<Object> parameter, Collection<Object> values)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameterList(parameter, values);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameterList} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param values see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameterList")
    default NativeQuery<T> setParameterListStringCollectionNativeQuery(String name, Collection values)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameterList(name, values);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameterList} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param values see the Hibernate 7 method
     * @param type see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameterList")
    default NativeQuery<T> setParameterListStringCollectionTypeNativeQuery(String name, Collection values, Type type)
    {
        NativeQuery<T> q = asNativeQuery();
        LegacyQueries.setParameterList(q, name, values, type);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameterList} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param values see the Hibernate 7 method
     * @param type see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameterList")
    default NativeQuery<T> setParameterListStringObjectArrayTypeNativeQuery(String name, Object[] values, Type type)
    {
        NativeQuery<T> q = asNativeQuery();
        LegacyQueries.setParameterList(q, name, values == null ? null : Arrays.asList(values), type);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setParameterList} returning a {@link NativeQuery}.
     *
     * @param name see the Hibernate 7 method
     * @param values see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setParameterList")
    default NativeQuery<T> setParameterListStringObjectArrayNativeQuery(String name, Object[] values)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setParameterList(name, values);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setProperties} returning a {@link NativeQuery}.
     *
     * @param bean see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setProperties")
    default NativeQuery<T> setPropertiesObjectNativeQuery(Object bean)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setProperties(bean);

        return q;
    }

    /**
     * Hibernate 5 version of {@code setProperties} returning a {@link NativeQuery}.
     *
     * @param bean see the Hibernate 7 method
     * @return this query
     */
    @BinaryOnly("setProperties")
    default NativeQuery<T> setPropertiesMapNativeQuery(Map bean)
    {
        NativeQuery<T> q = asNativeQuery();
        q.setProperties(bean);

        return q;
    }
}
