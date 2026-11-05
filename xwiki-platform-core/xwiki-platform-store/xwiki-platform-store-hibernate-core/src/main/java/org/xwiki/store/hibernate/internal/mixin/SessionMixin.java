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

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.xwiki.store.hibernate.internal.legacy.LegacySessionOperations;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

import jakarta.persistence.RefreshOption;

/**
 * Hibernate 5 {@link Session} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public interface SessionMixin extends Session
{
    /**
     * @param object a transient instance of a persistent class
     * @return the generated identifier
     * @deprecated use {@link #persist(Object)} instead
     */
    @Deprecated
    default Serializable save(Object object)
    {
        return LegacySessionOperations.save(this, null, object);
    }

    /**
     * @param entityName the entity name
     * @param object a transient instance of a persistent class
     * @return the generated identifier
     * @deprecated use {@link #persist(String, Object)} instead
     */
    @Deprecated
    default Serializable save(String entityName, Object object)
    {
        return LegacySessionOperations.save(this, entityName, object);
    }

    /**
     * @param object a detached instance containing updated state
     * @deprecated use {@link #merge(Object)} instead
     */
    @Deprecated
    default void update(Object object)
    {
        LegacySessionOperations.update(this, null, object);
    }

    /**
     * @param entityName the entity name
     * @param object a detached instance containing updated state
     * @deprecated use {@link #merge(String, Object)} instead
     */
    @Deprecated
    default void update(String entityName, Object object)
    {
        LegacySessionOperations.update(this, entityName, object);
    }

    /**
     * @param object a transient or detached instance containing new or updated state
     * @deprecated use {@link #persist(Object)} or {@link #merge(Object)} instead
     */
    @Deprecated
    default void saveOrUpdate(Object object)
    {
        LegacySessionOperations.saveOrUpdate(this, null, object);
    }

    /**
     * @param entityName the entity name
     * @param object a transient or detached instance containing new or updated state
     * @deprecated use {@link #persist(String, Object)} or {@link #merge(String, Object)} instead
     */
    @Deprecated
    default void saveOrUpdate(String entityName, Object object)
    {
        LegacySessionOperations.saveOrUpdate(this, entityName, object);
    }

    /**
     * @param object the instance to be removed
     * @deprecated use {@link #remove(Object)} instead
     */
    @Deprecated
    default void delete(Object object)
    {
        remove(object);
    }

    /**
     * @param entityName the entity name
     * @param object the instance to be removed
     * @deprecated use {@link #remove(Object)} instead
     */
    @Deprecated
    default void delete(String entityName, Object object)
    {
        remove(object);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @return a persistent instance or null
     * @deprecated use {@link #find(Class, Object)} instead
     */
    @Deprecated
    default <T> T get(Class<T> entityType, Serializable id)
    {
        return find(entityType, id);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @param lockMode the lock mode
     * @return a persistent instance or null
     * @deprecated use {@link #find(Class, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default <T> T get(Class<T> entityType, Serializable id, LockMode lockMode)
    {
        return find(entityType, id, lockMode);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @param lockOptions the lock options
     * @return a persistent instance or null
     * @deprecated use {@link #find(Class, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default <T> T get(Class<T> entityType, Serializable id, LockOptions lockOptions)
    {
        return get(entityType, (Object) id, lockOptions);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @return a persistent instance or null
     * @deprecated use {@link #find(String, Object)} instead
     */
    @Deprecated
    default Object get(String entityName, Serializable id)
    {
        return find(entityName, id);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @param lockMode the lock mode
     * @return a persistent instance or null
     * @deprecated use {@link #find(String, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default Object get(String entityName, Serializable id, LockMode lockMode)
    {
        return find(entityName, id, lockMode);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @param lockOptions the lock options
     * @return a persistent instance or null
     * @deprecated use {@link #find(String, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default Object get(String entityName, Serializable id, LockOptions lockOptions)
    {
        return get(entityName, (Object) id, lockOptions);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @return the persistent instance or proxy
     * @deprecated use {@link #getReference(Class, Object)} instead
     */
    @Deprecated
    default <T> T load(Class<T> entityType, Serializable id)
    {
        return getReference(entityType, id);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @param lockMode the lock mode
     * @return the persistent instance
     * @deprecated use {@link #find(Class, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default <T> T load(Class<T> entityType, Serializable id, LockMode lockMode)
    {
        return find(entityType, id, lockMode);
    }

    /**
     * @param <T> the entity type
     * @param entityType the entity type
     * @param id an identifier
     * @param lockOptions the lock options
     * @return the persistent instance
     * @deprecated use {@link #find(Class, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default <T> T load(Class<T> entityType, Serializable id, LockOptions lockOptions)
    {
        return get(entityType, (Object) id, lockOptions);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @return the persistent instance or proxy
     * @deprecated use {@link #getReference(String, Object)} instead
     */
    @Deprecated
    default Object load(String entityName, Serializable id)
    {
        return getReference(entityName, id);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @param lockMode the lock mode
     * @return the persistent instance
     * @deprecated use {@link #find(String, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default Object load(String entityName, Serializable id, LockMode lockMode)
    {
        return find(entityName, id, lockMode);
    }

    /**
     * @param entityName the entity name
     * @param id an identifier
     * @param lockOptions the lock options
     * @return the persistent instance
     * @deprecated use {@link #find(String, Object, jakarta.persistence.FindOption...)} instead
     */
    @Deprecated
    default Object load(String entityName, Serializable id, LockOptions lockOptions)
    {
        return get(entityName, (Object) id, lockOptions);
    }

    /**
     * @param object an "empty" instance of the persistent class
     * @param id an identifier
     * @deprecated use {@link #find(Class, Object)} instead
     */
    @Deprecated
    default void load(Object object, Serializable id)
    {
        load(object, (Object) id);
    }

    /**
     * @param entityName the entity name
     * @param object a persistent or detached instance
     * @deprecated use {@link #refresh(Object)} instead
     */
    @Deprecated
    default void refresh(String entityName, Object object)
    {
        refresh(object);
    }

    /**
     * @param object a persistent or detached instance
     * @param lockMode the lock mode to use
     * @deprecated use {@link #refresh(Object, RefreshOption...)} instead
     */
    @Deprecated
    default void refresh(Object object, LockMode lockMode)
    {
        refresh(object, new RefreshOption[] {lockMode});
    }

    /**
     * @param entityName the entity name
     * @param object a persistent or detached instance
     * @param lockOptions contains the lock mode to use
     * @deprecated use {@link #refresh(Object, RefreshOption...)} instead
     */
    @Deprecated
    default void refresh(String entityName, Object object, LockOptions lockOptions)
    {
        refresh(object, lockOptions);
    }

    /**
     * @param entityName the entity name
     * @param object a persistent instance
     * @param lockMode the lock level
     * @deprecated use {@link #lock(Object, LockMode)} instead
     */
    @Deprecated
    default void lock(String entityName, Object object, LockMode lockMode)
    {
        lock(object, lockMode);
    }

    /**
     * @param flushMode the new flush mode
     * @deprecated use {@link #setHibernateFlushMode(FlushMode)} instead
     */
    @Deprecated
    default void setFlushMode(FlushMode flushMode)
    {
        setHibernateFlushMode(flushMode);
    }

    /**
     * @param queryString a SQL query
     * @return the native query
     * @deprecated use {@link #createNativeQuery(String)} instead
     */
    @Deprecated
    default NativeQuery createSQLQuery(String queryString)
    {
        return createNativeQuery(queryString);
    }

    /**
     * Hibernate 5 {@code getIdentifier} returned a {@link Serializable}.
     *
     * @param object a persistent instance
     * @return the identifier
     */
    @BinaryOnly("getIdentifier")
    default Serializable getIdentifierSerializable(Object object)
    {
        return (Serializable) getIdentifier(object);
    }
}
