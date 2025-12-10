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
package org.xwiki.refactoring.internal.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiGroupsDocumentInitializer;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests about {@link UpdateRightsOnDocumentRenameListener}.
 *
 * @version $Id$
 * @since 11.9RC1
 */
@OldcoreTest
@ComponentList({
    // Components for the mandatory document initializers and dependencies for making them work.
    XWikiGroupsDocumentInitializer.class,
    XWikiUsersDocumentInitializer.class,
    DefaultConverterManager.class,
    ContextComponentManagerProvider.class,
    EnumConverter.class,
    ConvertUtilsConverter.class
})
@ReferenceComponentList
class UpdateRightsOnDocumentRenameListenerTest
{
    private static final List<String> WIKIS_LIST = Arrays.asList("wiki", "subwiki", "othersubwiki");

    @InjectMockComponents
    private UpdateRightsOnDocumentRenameListener listener;

    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named(RefactoringJobs.REPLACE_USER)
    private Job replaceUserJob;

    @MockComponent
    private RequestFactory requestFactory;

    // Needed for the mandatory document initializer
    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    @Named("class")
    private SheetBinder classSheetBinder;

    @MockComponent
    @Named("document")
    private SheetBinder documentSheetBinder;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Mock
    private ReplaceUserRequest replaceUserRequest;

    /**
     * Arguments provider that provides different combinations of source and target references including the wiki
     * references on which the change needs to be executed for user rename.
     */
    static Stream<Arguments> provideSourceTargetAndWikis()
    {
        return Stream.of(
            // User rename in main wiki
            Arguments.of(new DocumentReference("xwiki", "XWiki", "foo"),
                new DocumentReference("xwiki", "XWiki", "bar"),
                WIKIS_LIST),
            // User rename in subwiki
            Arguments.of(new DocumentReference("subwiki", "XWiki", "foo"),
                new DocumentReference("subwiki", "XWiki", "bar"),
                List.of("subwiki")),
            // User rename from subwiki to other subwiki
            Arguments.of(new DocumentReference("subwiki", "XWiki", "foo"),
                new DocumentReference("othersubwiki", "XWiki", "bar"),
                List.of("subwiki", "othersubwiki")),
            // User rename from main wiki to subwiki
            Arguments.of(new DocumentReference("xwiki", "XWiki", "foo"),
                new DocumentReference("subwiki", "XWiki", "bar"),
                WIKIS_LIST),
            // User rename from subwiki to main wiki
            Arguments.of(new DocumentReference("subwiki", "XWiki", "foo"),
                new DocumentReference("xwiki", "XWiki", "bar"),
                WIKIS_LIST)
        );
    }

    @BeforeEach
    void setup() throws Exception
    {
        XWikiContext context = this.mockitoOldcore.getXWikiContext();
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(WIKIS_LIST);

        // Initialize the mandatory documents in all wikis.
        for (String wikiId : WIKIS_LIST) {
            context.setWikiId(wikiId);
            this.mockitoOldcore.getSpyXWiki().initializeMandatoryDocuments(context);
        }
    }

    @ParameterizedTest
    @MethodSource("provideSourceTargetAndWikis")
    void processOnRenameWithUserOnly(DocumentReference source, DocumentReference target, List<String> wikis)
        throws XWikiException
    {
        when(this.requestFactory.createReplaceUserRequest(source, target)).thenReturn(this.replaceUserRequest);

        createAndSaveDocument(target, true, false);

        List<String> capturedWikisForSearch = captureWikisForSearchDocuments();
        List<String> capturedWikisForGroups = captureWikisForGroups(source, target);

        triggerRenameEvent(source, target);

        assertEquals(wikis, capturedWikisForSearch);
        assertEquals(wikis, capturedWikisForGroups);
        verifyUserReplacementJobCalled();
    }

