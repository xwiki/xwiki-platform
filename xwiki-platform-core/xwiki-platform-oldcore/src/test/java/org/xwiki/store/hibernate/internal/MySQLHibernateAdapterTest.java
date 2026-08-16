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

import java.util.Map;

import org.hibernate.dialect.MySQLDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link MySQLHibernateAdapter}.
 *
 * @version $Id$
 */
@ComponentTest
class MySQLHibernateAdapterTest
{
    @InjectMockComponents
    private MySQLHibernateAdapter adapter;

    @MockComponent
    private WikiDescriptorManager wikis;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private HibernateConfiguration hibernateConfiguration;

    @BeforeEach
    void beforeEach()
    {
        when(this.hibernateStore.getDialect()).thenReturn(new MySQLDialect());
        when(this.hibernateConfiguration.getDBPrefix()).thenReturn("");
        when(this.wikis.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    void getAlterRowFormatStringWhenRowFormatMatches() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");

        assertNull(this.adapter.getAlterRowFormatString("xwikircs", false, Map.of("xwikircs", "Dynamic"), null));
        assertNull(this.adapter.getAlterRowFormatString("xwikircs", true, Map.of("xwikircs", "Compressed"), null));
    }

    @Test
    void getAlterRowFormatStringIsQualifiedWithTheCurrentWikiDatabase() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("subwiki");

        // The statement must name the database explicitly, otherwise it is executed against whichever database the
        // pooled JDBC connection happens to be on.
        assertEquals("ALTER TABLE `subwiki`.xwikircs ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of("xwikircs", "Compact"), null));
        assertEquals("ALTER TABLE `subwiki`.xwikircs ROW_FORMAT=Compressed",
            this.adapter.getAlterRowFormatString("xwikircs", true, Map.of(), null));
    }

    @Test
    void getAlterRowFormatStringWhenMainWiki() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn("xwiki");
        when(this.hibernateConfiguration.getDB()).thenReturn("maindb");

        assertEquals("ALTER TABLE `maindb`.xwikircs ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of(), null));
    }

    @Test
    void getAlterRowFormatStringWhenNoCurrentWiki() throws Exception
    {
        when(this.wikis.getCurrentWikiId()).thenReturn(null);

        assertEquals("ALTER TABLE xwikircs ROW_FORMAT=Dynamic",
            this.adapter.getAlterRowFormatString("xwikircs", false, Map.of(), null));
    }
}
