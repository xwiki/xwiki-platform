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
package org.xwiki.store.hibernate.internal;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryProducer;
import org.hibernate.type.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the Hibernate 5 APIs restored in the patched Hibernate, against a real database.
 *
 * @version $Id$
 */
@SuppressWarnings({"deprecation", "removal", "rawtypes"})
class LegacyApiTest
{
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    @RegisterExtension
    private static LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void beforeAll()
    {
        sessionFactory = TestSessionFactories.create("legacy", logCapture);
    }

    @AfterAll
    static void afterAll()
    {
        sessionFactory.close();
    }

    @BeforeEach
    void beforeEach()
    {
        inTransaction(
            session -> session.createMutationQuery("delete from " + LegacyEntity.class.getName()).executeUpdate());
    }

    private void inTransaction(Consumer<Session> consumer)
    {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            consumer.accept(session);
            transaction.commit();
        }
    }

    private LegacyEntity load(long id)
    {
        try (Session session = sessionFactory.openSession()) {
            return session.find(LegacyEntity.class, id);
        }
    }

    /**
     * Signatures of Hibernate 5 methods (as found in code compiled against Hibernate 5) which are restored.
     */
    static Object[][] hibernate5Methods()
    {
        return new Object[][] {
            {Session.class, "save", MethodType.methodType(Serializable.class, Object.class)},
            {Session.class, "save", MethodType.methodType(Serializable.class, String.class, Object.class)},
            {Session.class, "update", MethodType.methodType(void.class, Object.class)},
            {Session.class, "update", MethodType.methodType(void.class, String.class, Object.class)},
            {Session.class, "saveOrUpdate", MethodType.methodType(void.class, Object.class)},
            {Session.class, "saveOrUpdate", MethodType.methodType(void.class, String.class, Object.class)},
            {Session.class, "delete", MethodType.methodType(void.class, Object.class)},
            {Session.class, "delete", MethodType.methodType(void.class, String.class, Object.class)},
            {Session.class, "get", MethodType.methodType(Object.class, Class.class, Serializable.class)},
            {Session.class, "get", MethodType.methodType(Object.class, String.class, Serializable.class)},
            {Session.class, "get",
                MethodType.methodType(Object.class, Class.class, Serializable.class, LockMode.class)},
            {Session.class, "load", MethodType.methodType(Object.class, Class.class, Serializable.class)},
            {Session.class, "load", MethodType.methodType(Object.class, String.class, Serializable.class)},
            {Session.class, "load", MethodType.methodType(void.class, Object.class, Serializable.class)},
            {Session.class, "createSQLQuery", MethodType.methodType(NativeQuery.class, String.class)},
            {Session.class, "getIdentifier", MethodType.methodType(Serializable.class, Object.class)},
            {Session.class, "setFlushMode", MethodType.methodType(void.class, FlushMode.class)},
            {Session.class, "refresh", MethodType.methodType(void.class, String.class, Object.class)},
            {Session.class, "refresh", MethodType.methodType(void.class, Object.class, LockMode.class)},
            {Session.class, "lock", MethodType.methodType(void.class, String.class, Object.class, LockMode.class)},
            {QueryProducer.class, "createQuery", MethodType.methodType(org.hibernate.Query.class, String.class)},
            {QueryProducer.class, "getNamedQuery", MethodType.methodType(org.hibernate.Query.class, String.class)},
            {QueryProducer.class, "createSQLQuery", MethodType.methodType(SQLQuery.class, String.class)},
            {Query.class, "setString", MethodType.methodType(Query.class, String.class, String.class)},
            {Query.class, "setString", MethodType.methodType(Query.class, int.class, String.class)},
            {Query.class, "setLong", MethodType.methodType(Query.class, String.class, long.class)},
            {Query.class, "setInteger", MethodType.methodType(Query.class, String.class, int.class)},
            {Query.class, "setBoolean", MethodType.methodType(Query.class, String.class, boolean.class)},
            {Query.class, "setTimestamp", MethodType.methodType(Query.class, String.class, Date.class)},
            {Query.class, "setEntity", MethodType.methodType(Query.class, String.class, Object.class)},
            {Query.class, "setParameter",
                MethodType.methodType(Query.class, String.class, Object.class, Type.class)},
            {Query.class, "setParameterList",
                MethodType.methodType(Query.class, String.class, Collection.class, Type.class)},
            {Query.class, "setFlushMode", MethodType.methodType(Query.class, FlushMode.class)},
            {Query.class, "setLockMode", MethodType.methodType(Query.class, String.class, LockMode.class)},
            {org.hibernate.Query.class, "list", MethodType.methodType(List.class)},
            {org.hibernate.Query.class, "uniqueResult", MethodType.methodType(Object.class)},
            {org.hibernate.Query.class, "setString",
                MethodType.methodType(org.hibernate.Query.class, String.class, String.class)},
            {org.hibernate.Query.class, "setParameter",
                MethodType.methodType(org.hibernate.Query.class, String.class, Object.class)},
            {org.hibernate.Query.class, "setMaxResults", MethodType.methodType(org.hibernate.Query.class, int.class)},
            {NativeQuery.class, "addScalar", MethodType.methodType(NativeQuery.class, String.class, Type.class)},
            {NativeQuery.class, "setParameter",
                MethodType.methodType(NativeQuery.class, String.class, Object.class, Type.class)},
            {Transaction.class, "getTimeout", MethodType.methodType(int.class)},
            {PersistentClass.class, "getPropertyIterator", MethodType.methodType(Iterator.class)},
            {Property.class, "getColumnIterator", MethodType.methodType(Iterator.class)},
            {Value.class, "getColumnIterator", MethodType.methodType(Iterator.class)},
            {Table.class, "getColumnIterator", MethodType.methodType(Iterator.class)},
            {Table.class, "getIndexIterator", MethodType.methodType(Iterator.class)},
            {Column.class, "getLength", MethodType.methodType(int.class)}};
    }

    @ParameterizedTest
    @MethodSource("hibernate5Methods")
    void hibernate5MethodIsLinkable(Class<?> owner, String name, MethodType type) throws Exception
    {
        // Resolve the method the same way the JVM resolves a call compiled against Hibernate 5
        assertNotNull(LOOKUP.findVirtual(owner, name, type));
    }

    @Test
    void hibernate5SubTypes()
    {
        assertTrue(org.hibernate.Query.class.isAssignableFrom(Query.class));
        assertTrue(SQLQuery.class.isAssignableFrom(NativeQuery.class));
    }

    @Test
    void saveAndGet()
    {
        LegacyEntity entity = new LegacyEntity(1, "name");

        inTransaction(session -> assertEquals(1L, session.save(entity)));

        try (Session session = sessionFactory.openSession()) {
            assertEquals("name", session.get(LegacyEntity.class, 1L).getName());
        }
    }

    @Test
    void updateDetachedInstance()
    {
        LegacyEntity entity = new LegacyEntity(1, "name");
        inTransaction(session -> session.save(entity));

        inTransaction(session -> {
            entity.setName("updated");
            session.update(entity);

            // Unlike merge(), update() makes the passed instance itself managed
            assertTrue(session.contains(entity));
            entity.setName("modified after update");
        });

        assertEquals("modified after update", load(1).getName());
    }

    @Test
    void saveOrUpdate()
    {
        LegacyEntity entity = new LegacyEntity(1, "name");

        // Transient
        inTransaction(session -> session.saveOrUpdate(entity));
        assertEquals("name", load(1).getName());

        // Detached
        inTransaction(session -> {
            entity.setName("updated");
            session.saveOrUpdate(entity);
            assertTrue(session.contains(entity));
        });
        assertEquals("updated", load(1).getName());

        // Persistent
        inTransaction(session -> {
            LegacyEntity persistent = session.get(LegacyEntity.class, 1L);
            persistent.setName("persistent");
            session.saveOrUpdate(persistent);
        });
        assertEquals("persistent", load(1).getName());
    }

    @Test
    void delete()
    {
        LegacyEntity entity = new LegacyEntity(1, "name");
        inTransaction(session -> session.save(entity));

        inTransaction(session -> session.delete(session.get(LegacyEntity.class, 1L)));

        assertNull(load(1));
    }

    @Test
    void typedParameterSetters()
    {
        inTransaction(session -> {
            session.save(new LegacyEntity(1, "one"));
            session.save(new LegacyEntity(2, "two"));
        });

        try (Session session = sessionFactory.openSession()) {
            Query<LegacyEntity> query = session.createQuery(
                "from " + LegacyEntity.class.getName() + " where name = :name and id = :id", LegacyEntity.class);
            List<LegacyEntity> result = query.setString("name", "two").setLong("id", 2L).list();

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getId());
        }
    }

    @Test
    void hibernate5LegacyQueryType() throws Throwable
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        try (Session session = sessionFactory.openSession()) {
            // Same calls as code compiled against org.hibernate.Query
            MethodHandle createQuery = LOOKUP.findVirtual(QueryProducer.class, "createQuery",
                MethodType.methodType(org.hibernate.Query.class, String.class));
            org.hibernate.Query query = (org.hibernate.Query) createQuery.invoke((QueryProducer) session,
                "select name from " + LegacyEntity.class.getName() + " where id = :id");

            assertSame(query, query.setLong("id", 1L));
            assertEquals("one", query.uniqueResult());
        }
    }

    @Test
    void createSQLQuery()
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        try (Session session = sessionFactory.openSession()) {
            SQLQuery query = session.createSQLQuery("select LE_NAME from legacyentity where LE_ID = ?1");
            query.setLong(1, 1L);

            assertEquals(List.of("one"), query.list());
        }
    }

    @Test
    void getIdentifier() throws Throwable
    {
        LegacyEntity entity = new LegacyEntity(1, "one");

        try (Session session = sessionFactory.openSession()) {
            session.save(entity);

            MethodHandle getIdentifier = LOOKUP.findVirtual(Session.class, "getIdentifier",
                MethodType.methodType(Serializable.class, Object.class));
            assertEquals(1L, (Serializable) getIdentifier.invoke(session, (Object) entity));
        }
    }
}
