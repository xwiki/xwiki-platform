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
package org.xwiki.repository.script;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.internal.ExtensionStore;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class RepositoryScriptServiceTest
{
    private static final String EXTENSION_ID = "my-extension";

    private static final String PROJECT_ID = "my-project";

    private static final String VERSION = "1.0";

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("wiki", "XWiki", "Author");

    private static final DocumentReference MAIN_DOCUMENT_REFERENCE =
        new DocumentReference("wiki", "Extension", "WebHome");

    private static final DocumentReference VERSION_DOCUMENT_REFERENCE =
        new DocumentReference("wiki", List.of("Extension", "ExtensionVersions", "1.0"), "WebHome");

    @InjectMockComponents
    private RepositoryScriptService scriptService;

    @MockComponent
    private ExtensionStore extensionStore;

    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiDocument mainDocument;

    private XWikiDocument versionDocument;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);

        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        this.mainDocument = mock(XWikiDocument.class, "main");
        when(this.mainDocument.getDocumentReference()).thenReturn(MAIN_DOCUMENT_REFERENCE);

        this.versionDocument = mock(XWikiDocument.class, "version");
        when(this.xwiki.getDocument(VERSION_DOCUMENT_REFERENCE, this.xcontext)).thenReturn(this.versionDocument);
    }

    private void setViewRight(DocumentReference documentReference, boolean allowed)
    {
        when(this.authorization.hasAccess(Right.VIEW, AUTHOR_REFERENCE, documentReference)).thenReturn(allowed);
    }

    private BaseObject setUpExtensionVersion() throws Exception
    {
        when(this.extensionStore.getExistingExtensionDocumentById(EXTENSION_ID)).thenReturn(this.mainDocument);
        when(this.extensionStore.getExtensionVersionDocumentReference(this.mainDocument, VERSION, this.xcontext))
            .thenReturn(VERSION_DOCUMENT_REFERENCE);

        BaseObject versionObject = mock(BaseObject.class);
        when(versionObject.getGuid()).thenReturn("extension-version-guid");
        when(this.extensionStore.getExtensionVersionObject(this.versionDocument, VERSION)).thenReturn(versionObject);

        return versionObject;
    }

    private BaseObject setUpProjectVersion() throws Exception
    {
        when(this.extensionStore.getExistingProjectDocumentById(PROJECT_ID)).thenReturn(this.mainDocument);
        when(this.extensionStore.getProjectVersionDocumentReference(this.mainDocument, VERSION, this.xcontext))
            .thenReturn(VERSION_DOCUMENT_REFERENCE);

        BaseObject versionObject = mock(BaseObject.class);
        when(versionObject.getGuid()).thenReturn("project-version-guid");
        when(this.extensionStore.getProjectVersionObject(this.versionDocument, VERSION)).thenReturn(versionObject);

        return versionObject;
    }

    @Test
    void getVersionObjectWhenAuthorCanView() throws Exception
    {
        setUpExtensionVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);

        Object versionObject = this.scriptService.getVersionObject(EXTENSION_ID, VERSION);

        assertEquals("extension-version-guid", versionObject.getGuid());
    }

    @Test
    void getVersionObjectWhenAuthorCannotViewVersionDocument() throws Exception
    {
        setUpExtensionVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, false);

        assertNull(this.scriptService.getVersionObject(EXTENSION_ID, VERSION));

        // The version document must not even be loaded when the author is not allowed to view it.
        verify(this.xwiki, never()).getDocument(VERSION_DOCUMENT_REFERENCE, this.xcontext);
        verify(this.extensionStore, never()).getExtensionVersionObject(this.versionDocument, VERSION);
    }

    @Test
    void getVersionObjectWhenAuthorCannotViewExtensionDocument() throws Exception
    {
        setUpExtensionVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, false);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);

        assertNull(this.scriptService.getVersionObject(EXTENSION_ID, VERSION));

        verify(this.xwiki, never()).getDocument(VERSION_DOCUMENT_REFERENCE, this.xcontext);
    }

    @Test
    void getVersionObjectWhenUnknownExtension() throws Exception
    {
        when(this.extensionStore.getExistingExtensionDocumentById(EXTENSION_ID)).thenReturn(null);

        assertNull(this.scriptService.getVersionObject(EXTENSION_ID, VERSION));
    }

    @Test
    void getVersionObjectWhenNoVersionObject() throws Exception
    {
        setUpExtensionVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);
        when(this.extensionStore.getExtensionVersionObject(this.versionDocument, VERSION)).thenReturn(null);

        assertNull(this.scriptService.getVersionObject(EXTENSION_ID, VERSION));
    }

    @Test
    void getProjectVersionObjectWhenAuthorCanView() throws Exception
    {
        setUpProjectVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);

        Object versionObject = this.scriptService.getProjectVersionObject(PROJECT_ID, VERSION);

        assertEquals("project-version-guid", versionObject.getGuid());
    }

    @Test
    void getProjectVersionObjectWhenAuthorCannotViewVersionDocument() throws Exception
    {
        setUpProjectVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, false);

        assertNull(this.scriptService.getProjectVersionObject(PROJECT_ID, VERSION));

        // The version document must not even be loaded when the author is not allowed to view it.
        verify(this.xwiki, never()).getDocument(VERSION_DOCUMENT_REFERENCE, this.xcontext);
        verify(this.extensionStore, never()).getProjectVersionObject(this.versionDocument, VERSION);
    }

    @Test
    void getProjectVersionObjectWhenAuthorCannotViewProjectDocument() throws Exception
    {
        setUpProjectVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, false);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);

        assertNull(this.scriptService.getProjectVersionObject(PROJECT_ID, VERSION));

        verify(this.xwiki, never()).getDocument(VERSION_DOCUMENT_REFERENCE, this.xcontext);
    }

    @Test
    void getProjectVersionObjectWhenUnknownProject() throws Exception
    {
        when(this.extensionStore.getExistingProjectDocumentById(PROJECT_ID)).thenReturn(null);

        assertNull(this.scriptService.getProjectVersionObject(PROJECT_ID, VERSION));
    }

    @Test
    void getProjectVersionObjectWhenNoVersionObject() throws Exception
    {
        setUpProjectVersion();
        setViewRight(MAIN_DOCUMENT_REFERENCE, true);
        setViewRight(VERSION_DOCUMENT_REFERENCE, true);
        when(this.extensionStore.getProjectVersionObject(this.versionDocument, VERSION)).thenReturn(null);

        assertNull(this.scriptService.getProjectVersionObject(PROJECT_ID, VERSION));
    }
}
