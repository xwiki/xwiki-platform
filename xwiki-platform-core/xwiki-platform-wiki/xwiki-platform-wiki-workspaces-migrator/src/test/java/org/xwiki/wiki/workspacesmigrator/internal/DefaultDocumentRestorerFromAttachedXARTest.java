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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultDocumentRestorerFromAttachedXAR}.
 *
 * @since 5.3RC1
 */
@ComponentTest
class DefaultDocumentRestorerFromAttachedXARTest
{
    @InjectMockComponents
    private DefaultDocumentRestorerFromAttachedXAR documentRestorer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Logger logger;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    private XWikiDocument docToRestore1;

    private XWikiDocument docToRestore2;

    private List<DocumentReference> docsToRestore;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        // Inputs
        this.docToRestore1 = mock(XWikiDocument.class);
        DocumentReference docToRestoreRef1 = new DocumentReference("workspace", "XWiki", "AdminRegistrationSheet");
        when(this.xwiki.getDocument(eq(docToRestoreRef1), any(XWikiContext.class))).thenReturn(this.docToRestore1);
        this.docToRestore2 = mock(XWikiDocument.class);
        DocumentReference docToRestoreRef2 = new DocumentReference("workspace", "XWiki", "RegistrationHelp");
        when(this.xwiki.getDocument(eq(docToRestoreRef2), any(XWikiContext.class))).thenReturn(this.docToRestore2);
        this.docsToRestore = new ArrayList<>();
        this.docsToRestore.add(docToRestoreRef1);
        this.docsToRestore.add(docToRestoreRef2);
    }

    @Test
    void restoreDocumentFromAttachedXAR() throws Exception
    {
        // Mocks
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(eq(new DocumentReference("mainWiki", "WorkspaceManager", "Install")),
            any(XWikiContext.class))).thenReturn(workspaceInstallDoc);
        XWikiAttachment xeXar = mock(XWikiAttachment.class);
        when(workspaceInstallDoc.getAttachment("workspace-template.xar")).thenReturn(xeXar);
        when(xeXar.getContentInputStream(any(XWikiContext.class))).thenReturn(
            getClass().getResourceAsStream("/test-restore-documents.xar"));

        // Run
        this.documentRestorer.restoreDocumentFromAttachedXAR(
            new DocumentReference("mainWiki", "WorkspaceManager", "Install"), "workspace-template.xar",
            this.docsToRestore);

        // Verify the document to restore has been restored from the xar
        verify(this.docToRestore1).fromXML(any(InputStream.class));
        verify(this.xwiki).saveDocument(this.docToRestore1, this.xcontext);
        verify(this.docToRestore2).fromXML(any(InputStream.class));
        verify(this.xwiki).saveDocument(this.docToRestore2, this.xcontext);
    }

    @Test
    void errorWhenNoDocument() throws Exception
    {
        // Mocks
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        DocumentReference workspaceInstallDocRef = new DocumentReference("mainWiki", "WorkspaceManager", "Install");
        when(this.xwiki.getDocument(eq(workspaceInstallDocRef), any(XWikiContext.class)))
            .thenReturn(workspaceInstallDoc);
        when(workspaceInstallDoc.isNew()).thenReturn(true);

        // Run
        this.documentRestorer.restoreDocumentFromAttachedXAR(
            new DocumentReference("mainWiki", "WorkspaceManager", "Install"), "workspace-template.xar",
            this.docsToRestore);

        // Verify
        verify(this.logger).warn("[{}] does not exist", workspaceInstallDocRef);
    }

    @Test
    void errorWhenNoAttachment() throws Exception
    {
        // Mocks
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        DocumentReference workspaceInstallDocRef = new DocumentReference("mainWiki", "WorkspaceManager", "Install");
        when(this.xwiki.getDocument(eq(workspaceInstallDocRef), any(XWikiContext.class)))
            .thenReturn(workspaceInstallDoc);
        when(workspaceInstallDoc.getAttachment("workspace-template.xar")).thenReturn(null);

        // Run
        this.documentRestorer.restoreDocumentFromAttachedXAR(
            new DocumentReference("mainWiki", "WorkspaceManager", "Install"), "workspace-template.xar",
            this.docsToRestore);

        // Verify
        verify(this.logger).warn("[{}] has no attachment named [{}].", workspaceInstallDocRef,
            "workspace-template.xar");
    }

    @Test
    void errorZipInvalid() throws Exception
    {
        // Mocks
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        DocumentReference workspaceInstallDocRef = new DocumentReference("mainWiki", "WorkspaceManager", "Install");
        when(this.xwiki.getDocument(eq(workspaceInstallDocRef), any(XWikiContext.class)))
            .thenReturn(workspaceInstallDoc);
        XWikiAttachment xeXar = mock(XWikiAttachment.class);
        when(workspaceInstallDoc.getAttachment("workspace-template.xar")).thenReturn(xeXar);
        when(xeXar.getContentInputStream(any(XWikiContext.class))).thenReturn(
            getClass().getResourceAsStream("/invalid-xar.xar"));

        // Run
        this.documentRestorer.restoreDocumentFromAttachedXAR(
            new DocumentReference("mainWiki", "WorkspaceManager", "Install"), "workspace-template.xar",
            this.docsToRestore);

        // Verify
        verify(this.logger).error(eq("Error during the decompression of [{}]."), eq("workspace-template.xar"),
            any(IOException.class));
    }
}
