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
package org.xwiki.notifications.filters.migration;

import java.util.HashSet;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.DataMigrationManager;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R140400000XWIKI15460DataMigration}.
 *
 * @version $Id$
 * @since 14.4
 * @since 13.10.6
 */
@ComponentTest
class R140400000XWIKI15460DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140400000XWIKI15460DataMigration dataMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private NotificationFilterPreferenceStore store;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named(XWikiHibernateBaseStore.HINT)
    protected Provider<DataMigrationManager> manager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Mock
    private XWikiContext context;

    @Mock
    private DataMigrationManager dataMigrationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", this.context);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(this.context.getMainXWiki()).thenReturn("mainwikiid");
        when(this.context.getWikiId()).thenReturn("mainwikiid");
        when(this.manager.get()).thenReturn(this.dataMigrationManager);
        when(this.dataMigrationManager.getDBVersion()).thenReturn(new XWikiDBVersion(0));
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(asList("mainwikiid", "wikiA"));
    }

    @Test
    void hibernateMigrateNotMainWiki() throws Exception
    {
        when(this.context.getWikiId()).thenReturn("subwikiid");
        this.dataMigration.hibernateMigrate();
        assertEquals(1, this.logCapture.size());
        assertEquals("Skipping, this migration only applies to the main wiki.", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        verifyNoInteractions(this.manager);
        verifyNoInteractions(this.wikiDescriptorManager);
        verifyNoInteractions(this.store);
    }

    @Test
    void hibernateMigrateAlreadyExecuted() throws Exception
    {
        when(this.dataMigrationManager.getDBVersion()).thenReturn(new XWikiDBVersion(131006001));
        this.dataMigration.hibernateMigrate();
        assertEquals(1, this.logCapture.size());
        assertEquals("Skipping, this migration has already been performed in 13.10.6+.", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
        verifyNoInteractions(this.wikiDescriptorManager);
        verifyNoInteractions(this.store);
    }

    @Test
    void hibernateMigrate() throws Exception
    {
        // nfp0 is from main wiki
        // nfp1 is from a removed wiki
        // nfp2 is from wikiA
        NotificationFilterPreference nfp0 = mock(NotificationFilterPreference.class);
        when(nfp0.isFromWiki("mainwikiid")).thenReturn(true);
        NotificationFilterPreference nfp1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference nfp2 = mock(NotificationFilterPreference.class);
        when(nfp2.isFromWiki("wikiA")).thenReturn(true);
        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenReturn(new HashSet<>(asList(
            nfp0,
            nfp1,
            nfp2
        )));
        this.dataMigration.hibernateMigrate();
        verify(this.store, never()).deleteFilterPreference(nfp0);
        verify(this.store).deleteFilterPreference(nfp1);
        verify(this.store, never()).deleteFilterPreference(nfp2);
        verify(this.store).getPaginatedFilterPreferences(1000, 0);
        // The offset is lowered by once since nfp1 was deleted.
        verify(this.store).getPaginatedFilterPreferences(1000, 999);
    }

    @Test
    void hibernateMigrateNotificationException() throws Exception
    {
        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenThrow(NotificationException.class);
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.hibernateMigrate());
        assertEquals("Failed to retrieve the notification filters preferences.", dataMigrationException.getMessage());
        assertEquals(NotificationException.class, dataMigrationException.getCause().getClass());
    }

    @Test
    void hibernateMigrateWikiManagerException() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenThrow(WikiManagerException.class);
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.hibernateMigrate());
        assertEquals("Failed to retrieve the ids of wikis of the farm.", dataMigrationException.getMessage());
        assertEquals(WikiManagerException.class, dataMigrationException.getCause().getClass());
    }
}
