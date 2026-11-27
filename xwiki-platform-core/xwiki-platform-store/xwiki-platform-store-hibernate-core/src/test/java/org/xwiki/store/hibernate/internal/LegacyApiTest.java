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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryProducer;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.LongType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringType;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void hibernate5TypeConstants() throws Throwable
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        // Same field access as code compiled against Hibernate 5
        Type stringType = (Type) LOOKUP.findStaticGetter(StandardBasicTypes.class, "STRING", StringType.class).invoke();
        Type longType = (Type) LOOKUP.findStaticGetter(StandardBasicTypes.class, "LONG", LongType.class).invoke();
        assertSame(StringType.INSTANCE, stringType);
        // The Java compiler still sees the Hibernate 7 constants
        assertInstanceOf(BasicTypeReference.class, StandardBasicTypes.STRING);

        try (Session session = sessionFactory.openSession()) {
            Query<String> query = session.createQuery(
                "select name from " + LegacyEntity.class.getName() + " where name = :name", String.class);
            assertEquals(List.of("one"), query.setParameter("name", "one", stringType).list());

            NativeQuery<?> nativeQuery = session.createNativeQuery("select LE_ID as id from legacyentity");
            nativeQuery.addScalar("id", longType);
            assertEquals(List.of(1L), nativeQuery.list());
        }
    }

    @Test
    void setParameterListWithNullElement()
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        try (Session session = sessionFactory.openSession()) {
            Query<String> query = session.createQuery(
                "select name from " + LegacyEntity.class.getName() + " where name in (:names)", String.class);
            query.setParameterList("names", new Object[] { "one", null }, StringType.INSTANCE);

            assertEquals(List.of("one"), query.list());
        }
    }

    @Test
    void hibernate5SQLQueryMethods() throws Throwable
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        try (Session session = sessionFactory.openSession()) {
            SQLQuery query = session.createSQLQuery("select LE_NAME from legacyentity where LE_ID = :id");

            // Same call as code compiled against Hibernate 5 using a SQLQuery variable
            MethodHandle setParameter = LOOKUP.findVirtual(SQLQuery.class, "setParameter",
                MethodType.methodType(NativeQuery.class, String.class, Object.class));
            MethodHandle setCacheable = LOOKUP.findVirtual(SQLQuery.class, "setCacheable",
                MethodType.methodType(NativeQuery.class, boolean.class));
            assertSame(query, setParameter.invoke(query, "id", (Object) 1L));
            assertSame(query, setCacheable.invoke(query, false));

            assertEquals(List.of("one"), query.list());
        }
    }

    @Test
    void hibernate5QueryMethods() throws Throwable
    {
        inTransaction(session -> {
            session.save(new LegacyEntity(1, "one"));
            session.save(new LegacyEntity(2, "two"));
        });

        try (Session session = sessionFactory.openSession()) {
            org.hibernate.Query<String> query = session.createQuery(
                "select name from " + LegacyEntity.class.getName() + " where id in (:ids) order by id", String.class);

            assertSame(query, query.setHint("org.hibernate.readOnly", true));
            assertSame(query, query.setParameterList("ids", List.of(1L, 2L)));
            assertEquals(List.of("ids"), List.of(query.getNamedParameters()));
            assertSame(query, query.setHibernateMaxResults(1));
            assertEquals(1, query.getHibernateMaxResults());

            Iterator<String> iterator = query.iterate();
            assertEquals("one", iterator.next());
            assertFalse(iterator.hasNext());

            // javax.persistence based variant, only visible to code compiled against Hibernate 5
            MethodHandle setFlushMode = LOOKUP.findVirtual(org.hibernate.Query.class, "setFlushMode",
                MethodType.methodType(org.hibernate.Query.class, javax.persistence.FlushModeType.class));
            assertSame(query, setFlushMode.invoke(query, javax.persistence.FlushModeType.COMMIT));
            assertEquals(jakarta.persistence.FlushModeType.COMMIT, ((Query<String>) query).getFlushMode());
        }
    }

    @Test
    void hibernate5ScrollableResults() throws Throwable
    {
        inTransaction(session -> session.save(new LegacyEntity(1, "one")));

        try (Session session = sessionFactory.openSession();
            ScrollableResults<String> results =
                session.createQuery("select name from " + LegacyEntity.class.getName(), String.class).scroll()) {
            assertTrue(results.next());

            // Hibernate 5 always returned the row as an array
            MethodHandle get =
                LOOKUP.findVirtual(ScrollableResults.class, "get", MethodType.methodType(Object[].class));
            MethodHandle getString = LOOKUP.findVirtual(ScrollableResults.class, "getString",
                MethodType.methodType(String.class, int.class));
            assertArrayEquals(new Object[] { "one" }, (Object[]) get.invoke(results));
            assertEquals("one", (String) getString.invoke(results, 0));
        }
    }

    @Test
    void hibernate5StatelessSession() throws Throwable
    {
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            Transaction transaction = session.beginTransaction();

            MethodHandle insert = LOOKUP.findVirtual(StatelessSession.class, "insert",
                MethodType.methodType(Serializable.class, Object.class));
            MethodHandle get = LOOKUP.findVirtual(StatelessSession.class, "get",
                MethodType.methodType(Object.class, Class.class, Serializable.class));
            assertEquals(1L, (Serializable) insert.invoke(session, (Object) new LegacyEntity(1, "one")));
            assertEquals("one", ((LegacyEntity) get.invoke(session, LegacyEntity.class, (Serializable) 1L)).getName());

            transaction.commit();
        }
    }

    @Test
    void javaxTransactionSynchronization() throws Throwable
    {
        List<Integer> statuses = new ArrayList<>();
        javax.transaction.Synchronization synchronization = new javax.transaction.Synchronization()
        {
            @Override
            public void beforeCompletion()
            {
                statuses.add(-1);
            }

            @Override
            public void afterCompletion(int status)
            {
                statuses.add(status);
            }
        };

        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            MethodHandle registerSynchronization = LOOKUP.findVirtual(Transaction.class, "registerSynchronization",
                MethodType.methodType(void.class, javax.transaction.Synchronization.class));
            registerSynchronization.invoke(transaction, synchronization);
            transaction.commit();
        }

        assertEquals(List.of(-1, jakarta.transaction.Status.STATUS_COMMITTED), statuses);
    }

    @Test
    void loadWithLockModeOfMissingEntity()
    {
        try (Session session = sessionFactory.openSession()) {
            assertThrows(ObjectNotFoundException.class,
                () -> session.load(LegacyEntity.class, (Serializable) 42L, LockMode.READ));
        }
    }

    @Test
    void deleteDynamicMapEntity()
    {
        Map<String, Object> entity = new HashMap<>();
        entity.put("id", 1L);
        entity.put("name", "one");
        inTransaction(session -> session.save("LegacyMap", entity));

        // A map built by the caller, which can only be identified from the passed entity name
        Map<String, Object> detached = new HashMap<>(entity);
        inTransaction(session -> session.delete("LegacyMap", detached));

        try (Session session = sessionFactory.openSession()) {
            assertNull(session.find("LegacyMap", 1L));
        }
    }

    @Test
    void enumType()
    {
        LegacyEnumEntity entity = new LegacyEnumEntity();
        entity.setId(1);
        entity.setOrdinalUnit(TimeUnit.MINUTES);
        entity.setNamedUnit(TimeUnit.HOURS);
        entity.setTypedUnit(TimeUnit.DAYS);
        LegacyEnumEntity nullEntity = new LegacyEnumEntity();
        nullEntity.setId(2);
        inTransaction(session -> {
            session.persist(entity);
            session.persist(nullEntity);
        });

        try (Session session = sessionFactory.openSession()) {
            // Stored the same way as Hibernate 5 did
            Object[] row = session.createNativeQuery(
                "select LEE_ORDINAL, LEE_NAMED, LEE_TYPED from legacyenumentity where LEE_ID = 1", Object[].class)
                .getSingleResult();
            assertEquals(TimeUnit.MINUTES.ordinal(), ((Number) row[0]).intValue());
            assertEquals("HOURS", row[1]);
            assertEquals("DAYS", row[2]);

            LegacyEnumEntity loaded = session.find(LegacyEnumEntity.class, 1L);
            assertEquals(TimeUnit.MINUTES, loaded.getOrdinalUnit());
            assertEquals(TimeUnit.HOURS, loaded.getNamedUnit());
            assertEquals(TimeUnit.DAYS, loaded.getTypedUnit());

            LegacyEnumEntity loadedNull = session.find(LegacyEnumEntity.class, 2L);
            assertNull(loadedNull.getOrdinalUnit());
            assertNull(loadedNull.getNamedUnit());
            assertNull(loadedNull.getTypedUnit());

            assertEquals(List.of(1L), session
                .createQuery("select e.id from LegacyEnumEntity e where e.namedUnit = :unit", Long.class)
                .setParameter("unit", TimeUnit.HOURS).list());
        }
    }

    @Test
    void namedQueryDefinition()
    {
        Configuration configuration = new Configuration();
        configuration.addResource("legacy.hbm.xml");

        // Like Hibernate 5, the configuration doesn't expose the named queries of the mappings
        assertTrue(configuration.getNamedQueries().isEmpty());

        // The named query definitions found in the metadata are also Hibernate 5 definitions
        try (StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("jakarta.persistence.database-product-name", "HSQL Database Engine")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false").build()) {
            org.hibernate.engine.spi.NamedQueryDefinition definition =
                (org.hibernate.engine.spi.NamedQueryDefinition) new MetadataSources(registry)
                    .addResource("legacy.hbm.xml").buildMetadata().getNamedHqlQueryMapping("legacyEntityNames");

            assertEquals("select entity.name from org.xwiki.store.hibernate.internal.LegacyEntity entity",
                definition.getQuery());
        }

        assertTrue(logCapture.getMessage(logCapture.size() - 1)
            .startsWith("HHH90000028: Support for `<hibernate-mappings/>` is deprecated"));
    }
}
