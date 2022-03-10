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

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.ReplaceUserRequest;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests about {@link UpdateRightsOnDocumentRenameListener}.
 *
 * @version $Id$
 * @since 11.9RC1
 */
@OldcoreTest
class UpdateRightsOnDocumentRenameListenerTest
{
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

    @Mock
    private XWikiDocument targetUserDocument;

    @Mock
    private ReplaceUserRequest replaceUserRequest;

    @MockComponent
    @Named("default")
    private EntityReferenceResolver<String> stringEntityReferenceResolver;

    private List<String> listWikiIds = Arrays.asList("wiki", "subwiki");

    private DocumentReference userClassDocumentReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
    private DocumentReference groupClassDocumentReference = new DocumentReference("xwiki", "XWiki", "XWikiGroups");

    private DocumentReference sourceUserDocumentReference = new DocumentReference("xwiki", "XWiki", "foo");
    private DocumentReference targetUserDocumentReference = new DocumentReference("xwiki", "XWiki", "bar");

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        XWikiContext context = this.mockitoOldcore.getXWikiContext();
        when(context.getWiki().getDocument(targetUserDocumentReference, context)).thenReturn(targetUserDocument);
        when(wikiDescriptorManager.getAllIds()).thenReturn(listWikiIds);
        when(stringEntityReferenceResolver.resolve("XWiki.XWikiUsers", EntityType.DOCUMENT))
            .thenReturn(userClassDocumentReference);
        when(stringEntityReferenceResolver.resolve("XWiki.XWikiGroups", EntityType.DOCUMENT))
            .thenReturn(groupClassDocumentReference);
        when(requestFactory.createReplaceUserRequest(sourceUserDocumentReference, targetUserDocumentReference))
            .thenReturn(replaceUserRequest);

        // Needed for RightsManager
        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        EntityReferenceSerializer<String> mockLocalSerializer
            = componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");

        when(mockLocalSerializer.serialize(new DocumentReference("xwiki", "XWiki", "XWikiRights")))
            .thenReturn("XWiki.XWikiRights");
        when(mockLocalSerializer.serialize(new DocumentReference("xwiki", "XWiki", "XWikiGlobalRights")))
            .thenReturn("XWiki.XWikiGlobalRights");
    }

    @Test
    void processOnRenameWithUserOnly() throws XWikiException
    {
        when(targetUserDocument.getXObject((EntityReference) userClassDocumentReference)).thenReturn(new BaseObject());
        DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(sourceUserDocumentReference,
            targetUserDocumentReference);

        listener.onEvent(documentRenamedEvent, null, null);

        // the update of rights and groups should be done twice since we have 2 wikis
        verify(mockitoOldcore.getMockStore(), times(2)).searchDocuments(any(), any(), any());
        verify(mockitoOldcore.getMockGroupService(), times(2)).replaceMemberInAllGroups(sourceUserDocumentReference,
            targetUserDocumentReference, mockitoOldcore.getXWikiContext());

        // the job for user replacement should be done once
        verify(replaceUserRequest, times(1)).setReplaceDocumentAuthor(true);
        verify(replaceUserRequest, times(1)).setReplaceDocumentContentAuthor(true);
        verify(replaceUserRequest, times(1)).setReplaceDocumentCreator(true);
        verify(replaceUserRequest, times(1)).setCheckAuthorRights(false);
        verify(replaceUserJob, times(1)).initialize(replaceUserRequest);
    }

    @Test
    void processOnRenameWithGroupOnly() throws XWikiException
    {
        when(targetUserDocument.getXObject((EntityReference) groupClassDocumentReference)).thenReturn(new BaseObject());
        DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(sourceUserDocumentReference,
            targetUserDocumentReference);

        listener.onEvent(documentRenamedEvent, null, null);

        // the update of rights and groups should be done twice since we have 2 wikis
        verify(mockitoOldcore.getMockStore(), times(2)).searchDocuments(any(), any(), any());
        verify(mockitoOldcore.getMockGroupService(), times(2)).replaceMemberInAllGroups(sourceUserDocumentReference,
            targetUserDocumentReference, mockitoOldcore.getXWikiContext());

        // the job for user replacement should not be called
        verify(replaceUserRequest, never()).setReplaceDocumentAuthor(true);
        verify(replaceUserRequest, never()).setReplaceDocumentContentAuthor(true);
        verify(replaceUserRequest, never()).setReplaceDocumentCreator(true);
        verify(replaceUserRequest, never()).setCheckAuthorRights(false);
        verify(replaceUserJob, never()).initialize(replaceUserRequest);
    }

    @Test
    void processOnRenameWithUserAndGroup() throws XWikiException
    {
        when(targetUserDocument.getXObject((EntityReference) userClassDocumentReference)).thenReturn(new BaseObject());
        when(targetUserDocument.getXObject((EntityReference) groupClassDocumentReference)).thenReturn(new BaseObject());
        DocumentRenamedEvent documentRenamedEvent = new DocumentRenamedEvent(sourceUserDocumentReference,
            targetUserDocumentReference);

        listener.onEvent(documentRenamedEvent, null, null);

        // the update of rights and groups should be done twice since we have 2 wikis, but for both user and groups
        // so 4 times in total
        verify(mockitoOldcore.getMockStore(), times(4)).searchDocuments(any(), any(), any());
        verify(mockitoOldcore.getMockGroupService(), times(4)).replaceMemberInAllGroups(sourceUserDocumentReference,
            targetUserDocumentReference, mockitoOldcore.getXWikiContext());

        // the job for user replacement should be called once only for users
        verify(replaceUserRequest, times(1)).setReplaceDocumentAuthor(true);
        verify(replaceUserRequest, times(1)).setReplaceDocumentContentAuthor(true);
        verify(replaceUserRequest, times(1)).setReplaceDocumentCreator(true);
        verify(replaceUserRequest, times(1)).setCheckAuthorRights(false);
        verify(replaceUserJob, times(1)).initialize(replaceUserRequest);
    }
}
