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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultDocumentRestorerFromAttachedXAR}.
 *
 * @since 5.3RC1
 */
public class DefaultDocumentRestorerFromAttachedXARTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultDocumentRestorerFromAttachedXAR> mocker =
            new MockitoComponentMockingRule(DefaultDocumentRestorerFromAttachedXAR.class);

    private Provider<XWikiContext> xcontextProvider;

    private XWikiContext xcontext;

    private com.xpn.xwiki.XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        xcontextProvider = mocker.getInstance(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void restoreDocumentFromAttachedXAR() throws Exception
    {
        // Mocks
        XWikiDocument workspaceInstallDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(new DocumentReference("mainWiki", "WorkspaceManager", "Install")),
                any(XWikiContext.class))).thenReturn(workspaceInstallDoc);
        XWikiAttachment xeXar = mock(XWikiAttachment.class);
        when(workspaceInstallDoc.getAttachment("workspace-template.xar")).thenReturn(xeXar);
        when(xeXar.getContentInputStream(any(XWikiContext.class))).thenReturn(
                getClass().getResourceAsStream("/test-restore-documents.xar"));
        XWikiDocument docToRestore1 = mock(XWikiDocument.class);
        DocumentReference docToRestoreRef1 = new DocumentReference("workspace", "XWiki", "AdminRegistrationSheet");
        when(xwiki.getDocument(eq(docToRestoreRef1), any(XWikiContext.class))).thenReturn(docToRestore1);
        XWikiDocument docToRestore2 = mock(XWikiDocument.class);
        DocumentReference docToRestoreRef2 = new DocumentReference("workspace", "XWiki", "RegistrationHelp");
        when(xwiki.getDocument(eq(docToRestoreRef2), any(XWikiContext.class))).thenReturn(docToRestore2);

        // Inputs
        List<DocumentReference> docsToRestore = new ArrayList<DocumentReference>();
        docsToRestore.add(docToRestoreRef1);
        docsToRestore.add(docToRestoreRef2);

        // Run
        mocker.getComponentUnderTest().restoreDocumentFromAttachedXAR(new DocumentReference("mainWiki",
                "WorkspaceManager", "Install"), "workspace-template.xar", docsToRestore);

        // Verify the document to restore has been restored from the xar
        verify(docToRestore1).fromXML(any(InputStream.class));
        verify(xwiki, times(1)).saveDocument(docToRestore1, xcontext);
        verify(docToRestore2).fromXML(any(InputStream.class));
        verify(xwiki, times(1)).saveDocument(docToRestore2, xcontext);
    }
}
