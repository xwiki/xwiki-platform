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
package org.xwiki.wiki.user.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiUserFromWorkspaceMigrationTest
{
    @Rule
    public MockitoComponentMockingRule<WikiUserFromWorkspaceMigration> mocker =
            new MockitoComponentMockingRule(WikiUserFromWorkspaceMigration.class, HibernateDataMigration.class,
                    "R530000WikiUserFromWorkspaceMigration");

    private WikiDescriptorManager wikiDescriptorManager;

    private WikiUserConfigurationHelper wikiUserConfigurationHelper;

    private DocumentRestorerFromAttachedXAR documentRestorerFromAttachedXAR;

    private Execution execution;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        wikiUserConfigurationHelper = mocker.getInstance(WikiUserConfigurationHelper.class);
        documentRestorerFromAttachedXAR = mocker.getInstance(DocumentRestorerFromAttachedXAR.class);
        execution = mock(Execution.class);
        mocker.registerComponent(Execution.class, execution);
        xcontext = mock(XWikiContext.class);
        xwiki = mock(XWiki.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
    }

    @Test
    public void upgradeRegularSubWiki() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerSubwiki")),
                any(XWikiContext.class))).thenReturn(oldDescriptorDocument);
        when(oldDescriptorDocument.getXObject(eq(new DocumentReference("mainWiki", "WorkspaceManager", "WorkspaceClass")
        ))).thenReturn(null);

        // Run
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify
        WikiUserConfiguration expectedConfiguration = new WikiUserConfiguration();
        expectedConfiguration.setUserScope(UserScope.LOCAL_AND_GLOBAL);
        expectedConfiguration.setMembershipType(MembershipType.INVITE);
        verify(wikiUserConfigurationHelper).saveConfiguration(eq(expectedConfiguration), eq("subwiki"));
    }

    @Test
    public void upgradeWorkspace() throws Exception
    {
        // Mocks about the descriptor
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("workspace");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerWorkspace")),
                any(XWikiContext.class))).thenReturn(oldDescriptorDocument);

        // Mocks about the old workspace object
        BaseObject oldObject = mock(BaseObject.class);
        when(oldDescriptorDocument.getXObject(eq(new DocumentReference("mainWiki", "WorkspaceManager", "WorkspaceClass")
        ))).thenReturn(oldObject);
        when(oldObject.getStringValue("membershipType")).thenReturn("request");

        // Mocks about candidacies
        DocumentReference memberGroupRef = new DocumentReference("workspace", XWiki.SYSTEM_SPACE, "XWikiAllGroup");
        XWikiDocument memberGroupDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(memberGroupRef), any(XWikiContext.class))).thenReturn(memberGroupDoc);
        DocumentReference candidacyOldClass = new DocumentReference("workspace", "XWiki",
                "WorkspaceCandidateMemberClass");
        List<BaseObject> oldCandidacies = new ArrayList<BaseObject>();
        BaseObject oldCandidacy = mock(BaseObject.class);
        oldCandidacies.add(oldCandidacy);
        when(memberGroupDoc.getXObjects(eq(candidacyOldClass))).thenReturn(oldCandidacies);
        DocumentReference newCandidacyClassRef = new DocumentReference("workspace",
                WikiCandidateMemberClassInitializer.DOCUMENT_SPACE, WikiCandidateMemberClassInitializer.DOCUMENT_NAME);
        BaseObject newCandidacyObject = mock(BaseObject.class);
        when(memberGroupDoc.newXObject(eq(newCandidacyClassRef), any(XWikiContext.class)))
                .thenReturn(newCandidacyObject);
        when(oldCandidacy.getStringValue("type")).thenReturn("aa");
        when(oldCandidacy.getStringValue("status")).thenReturn("bb");
        when(oldCandidacy.getStringValue("userName")).thenReturn("cc");
        when(oldCandidacy.getLargeStringValue("userComment")).thenReturn("dd");
        when(oldCandidacy.getStringValue("reviewer")).thenReturn("ee");
        when(oldCandidacy.getLargeStringValue("reviewerComment")).thenReturn("ff");
        when(oldCandidacy.getLargeStringValue("reviewerPrivateComment")).thenReturn("gg");
        when(oldCandidacy.getDateValue("date")).thenReturn(new Date(2000));
        when(oldCandidacy.getDateValue("resolutionDate")).thenReturn(new Date(8000));

        // Mocks about the old document to restore form the main wiki
        DocumentReference documentToRestore2 = new DocumentReference("mainWiki", "XWiki", "RegistrationConfig");
        XWikiDocument documentToRestore2FromMainWiki = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(documentToRestore2), any(XWikiContext.class))).
                thenReturn(documentToRestore2FromMainWiki);
        when(xwiki.exists(documentToRestore2, xcontext)).thenReturn(true);

        // Run
        mocker.getComponentUnderTest().hibernateMigrate();

        // Verify the user configuration is accurate
        WikiUserConfiguration expectedConfiguration = new WikiUserConfiguration();
        expectedConfiguration.setUserScope(UserScope.GLOBAL_ONLY);
        expectedConfiguration.setMembershipType(MembershipType.REQUEST);
        verify(wikiUserConfigurationHelper).saveConfiguration(eq(expectedConfiguration), eq("workspace"));

        // Verify the old workspace object has been removed and the descriptor saved
        verify(oldDescriptorDocument).removeXObject(oldObject);
        verify(xwiki, times(1)).saveDocument(oldDescriptorDocument, "Remove the old WorkspaceManager.WorkspaceClass" +
                " object.", xcontext);

        // Verify the candidacy has been upgraded
        verify(newCandidacyObject).setStringValue(WikiCandidateMemberClassInitializer.FIELD_TYPE, "aa");
        verify(newCandidacyObject).setStringValue(WikiCandidateMemberClassInitializer.FIELD_STATUS, "bb");
        verify(newCandidacyObject).setStringValue(WikiCandidateMemberClassInitializer.FIELD_USER, "cc");
        verify(newCandidacyObject).setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_USER_COMMENT, "dd");
        verify(newCandidacyObject).setStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN, "ee");
        verify(newCandidacyObject).setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_COMMENT, "ff");
        verify(newCandidacyObject).setLargeStringValue(WikiCandidateMemberClassInitializer.FIELD_ADMIN_PRIVATE_COMMENT,
                "gg");
        verify(newCandidacyObject).setDateValue(eq(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CREATION),
                eq(new Date(2000)));
        verify(newCandidacyObject).setDateValue(eq(WikiCandidateMemberClassInitializer.FIELD_DATE_OF_CLOSURE),
                eq(new Date(8000)));

        // Verify the old candidacy has been removed and the document saved
        verify(memberGroupDoc).removeXObject(oldCandidacy);
        verify(xwiki, times(1)).saveDocument(memberGroupDoc, "Upgrade candidacies from the old Workspace Application" +
                " to the new Wiki Application.", xcontext);

        // Verify the document to restore has been restored from the xar
        verify(documentRestorerFromAttachedXAR).restoreDocumentFromAttachedXAR(eq(new DocumentReference("mainWiki",
                "WorkspaceManager", "Install")), eq("workspace-template.xar"), any(List.class));

        // Verify the document to restore has been restored from the main wiki
        verify(xwiki).copyDocument(eq(documentToRestore2),
                eq(new DocumentReference("workspace", "XWiki", "RegistrationConfig")), any(XWikiContext.class));
    }

}
