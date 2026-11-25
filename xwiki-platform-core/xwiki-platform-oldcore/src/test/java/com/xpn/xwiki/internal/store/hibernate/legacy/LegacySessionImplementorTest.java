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
package com.xpn.xwiki.internal.store.hibernate.legacy;

import java.util.List;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.internal.SqmQueryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.objects.StringProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link LegacySessionImplementor} on a real (in-memory HSQLDB) database with the XWiki mapping.
 *
 * @version $Id$
 */
class LegacySessionImplementorTest
{
    private static SessionFactory sessionFactory;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private LoggerConfiguration loggerConfiguration = mock(LoggerConfiguration.class);

    @BeforeAll
    static void beforeAll()
    {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbc.JDBCDriver");
        configuration.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:legacysession;shutdown=true");
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        configuration.addResource("xwiki.hbm.xml");
        // The named queries are validated when building the session factory
        configuration.addResource("queries.hbm.xml");

        sessionFactory = configuration.buildSessionFactory();

        sessionFactory.inTransaction(session -> {
            session.createNativeMutationQuery("insert into xwikiobjects (XWO_ID, XWO_NUMBER, XWO_NAME, XWO_CLASSNAME)"
                + " values (1, 0, 'Space.Page', 'Space.Class')").executeUpdate();
            StringProperty property = new StringProperty();
            property.setId(1);
            property.setName("prop");
            property.setValue("value");
            session.persist(property);
        });
    }

    @AfterAll
    static void afterAll()
    {
        sessionFactory.close();
    }

    @BeforeEach
    void beforeEach()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(true);
    }

    private <T> T read(Function<Session, T> operation)
    {
        try (Session session = sessionFactory.openSession()) {
            return operation
                .apply(new LegacySessionImplementor((SessionImplementor) session, this.loggerConfiguration));
        }
    }

    @Test
    void containsLegacyPropertyIdentifier()
    {
        assertTrue(LegacySessionImplementor.containsLegacyPropertyIdentifier("where prop.id.id = obj.id"));
        assertTrue(LegacySessionImplementor.containsLegacyPropertyIdentifier("where prop.id.name = 'name'"));
        assertFalse(LegacySessionImplementor.containsLegacyPropertyIdentifier("where prop.id = obj.id"));
        assertFalse(LegacySessionImplementor.containsLegacyPropertyIdentifier("where rcs.id.docId = :docId"));
        assertFalse(LegacySessionImplementor.containsLegacyPropertyIdentifier("where prop.id.identifier = 1"));
        assertTrue(LegacySessionImplementor.containsLegacyPropertyIdentifier("where id.id = 1"));
    }

    @Test
    void legacyPropertyIdentifier()
    {
        String statement = "select obj.name from BaseObject obj, StringProperty prop"
            + " where prop.id.id = obj.id and prop.id.name = 'prop' and prop.value = 'value'";

        assertEquals(List.of("Space.Page"), read(session -> session.createQuery(statement, String.class).list()));

        assertEquals("Deprecated usage of the HQL paths [prop.id.id] and [prop.id.name] to the identifier of the"
            + " object properties; use [prop.id] and [prop.name] instead. Query [" + statement
            + "] has been converted to [select obj.name from BaseObject obj, StringProperty prop"
            + " where prop.id = obj.id and prop.name = 'prop' and prop.value = 'value']", this.logCapture.getMessage(0));

        // The conversion is remembered
        assertEquals(List.of("Space.Page"), read(session -> session.createQuery(statement, String.class).list()));
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void legacyPropertyIdentifierWithoutResultType()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        assertEquals(List.of("value"), read(session -> session
            .createQuery("select prop.value from StringProperty prop where prop.id.id = 1 and prop.id.name = 'prop'")
            .list()));
    }

    @Test
    void legacyPropertyIdentifierAndLegacyOrdinalParameters()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        assertEquals(List.of("Space.Page"),
            read(session -> session
                .createQuery("select obj.name from BaseObject obj, StringProperty prop"
                    + " where prop.id.id = obj.id and prop.id.name = ? and prop.value = ?", String.class)
                .setParameter(0, "prop").setParameter(1, "value").list()));
    }

    @Test
    void validStatementIsNotConverted()
    {
        assertEquals(List.of("value"), read(session -> session
            .createQuery("select prop.value from StringProperty prop where prop.id = 1 and prop.name = 'prop'",
                String.class)
            .list()));
    }

    @Test
    void invalidStatementAfterConversion()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        assertThrows(UnknownPathException.class, () -> read(session -> session
            .createQuery("select prop.value from StringProperty prop where prop.id.id = 1 and prop.unknown = 'prop'",
                String.class)));
    }

    @Test
    void legacyPropertyIdentifierDoesNotChangeStringLiterals()
    {
        String statement = "select prop.value from StringProperty prop"
            + " where prop.id.id = 1 and prop.id.name = 'prop' and prop.value <> 'prop.id.name'";

        assertEquals(List.of("value"), read(session -> session.createQuery(statement, String.class).list()));

        assertEquals("Deprecated usage of the HQL paths [prop.id.id] and [prop.id.name] to the identifier of the"
            + " object properties; use [prop.id] and [prop.name] instead. Query [" + statement
            + "] has been converted to [select prop.value from StringProperty prop"
            + " where prop.id = 1 and prop.name = 'prop' and prop.value <> 'prop.id.name']",
            this.logCapture.getMessage(0));
    }

    @Test
    void legacyPropertyIdentifierWithFullEntityNameAndAs()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        assertEquals(List.of("value"), read(session -> session.createQuery(
            "select prop.value from com.xpn.xwiki.objects.StringProperty as prop where prop.id.id = 1", String.class)
            .list()));
    }

    @Test
    void legacyPropertyIdentifierWithoutAlias()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        assertEquals(List.of("value"), read(session -> session
            .createQuery("select value from StringProperty where id.id = 1 and id.name = 'prop'", String.class)
            .list()));
    }

    @Test
    void invalidPathOnOtherEntityIsNotConverted()
    {
        // obj.id is the identifier of the object, which has no name: converting obj.id.name to obj.name would select
        // another field instead of failing
        assertThrows(UnknownPathException.class, () -> read(session -> session.createQuery(
            "select obj.name from BaseObject obj, StringProperty prop where prop.id = obj.id and obj.id.name = 'a'",
            String.class)));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void unwrapLegacyOrdinalParametersQuery()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);

        read(session -> {
            Query<String> query =
                session.createQuery("select prop.value from StringProperty prop where prop.id = ?", String.class);

            // The implementation class is not implemented by the proxy handling the legacy parameters
            assertInstanceOf(SqmQueryImpl.class, query.unwrap(SqmQueryImpl.class));
            // An interface of the query is still unwrapped to the proxy
            assertSame(query, query.unwrap(Query.class));
            // The positions keep being converted
            assertEquals(List.of("value"), query.unwrap(Query.class).setParameter(0, 1L).list());

            return null;
        });
    }
}
