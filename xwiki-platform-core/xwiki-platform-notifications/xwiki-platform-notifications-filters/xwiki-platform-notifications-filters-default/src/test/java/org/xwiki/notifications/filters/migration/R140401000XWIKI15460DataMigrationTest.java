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
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceConfiguration;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R140401000XWIKI15460DataMigration}.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@ComponentTest
class R140401000XWIKI15460DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140401000XWIKI15460DataMigration dataMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private NotificationFilterPreferenceStore store;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    private NotificationFilterPreferenceConfiguration filterPreferenceConfiguration;

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
    void shouldExecuteAlreadyExecuted()
    {
        assertFalse(this.dataMigration.shouldExecute(new XWikiDBVersion(131007001)));
    }

    @Test
    void shouldExecute()
    {
        assertTrue(this.dataMigration.shouldExecute(new XWikiDBVersion(0)));
    }

    /**
     * This test is called twice. The first time on the main wiki, where
     * {@link NotificationFilterPreferenceStore#deleteFilterPreference(WikiReference)} is expected to be called once to
     * remove the filter preferences of a removed wiki ({@code unknownikiid}). The second time with on a sub-wiki, where
     * wikis are not inspected, and {@link NotificationFilterPreferenceStore#deleteFilterPreference(WikiReference)}
     * should not be called.
     *
     * @param currentWikiId the current wiki id
     * @param expectedTimes the expected number of times
     *     {@link NotificationFilterPreferenceStore#deleteFilterPreference(WikiReference)} is called
     */
    @ParameterizedTest
    @CsvSource({ "mainwikiid,1", "anotherwiki,0" })
    void hibernateMigrate(String currentWikiId, int expectedTimes) throws Exception
    {
        // nfp0 is from main wiki
        // nfp1 is from a removed wiki
        // nfp2 is from a not-removed wikiA
        DefaultNotificationFilterPreference nfp0 = new DefaultNotificationFilterPreference();
        nfp0.setWiki("mainwikiid");
        nfp0.setOwner("xwiki:XWiki.ExistingUser");
        DefaultNotificationFilterPreference nfp1 = new DefaultNotificationFilterPreference();
        nfp1.setPageOnly("unknownikiid:XWiki.Test");
        nfp1.setOwner("xwiki:XWiki.ExistingUser");
        DefaultNotificationFilterPreference nfp2 = new DefaultNotificationFilterPreference();
        nfp2.setPage("wikiA:XWiki.test");
        nfp2.setOwner("xwiki:XWiki.DeletedUser");
        DefaultNotificationFilterPreference nfp3 = new DefaultNotificationFilterPreference();
        nfp3.setPage("wikiA:XWiki.test");
        nfp3.setOwner("xwiki:XWiki.DeletedUser");
        DocumentReference existingUserDocumentReference = new DocumentReference("xwiki", "XWiki", "ExistingUser");
        DocumentReference deletedUserDocumentReference = new DocumentReference("xwiki", "XWiki", "DeletedUser");

        UserReference existingUserReference = mock(UserReference.class);
        UserReference deletedUserReference = mock(UserReference.class);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(currentWikiId);
        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenReturn(new HashSet<>(asList(
            nfp0,
            nfp1,
            nfp2,
            nfp3
        )));
        when(this.resolver.resolve("xwiki:XWiki.ExistingUser")).thenReturn(existingUserDocumentReference);
        when(this.resolver.resolve("xwiki:XWiki.DeletedUser")).thenReturn(deletedUserDocumentReference);
        when(this.documentReferenceUserReferenceResolver.resolve(existingUserDocumentReference))
            .thenReturn(existingUserReference);
        when(this.documentReferenceUserReferenceResolver.resolve(deletedUserDocumentReference))
            .thenReturn(deletedUserReference);
        when(this.userManager.exists(existingUserReference)).thenReturn(true);
        when(this.userManager.exists(deletedUserReference)).thenReturn(false);
        when(this.store.getPreferencesOfUser(deletedUserDocumentReference)).thenReturn(List.of(nfp3));

        this.dataMigration.hibernateMigrate();

        // Verify that the notification preferences of the unknown wiki are removed.
        verify(this.store, times(expectedTimes)).deleteFilterPreference(new WikiReference("unknownikiid"));
        // Verify that the notification preferences of the unknown user are removed.
        verify(this.store).deleteFilterPreferences(deletedUserDocumentReference);
        // Verify that the users are only resolved once.
        verify(this.resolver).resolve("xwiki:XWiki.ExistingUser");
        // Unknown users are resolved a second time when removed. 
        verify(this.resolver, times(2)).resolve("xwiki:XWiki.DeletedUser");
        // Verify that  the results are paginated.
        verify(this.store).getPaginatedFilterPreferences(1000, 0);
        verify(this.store).getPaginatedFilterPreferences(1000, 1000);
    }

    @Test
    void hibernateMigrateSkipped() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("anotherwiki");
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(true);
        this.dataMigration.hibernateMigrate();

        // The store is used on all execution path after the check of the first condition.
        // If it is used, this means the migration is not skipped.
        verifyNoInteractions(this.store);
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
