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

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;

import org.hibernate.Query;
import org.hibernate.transform.ResultTransformer;
import org.xwiki.store.hibernate.internal.bridge.PackageBridgeUtils;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Restore the methods of the Hibernate 5 {@link Query} which can't be declared in the restored {@link Query} itself
 * without being visible to the Java compiler (they use the {@code javax.persistence} types, or conflict with a method
 * of {@link org.hibernate.query.Query}).
 *
 * @param <R> the query result type
 * @version $Id$
 */
@Mixin
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface LegacyQueryMixin<R> extends Query<R>
{
    private org.hibernate.query.Query<R> asHibernateQuery()
    {
        return (org.hibernate.query.Query<R>) this;
    }

    /**
     * Hibernate 5 version of {@code setFlushMode} taking a {@code javax.persistence} flush mode.
     *
     * @param flushMode the flush mode
     * @return this query
     */
    @BinaryOnly("setFlushMode")
    default Query<R> setFlushModeJavax(javax.persistence.FlushModeType flushMode)
    {
        asHibernateQuery().setFlushMode((FlushModeType) PackageBridgeUtils.toEnum(flushMode, FlushModeType.class));

        return this;
    }

    /**
     * Hibernate 5 version of {@code setLockMode} taking a {@code javax.persistence} lock mode.
     *
     * @param lockMode the lock mode
     * @return this query
     */
    @BinaryOnly("setLockMode")
    default Query<R> setLockModeJavax(javax.persistence.LockModeType lockMode)
    {
        asHibernateQuery().setLockMode((LockModeType) PackageBridgeUtils.toEnum(lockMode, LockModeType.class));

        return this;
    }

    /**
     * Hibernate 5 version of {@code setParameter} taking a {@code javax.persistence} parameter (the
     * {@code jakarta.persistence} parameters extend their {@code javax.persistence} twin in the patched Hibernate).
     *
     * @param parameter the parameter
     * @param value the parameter value
     * @return this query
     */
    @BinaryOnly("setParameter")
    default Query<R> setParameterJavax(javax.persistence.Parameter parameter, Object value)
    {
        asHibernateQuery().setParameter((Parameter<Object>) (Object) parameter, value);

        return this;
    }

    /**
     * Hibernate 5 version of {@code setResultTransformer} returning a {@link Query}.
     *
     * @param transformer the transformer
     * @return this query
     */
    @BinaryOnly("setResultTransformer")
    default Query<R> setResultTransformerLegacy(ResultTransformer transformer)
    {
        asHibernateQuery().setResultTransformer(transformer);

        return this;
    }
}