    @ParameterizedTest
    @MethodSource("provideSourceTargetAndWikis")
    void processOnRenameWithGroupOnly(DocumentReference source, DocumentReference target, List<String> wikis)
        throws XWikiException
    {
        when(this.requestFactory.createReplaceUserRequest(source, target)).thenReturn(this.replaceUserRequest);

        createAndSaveDocument(target, false, true);

        List<String> capturedWikisForSearch = captureWikisForSearchDocuments();
        List<String> capturedWikisForGroups = captureWikisForGroups(source, target);

        triggerRenameEvent(source, target);

        assertEquals(wikis, capturedWikisForSearch);
        assertEquals(wikis, capturedWikisForGroups);

        verifyNoInteractions(this.replaceUserRequest);
        verifyNoInteractions(this.replaceUserJob);
    }

    @ParameterizedTest
    @MethodSource("provideSourceTargetAndWikis")
    void processOnRenameWithUserAndGroup(DocumentReference source, DocumentReference target, List<String> wikis)
        throws XWikiException
    {
        when(this.requestFactory.createReplaceUserRequest(source, target)).thenReturn(this.replaceUserRequest);

        createAndSaveDocument(target, true, true);
        List<String> capturedWikisForSearch = captureWikisForSearchDocuments();
        List<String> capturedWikisForGroups = captureWikisForGroups(source, target);

        triggerRenameEvent(source, target);

        // The search and the group member replacement should be called twice, once for the user object and once for
        // the group.
        assertEquals(wikis, capturedWikisForSearch.subList(0, wikis.size()));
        assertEquals(wikis, capturedWikisForSearch.subList(wikis.size(), wikis.size() * 2));
        assertEquals(wikis, capturedWikisForGroups.subList(0, wikis.size()));
        assertEquals(wikis, capturedWikisForGroups.subList(wikis.size(), wikis.size() * 2));

        verifyUserReplacementJobCalled();
    }

    /**
     * Creates and saves a document with optional user and/or group objects.
     */
    private void createAndSaveDocument(DocumentReference reference, boolean user, boolean group) throws XWikiException
    {
        XWikiContext context = this.mockitoOldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(reference);
        if (user) {
            document.newXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE, context);
        }
        if (group) {
            document.newXObject(XWikiGroupsDocumentInitializer.XWIKI_GROUPS_DOCUMENT_REFERENCE, context);
        }
        this.mockitoOldcore.getSpyXWiki().saveDocument(document, context);
    }

    private List<String> captureWikisForGroups(DocumentReference source, DocumentReference target)
        throws XWikiException
    {
        XWikiContext context = this.mockitoOldcore.getXWikiContext();
        List<String> capturedWikisForGroups = new ArrayList<>();
        doAnswer(invocation -> {
            capturedWikisForGroups.add(context.getWikiId());
            return null;
        }).when(this.mockitoOldcore.getMockGroupService()).replaceMemberInAllGroups(source, target, context);
        return capturedWikisForGroups;
    }

    private List<String> captureWikisForSearchDocuments() throws XWikiException
    {
        XWikiContext context = this.mockitoOldcore.getXWikiContext();
        List<String> capturedWikis = new ArrayList<>();
        doAnswer(invocation -> {
            capturedWikis.add(context.getWikiId());
            return List.of();
        }).when(this.mockitoOldcore.getMockStore()).searchDocuments(any(), any(), any());
        return capturedWikis;
    }

    /**
     * Triggers a document rename event.
     */
    private void triggerRenameEvent(DocumentReference source, DocumentReference target)
    {
        DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(source, target);
        this.listener.onEvent(documentRenamedEvent, null, null);
    }

    /**
     * Verifies that the user replacement job was called with the correct configuration.
     */
    private void verifyUserReplacementJobCalled()
    {
        verify(this.replaceUserRequest).setReplaceDocumentAuthor(true);
        verify(this.replaceUserRequest).setReplaceDocumentContentAuthor(true);
        verify(this.replaceUserRequest).setReplaceDocumentCreator(true);
        verify(this.replaceUserRequest).setCheckAuthorRights(false);
        verify(this.replaceUserJob).initialize(this.replaceUserRequest);
    }
}
