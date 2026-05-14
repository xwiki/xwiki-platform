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
package org.xwiki.wiki.workspacesmigrator.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class WorkspaceMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private WorkspacesMigration workspacesMigration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentRestorerFromAttachedXAR documentRestorerFromAttachedXAR;

    @MockComponent
    private Execution execution;

    @MockComponent
    private Logger logger;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        this.xcontext = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
    }

    @Test
    void upgradeWorkspace() throws Exception
    {
        // Mocks about the descriptor
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("workspace");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(
            eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerWorkspace")),
            any(XWikiContext.class))).thenReturn(oldDescriptorDocument);

        // Mocks about the old workspace object
        BaseObject oldWorkspaceObject = mock(BaseObject.class);
        when(oldDescriptorDocument.getXObject(
            eq(new DocumentReference("mainWiki", "WorkspaceManager", "WorkspaceClass"))))
            .thenReturn(oldWorkspaceObject);

        // Mocks about the old document to restore form the main wiki
        DocumentReference documentToRestore2 = new DocumentReference("mainWiki", "XWiki", "RegistrationConfig");
        XWikiDocument documentToRestore2FromMainWiki = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(documentToRestore2), any(XWikiContext.class)))
            .thenReturn(documentToRestore2FromMainWiki);
        when(this.xwiki.exists(documentToRestore2, this.xcontext)).thenReturn(true);

        // Run
        this.workspacesMigration.hibernateMigrate();

        // Verify we try to restore the documents from the xar
        verify(this.documentRestorerFromAttachedXAR).restoreDocumentFromAttachedXAR(
            eq(new DocumentReference("mainWiki", "WorkspaceManager", "Install")), eq("workspace-template.xar"),
            any(List.class));

        // Verify the document to restore has been restored from the main wiki
        verify(this.xwiki).copyDocument(eq(documentToRestore2),
            eq(new DocumentReference("workspace", "XWiki", "RegistrationConfig")), any(XWikiContext.class));

        // Verify that the log contains a warning about the documents that the migration failed to restore
        verify(this.logger).warn("Failed to restore some documents: [{}]. You should import manually "
            + "(1) xwiki-platform-administration-ui.xar and then (2) xwiki-platform-wiki-ui-wiki.xar into your"
            + " wiki, to restore these documents.", "workspace:XWiki.AdminRegistrationSheet, "
            + "workspace:XWiki.RegistrationHelp, workspace:XWiki.AdminUsersSheet");
    }

    @Test
    void upgradeWorkspaceTemplate() throws Exception
    {
        // Mocks about the descriptor
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("workspacetemplate");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(
            eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerWorkspacetemplate")),
            any(XWikiContext.class))).thenReturn(oldDescriptorDocument);

        // Mock that the workspace special page exists
        DocumentReference workspacePageReference = new DocumentReference("workspacetemplate", "XWiki",
            "ManageWorkspace");
        when(this.xwiki.exists(eq(workspacePageReference), any(XWikiContext.class))).thenReturn(true);

        // Run
        this.workspacesMigration.hibernateMigrate();

        // Verify that the log contains a warning about the documents that the migration failed to restore
        verify(this.logger).warn("Failed to restore some documents: [{}]. You should import manually "
            + "(1) xwiki-platform-administration-ui.xar and then (2) xwiki-platform-wiki-ui-wiki.xar into your"
            + " wiki, to restore these documents.", "workspacetemplate:XWiki.AdminRegistrationSheet, "
            + "workspacetemplate:XWiki.RegistrationConfig, workspacetemplate:XWiki.RegistrationHelp, "
            + "workspacetemplate:XWiki.AdminUsersSheet");
    }

    @Test
    void upgradeRegularSubwiki() throws Exception
    {
        // Mocks about the descriptor
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(
            eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerSubwiki")),
            any(XWikiContext.class))).thenReturn(oldDescriptorDocument);

        // Run
        this.workspacesMigration.hibernateMigrate();

        // Verify that the migration did not try to restore old documents
        verify(this.xwiki, never()).exists(eq(new DocumentReference("subwiki", "XWiki", "AdminRegistrationSheet")),
            any(XWikiContext.class));
    }

    @Test
    void errorWhenRestoringFromXAR() throws Exception
    {
        // Mocks about the descriptor
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("workspace");
        XWikiDocument oldDescriptorDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(
            eq(new DocumentReference("mainWiki", XWiki.SYSTEM_SPACE, "XWikiServerWorkspace")),
            any(XWikiContext.class))).thenReturn(oldDescriptorDocument);
        // Mocks about the old workspace object
        BaseObject oldWorkspaceObject = mock(BaseObject.class);
        when(oldDescriptorDocument.getXObject(
            eq(new DocumentReference("mainWiki", "WorkspaceManager", "WorkspaceClass"))))
            .thenReturn(oldWorkspaceObject);

        doThrow(new XWikiException()).when(this.documentRestorerFromAttachedXAR).restoreDocumentFromAttachedXAR(
            any(DocumentReference.class), any(String.class), any(List.class));
        // Run
        this.workspacesMigration.hibernateMigrate();

        // Verify
        verify(this.logger).error(eq("Error while restoring documents from the Workspace XAR"),
            any(XWikiException.class));
    }
}
