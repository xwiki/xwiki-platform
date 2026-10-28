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

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link OracleHibernateAdapter}.
 *
 * @version $Id$
 */
@ComponentTest
class OracleHibernateAdapterTest
{
    @InjectMockComponents
    private OracleHibernateAdapter adapter;

    @MockComponent
    private WikiDescriptorManager wikis;

    @MockComponent
    private HibernateConfiguration hibernateConfiguration;

    @BeforeEach
    void beforeEach()
    {
        when(this.hibernateConfiguration.getDBPrefix()).thenReturn("");
        when(this.wikis.getMainWikiId()).thenReturn("xwiki");
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");
    }

    @Test
    void getAlterCompressionStringWhenCompressionMatches()
    {
        assertNull(this.adapter.getAlterCompressionString("XWIKIRCS", true, Set.of("XWIKIRCS")));
        assertNull(this.adapter.getAlterCompressionString("XWIKIRCS", false, Set.of()));
    }

    @Test
    void getAlterCompressionStringIsQualifiedWithTheCurrentWikiSchema()
    {
        // The statement must name the schema explicitly, otherwise it is executed against whichever schema the pooled
        // JDBC connection happens to be on.
        assertEquals("ALTER TABLE SUBWIKI.XWIKIRCS COMPRESS",
            this.adapter.getAlterCompressionString("XWIKIRCS", true, Set.of()));
        assertEquals("ALTER TABLE SUBWIKI.XWIKIRCS NOCOMPRESS",
            this.adapter.getAlterCompressionString("XWIKIRCS", false, Set.of("XWIKIRCS")));
    }

    @Test
    void getAlterCompressionStringWhenMainWiki()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("xwiki");
        when(this.hibernateConfiguration.getDB()).thenReturn("mainschema");

        assertEquals("ALTER TABLE MAINSCHEMA.XWIKIRCS COMPRESS",
            this.adapter.getAlterCompressionString("XWIKIRCS", true, Set.of()));
    }

    @Test
    void getAlterCompressionStringWhenNoCurrentWiki()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        assertEquals("ALTER TABLE XWIKIRCS COMPRESS",
            this.adapter.getAlterCompressionString("XWIKIRCS", true, Set.of()));
    }

    @Test
    void getCompressedTablesStatementIsRestrictedToTheSchema()
    {
        // USER_TABLES would list the tables owned by the connected user instead of those of the wiki being updated.
        assertEquals("SELECT DISTINCT table_name FROM all_tables WHERE compression = 'ENABLED' AND owner = :owner",
            this.adapter.getCompressedTablesStatement("SUBWIKI"));
    }

    @Test
    void getCompressedTablesStatementWhenNoSchema()
    {
        assertEquals("SELECT DISTINCT table_name FROM all_tables WHERE compression = 'ENABLED'",
            this.adapter.getCompressedTablesStatement(null));
    }

    @Test
    void getCompressedTables()
    {
        Session session = mock(Session.class);
        NativeQuery<String> query = mock(NativeQuery.class);
        when(session.createNativeQuery(
            "SELECT DISTINCT table_name FROM all_tables WHERE compression = 'ENABLED' AND owner = :owner"))
                .thenReturn(query);
        when(query.list()).thenReturn(List.of("xwikircs", "XWikiDoc"));

        assertEquals(Set.of("XWIKIRCS", "XWIKIDOC"), this.adapter.getCompressedTables(session));

        // The schema of the current wiki is passed as a bind parameter rather than concatenated in the statement.
        verify(query).setParameter("owner", "SUBWIKI");
    }

    @Test
    void getCompressedTablesWhenNoCurrentWiki()
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        Session session = mock(Session.class);
        NativeQuery<String> query = mock(NativeQuery.class);
        when(session.createNativeQuery("SELECT DISTINCT table_name FROM all_tables WHERE compression = 'ENABLED'"))
            .thenReturn(query);
        when(query.list()).thenReturn(List.of("XWIKIRCS"));

        assertEquals(Set.of("XWIKIRCS"), this.adapter.getCompressedTables(session));

        verify(query, never()).setParameter(anyString(), any());
    }

    @Test
    void getColumnName()
    {
        // Oracle stores the unquoted column names in upper case, whatever the case used in the mapping
        assertEquals("ASE_PAGE", this.adapter.getColumnName("ase_page"));
        assertEquals("ASE_PAGE", this.adapter.getColumnName("ASE_PAGE"));
        assertNull(this.adapter.getColumnName(null));
    }
}
