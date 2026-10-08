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
package org.xwiki.internal.migration;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link R180407000XWIKI25199DataMigration}.
 *
 * @version $Id$
 */
@ComponentTest
class R180407000XWIKI25199DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R180407000XWIKI25199DataMigration dataMigration;

    @MockComponent
    @Named("180100000XWIKI23827")
    private HibernateDataMigration passwordStorageMigration;

    @MockComponent
    @Named("180405000XWIKI24357")
    private HibernateDataMigration passwordHashMigration;

    @MockComponent
    private Execution execution;

    @BeforeEach
    void setUp()
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);
        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    void getVersion()
    {
        assertEquals(180407000, this.dataMigration.getVersion().getVersion());
    }

    @Test
    void migrate() throws Exception
    {
        this.dataMigration.migrate();

        // The passwords put back in a StringProperty need to be moved to their dedicated table before being
        // re-encoded, since the re-encoding only looks at that table.
        InOrder inOrder = inOrder(this.passwordStorageMigration, this.passwordHashMigration);
        inOrder.verify(this.passwordStorageMigration).migrate();
        inOrder.verify(this.passwordHashMigration).migrate();
    }
}
