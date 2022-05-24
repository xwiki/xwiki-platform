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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.WikiReference;
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

import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R140500000XWIKI15460DataMigration}.
 *
 * @version $Id$
 * @since 14.5RC1
 * @since 14.4.1
 * @since 13.10.7
 */
@ComponentTest
class R140500000XWIKI15460DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140500000XWIKI15460DataMigration dataMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private NotificationFilterPreferenceStore store;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(asList("mainwikiid", "wikiA"));
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainwikiid");
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainwikiid");
    }

    @Test
    void shouldExecuteNotMainWiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwikiid");
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(0)));
    }

    @Test
    void shouldExecuteAlreadyExecuted()
    {
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(131006001)));
    }

    @Test
    void shouldExecute()
    {
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(0)));
    }

    @Test
    void hibernateMigrate() throws Exception
    {
        // nfp0 is from main wiki
        // nfp1 is from a removed wiki
        // nfp2 is from wikiA
        NotificationFilterPreference nfp0 = mock(NotificationFilterPreference.class);
        when(nfp0.getWikiId()).thenReturn(Optional.of("mainwikiid"));
        NotificationFilterPreference nfp1 = mock(NotificationFilterPreference.class);
        when(nfp1.getWikiId()).thenReturn(Optional.of("unknownikiid"));
        NotificationFilterPreference nfp2 = mock(NotificationFilterPreference.class);
        when(nfp2.getWikiId()).thenReturn(Optional.of("wikiA"));
        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenReturn(new HashSet<>(asList(
            nfp0,
            nfp1,
            nfp2
        )));
        this.dataMigration.hibernateMigrate();
        verify(this.store).deleteFilterPreference(new WikiReference("unknownikiid"));
        verify(this.store).getPaginatedFilterPreferences(1000, 0);
        // The offset is lowered by once since nfp1 was deleted.
        verify(this.store).getPaginatedFilterPreferences(1000, 1000);
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
