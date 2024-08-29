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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
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

import com.xpn.xwiki.XWikiContext;
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
    private Execution execution;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("count")
    private QueryFilter countFilter;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ConfigurationSource configurationSource;

    private XWikiContext context;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(asList("mainwikiid", "wikiA"));
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainwikiid");
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainwikiid");

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);

        this.context = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.context);
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
        when(this.configurationSource.getProperty("eventstream.usemainstore", true)).thenReturn(false);
        // nfp0 is from main wiki
        // nfp1 is from a removed wiki
        // nfp2 is from a not-removed wikiA
        DefaultNotificationFilterPreference nfp0 = new DefaultNotificationFilterPreference();
        nfp0.setWiki("mainwikiid");
        nfp0.setOwner("xwiki:XWiki.ExistingUser");

        DefaultNotificationFilterPreference nfp1 = new DefaultNotificationFilterPreference();
        nfp1.setPageOnly("unknownikiid:XWiki.Test");
        nfp1.setOwner("mainwikiid:XWiki.ExistingUser");

        DefaultNotificationFilterPreference nfp2 = new DefaultNotificationFilterPreference();
        nfp2.setPage("wikiA:XWiki.test");
        nfp2.setOwner("xwiki:XWiki.DeletedUser");

        DefaultNotificationFilterPreference nfp3 = new DefaultNotificationFilterPreference();
        nfp3.setPage("wikiA:XWiki.test");
        nfp3.setOwner("otherwiki:XWiki.DeletedUser");

        DefaultNotificationFilterPreference nfp4 = new DefaultNotificationFilterPreference();
        nfp4.setPage("wikiA:XWiki.test");
        nfp4.setOwner("deletedwiki:XWiki.DeletedUser");

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(currentWikiId);
        when(this.context.getWikiReference()).thenReturn(new WikiReference(currentWikiId));

        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenReturn(new HashSet<>(asList(
            nfp0,
            nfp1,
            nfp2,
            nfp3,
            nfp4
        )));

        DocumentReference existingUserDocumentReference = new DocumentReference("xwiki", "XWiki", "ExistingUser");
        DocumentReference existingUserDocumentReference2 = new DocumentReference("mainwikiid", "XWiki", "ExistingUser");
        DocumentReference deletedUserDocumentReference = new DocumentReference("xwiki", "XWiki", "DeletedUser");
        DocumentReference deletedUserDocumentReference2 = new DocumentReference("otherwiki", "XWiki", "DeletedUser");
        DocumentReference deletedUserDocumentReference3 = new DocumentReference("deletedwiki", "XWiki", "DeletedUser");

        when(this.resolver.resolve("xwiki:XWiki.ExistingUser")).thenReturn(existingUserDocumentReference);
        when(this.resolver.resolve("mainwikiid:XWiki.ExistingUser")).thenReturn(existingUserDocumentReference2);
        when(this.resolver.resolve("xwiki:XWiki.DeletedUser")).thenReturn(deletedUserDocumentReference);
        when(this.resolver.resolve("otherwiki:XWiki.DeletedUser")).thenReturn(deletedUserDocumentReference2);
        when(this.resolver.resolve("deletedwiki:XWiki.DeletedUser")).thenReturn(deletedUserDocumentReference3);

        when(this.wikiDescriptorManager.exists("deletedwiki")).thenReturn(false);
        when(this.wikiDescriptorManager.exists("otherwiki")).thenReturn(true);
        when(this.wikiDescriptorManager.exists("xwiki")).thenReturn(true);

        String statement = ", BaseObject as obj where doc.fullName = :username and "
            + "doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'";

        Query query = mock(Query.class, "generalmock");
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);

        Query xwikiWikiQuery = mock(Query.class, "xwiki");
        Query otherWikiQuery = mock(Query.class, "otherWiki");
        when(query.setWiki("otherwiki")).thenReturn(otherWikiQuery);
        when(query.setWiki("xwiki")).thenReturn(xwikiWikiQuery);

        when(this.entityReferenceSerializer.serialize(existingUserDocumentReference)).thenReturn("XWiki.ExistingUser");
        when(this.entityReferenceSerializer.serialize(existingUserDocumentReference2)).thenReturn("XWiki.ExistingUser");

        when(this.entityReferenceSerializer.serialize(deletedUserDocumentReference)).thenReturn("XWiki.DeletedUser");
        when(this.entityReferenceSerializer.serialize(deletedUserDocumentReference2)).thenReturn("XWiki.DeletedUser");

        Query existingUserXWiki = mock(Query.class, "xwikiExistingUser");
        when(xwikiWikiQuery.bindValue("username", "XWiki.ExistingUser")).thenReturn(existingUserXWiki);
        when(existingUserXWiki.addFilter(this.countFilter)).thenReturn(existingUserXWiki);
        when(existingUserXWiki.setLimit(1)).thenReturn(existingUserXWiki);
        when(existingUserXWiki.execute()).thenReturn(Collections.singletonList(1L));

        Query deletedUserXWiki = mock(Query.class, "xwikiDeletedUser");
        when(xwikiWikiQuery.bindValue("username", "XWiki.DeletedUser")).thenReturn(deletedUserXWiki);
        when(deletedUserXWiki.addFilter(this.countFilter)).thenReturn(deletedUserXWiki);
        when(deletedUserXWiki.setLimit(1)).thenReturn(deletedUserXWiki);
        when(deletedUserXWiki.execute()).thenReturn(Collections.singletonList(0L));

        Query deletedUserOther = mock(Query.class, "otherDeletedUser");
        when(otherWikiQuery.bindValue("username", "XWiki.DeletedUser")).thenReturn(deletedUserOther);
        when(deletedUserOther.addFilter(this.countFilter)).thenReturn(deletedUserOther);
        when(deletedUserOther.setLimit(1)).thenReturn(deletedUserOther);
        when(deletedUserOther.execute()).thenReturn(Collections.singletonList(0L));

        if (currentWikiId.equals("mainwikiid")) {
            UserReference existingUserReference = mock(UserReference.class);
            when(this.documentReferenceUserReferenceResolver.resolve(existingUserDocumentReference))
                .thenReturn(existingUserReference);
            when(this.userManager.exists(existingUserReference)).thenReturn(true);
        } else {
            when(this.wikiDescriptorManager.exists("mainwikiid")).thenReturn(true);

            Query mainwikiidWikiQuery = mock(Query.class, "mainwikiid");
            when(query.setWiki("mainwikiid")).thenReturn(mainwikiidWikiQuery);

            Query existingUserMainWiki = mock(Query.class, "xwikiExistingUser");
            when(mainwikiidWikiQuery.bindValue("username", "XWiki.ExistingUser")).thenReturn(existingUserMainWiki);
            when(existingUserMainWiki.addFilter(this.countFilter)).thenReturn(existingUserMainWiki);
            when(existingUserMainWiki.setLimit(1)).thenReturn(existingUserMainWiki);
            when(existingUserMainWiki.execute()).thenReturn(Collections.singletonList(1L));
        }

        when(this.store.getPreferencesOfUser(deletedUserDocumentReference)).thenReturn(List.of(nfp2));
        when(this.store.getPreferencesOfUser(deletedUserDocumentReference2)).thenReturn(List.of(nfp3));
        when(this.store.getPreferencesOfUser(deletedUserDocumentReference3)).thenReturn(List.of(nfp4));

        this.dataMigration.hibernateMigrate();

        // Verify that the notification preferences of the unknown wiki are removed.
        verify(this.store, times(expectedTimes)).deleteFilterPreference(new WikiReference("unknownikiid"));
        // Verify that the notification preferences of the unknown users are removed.
        verify(this.store).deleteFilterPreferences(deletedUserDocumentReference);
        verify(this.store).deleteFilterPreferences(deletedUserDocumentReference2);
        verify(this.store).deleteFilterPreferences(deletedUserDocumentReference3);
        // Verify that  the results are paginated.
        verify(this.store).getPaginatedFilterPreferences(1000, 0);
        verify(this.store).getPaginatedFilterPreferences(1000, 1000);
    }

    @Test
    void hibernateMigrateSkipped() throws Exception
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("anotherwiki");
        when(this.configurationSource.getProperty("eventstream.usemainstore", true)).thenReturn(true);
        this.dataMigration.hibernateMigrate();

        // The store is used on all execution path after the check of the first condition.
        // If it is used, this means the migration is not skipped.
        verifyNoInteractions(this.store);
    }

    @Test
    void hibernateMigrateNotificationException() throws Exception
    {

        when(this.configurationSource.getProperty("eventstream.usemainstore", true)).thenReturn(false);
        when(this.store.getPaginatedFilterPreferences(1000, 0)).thenThrow(NotificationException.class);
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.hibernateMigrate());
        assertEquals("Failed to retrieve the notification filters preferences.", dataMigrationException.getMessage());
        assertEquals(NotificationException.class, dataMigrationException.getCause().getClass());
    }

    @Test
    void hibernateMigrateWikiManagerException() throws Exception
    {
        when(this.configurationSource.getProperty("eventstream.usemainstore", true)).thenReturn(false);
        when(this.wikiDescriptorManager.getAllIds()).thenThrow(WikiManagerException.class);
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class, () -> this.dataMigration.hibernateMigrate());
        assertEquals("Failed to retrieve the ids of wikis of the farm.", dataMigrationException.getMessage());
        assertEquals(WikiManagerException.class, dataMigrationException.getCause().getClass());
    }
}
