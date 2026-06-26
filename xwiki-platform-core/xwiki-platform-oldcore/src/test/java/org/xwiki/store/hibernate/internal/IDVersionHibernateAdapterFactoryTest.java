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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Optional;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.store.hibernate.DatabaseProductNameResolver;
import org.xwiki.store.hibernate.HibernateAdapter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link IDVersionHibernateAdapterFactory}.
 * 
 * @version $Id$
 */
@ComponentTest
class IDVersionHibernateAdapterFactoryTest
{
    @InjectMockComponents
    private IDVersionHibernateAdapterFactory factory;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DatabaseMetaData metaData = mock(DatabaseMetaData.class);

    @MockComponent
    @Named("resolver")
    private DatabaseProductNameResolver resolver;

    private Optional<HibernateAdapter> createHibernateAdapter(String databaseName, String databaseVersion)
        throws SQLException
    {
        when(this.metaData.getDatabaseProductName()).thenReturn(databaseName);
        when(this.metaData.getDatabaseProductVersion()).thenReturn(databaseVersion);

        return this.factory.createHibernateAdapter(this.metaData, null);
    }

    private HibernateAdapter registerAdapter(String hint) throws Exception
    {
        return this.componentManager.registerMockComponent(HibernateAdapter.class, hint);
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.componentManager.registerComponent(DatabaseProductNameResolver.class, "resolver1",
            new DatabaseProductNameResolver()
            {
                @Override
                public Optional<String> resolve(String databaseProductName)
                {
                    return databaseProductName.equals("database") ? Optional.of("database1") : Optional.empty();
                }
            });
    }

    @Test
    void createHibernateAdapter() throws Exception
    {
        assertFalse(createHibernateAdapter("database", "version").isPresent());

        HibernateAdapter adapter1 = registerAdapter("database1");
        HibernateAdapter adapter2 = registerAdapter("database2");

        assertFalse(createHibernateAdapter("database", "version").isPresent());
        assertSame(adapter1, createHibernateAdapter("database1", "version").get());
        assertSame(adapter2, createHibernateAdapter("database2", "version").get());

        HibernateAdapter adapter11 = registerAdapter("database1/1.0");
        HibernateAdapter adapter12 = registerAdapter("database1/2.0");

        assertFalse(createHibernateAdapter("database", "version").isPresent());
        assertSame(adapter1, createHibernateAdapter("database1", "version").get());
        assertSame(adapter2, createHibernateAdapter("database2", "version").get());
        assertSame(adapter11, createHibernateAdapter("database1", "1.0").get());
        assertSame(adapter11, createHibernateAdapter("database1", "1.5").get());
        assertSame(adapter12, createHibernateAdapter("database1", "2.0").get());
        assertSame(adapter12, createHibernateAdapter("database1", "42").get());

        when(this.resolver.resolve("database")).thenReturn(Optional.of("database1"));

        assertSame(adapter1, createHibernateAdapter("database", "version").get());
    }
}
