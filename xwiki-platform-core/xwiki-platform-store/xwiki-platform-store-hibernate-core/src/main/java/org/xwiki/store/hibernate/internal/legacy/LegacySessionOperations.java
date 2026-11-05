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
package org.xwiki.store.hibernate.internal.legacy;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.ObjectDeletedException;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.EntityState;
import org.hibernate.event.internal.OnUpdateVisitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implementation of the Hibernate 5 {@link Session} write APIs removed in Hibernate 7 and restored in the patched
 * Hibernate.
 * <p>
 * Hibernate 7 cannot attach a detached instance to a session anymore ({@code merge} returns a managed copy), so the
 * re-attachment logic of the {@code update} event listener of Hibernate 6 is reproduced here: code using the restored
 * {@code update} and {@code saveOrUpdate} expects the passed instance itself to be managed after the call.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class LegacySessionOperations
{
    private LegacySessionOperations()
    {
    }

    /**
     * Make a transient instance persistent and return its identifier.
     *
     * @param session the session
     * @param entityName the entity name or {@code null} to deduce it from the instance
     * @param object the instance to save
     * @return the identifier of the instance
     */
    public static Serializable save(Session session, String entityName, Object object)
    {
        if (entityName != null) {
            session.persist(entityName, object);
        } else {
            session.persist(object);
        }

        return (Serializable) session.getIdentifier(object);
    }

    /**
     * Attach a detached instance to the session (an instance already associated with the session is left
     * untouched).
     *
     * @param session the session
     * @param entityName the entity name or {@code null} to deduce it from the instance
     * @param object the instance to update
     */
    public static void update(Session session, String entityName, Object object)
    {
        EventSource source = session.unwrap(EventSource.class);
        PersistenceContext persistenceContext = source.getPersistenceContextInternal();

        if (!persistenceContext.reassociateIfUninitializedProxy(object)) {
            Object entity = persistenceContext.unproxyAndReassociate(object);
            EntityEntry entry = persistenceContext.getEntry(entity);

            if (entry == null) {
                reattach(source, entityName, entity);
            } else if (entry.getStatus() == Status.DELETED) {
                throw new ObjectDeletedException("deleted instance passed to update()", null, entityName);
            }
        }
    }

    /**
     * Save a transient instance or attach a detached instance to the session.
     *
     * @param session the session
     * @param entityName the entity name or {@code null} to deduce it from the instance
     * @param object the instance to save or update
     */
    public static void saveOrUpdate(Session session, String entityName, Object object)
    {
        EventSource source = session.unwrap(EventSource.class);
        PersistenceContext persistenceContext = source.getPersistenceContextInternal();

        if (!persistenceContext.reassociateIfUninitializedProxy(object)) {
            Object entity = persistenceContext.unproxyAndReassociate(object);
            EntityEntry entry = persistenceContext.getEntry(entity);

            switch (EntityState.getEntityState(entity, entityName, entry, source, null)) {
                case DETACHED:
                    reattach(source, entityName, entity);
                    break;

                case PERSISTENT:
                    // Nothing to do
                    break;

                default:
                    // Transient or deleted
                    save(source, entityName, entity);
                    break;
            }
        }
    }

    private static void reattach(EventSource source, String entityName, Object entity)
    {
        EntityPersister persister = source.getEntityPersister(entityName, entity);

        Object id = persister.getIdentifier(entity, source);
        if (id == null) {
            throw new TransientObjectException(
                "The given object has a null identifier: " + persister.getEntityName());
        }

        PersistenceContext persistenceContext = source.getPersistenceContextInternal();
        EntityKey key = source.generateEntityKey(id, persister);
        persistenceContext.checkUniqueness(key, entity);

        new OnUpdateVisitor(source, id, entity).process(entity, persister);

        // The loaded state is unknown (null) so that the whole entity is written to the database at flush time
        persistenceContext.addEntity(entity, persister.isMutable() ? Status.MANAGED : Status.READ_ONLY, null, key,
            persister.getVersion(entity), LockMode.NONE, true, persister, false);

        persister.afterReassociate(entity, source);

        // TODO: cascade the update to the associations mapped with cascade="save-update" (not used by XWiki core
        // mappings)
    }
}
