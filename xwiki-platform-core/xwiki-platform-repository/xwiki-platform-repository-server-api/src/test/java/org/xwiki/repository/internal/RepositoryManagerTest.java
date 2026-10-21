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
package org.xwiki.repository.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.extension.version.Version.Type.STABLE;

/**
 * Unit tests for {@link RepositoryManager}.
 *
 * @version $Id$
 */
@ComponentTest
class RepositoryManagerTest
{
    @InjectMockComponents
    private RepositoryManager repositoryManager;

    @MockComponent
    private ExtensionStore extensionStore;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private QueryManager queryManager;

    @Test
    void importExtensionChecksEditRightBeforeAnySideEffect() throws Exception
    {
        String extensionIdString = "my-ext";
        ExtensionId extensionId = new ExtensionId(extensionIdString, new DefaultVersion("1.0"));

        ExtensionRepository repository = mock(ExtensionRepository.class);
        when(repository.resolveVersions(extensionIdString, 0, -1))
            .thenReturn(new CollectionIterableResult<>(1, 0, List.of(extensionId.getVersion())));
        Extension extension = mock(Extension.class);
        when(extension.getId()).thenReturn(extensionId);
        when(repository.resolve(extensionId)).thenReturn(extension);

        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);

        DocumentReference extensionReference =
            new DocumentReference("xwiki", List.of("Extension", "MyExt"), "WebHome");
        when(this.extensionStore.getExistingExtensionDocumentReferenceById(extensionIdString))
            .thenReturn(extensionReference);

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.EDIT, extensionReference);

        assertThrows(AccessDeniedException.class,
            () -> this.repositoryManager.importExtension(extensionIdString, repository, STABLE));

        // The edit right must be checked on the imported document before performing any side effect on the wiki.
        verify(this.authorization).checkAccess(Right.EDIT, extensionReference);
        verify(xwiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
        verify(xwiki, never()).deleteDocument(any(XWikiDocument.class), any(XWikiContext.class));
    }

    @Test
    void importExtensionIgnoresLegacyVersionWithBlankVersion() throws Exception
    {
        String extensionIdString = "my-ext";
        ExtensionId extensionId = new ExtensionId(extensionIdString, new DefaultVersion("1.0"));

        ExtensionRepository repository = mock(ExtensionRepository.class);
        when(repository.resolveVersions(extensionIdString, 0, -1))
            .thenReturn(new CollectionIterableResult<>(1, 0, List.of(extensionId.getVersion())));
        Extension extension = mock(Extension.class);
        when(extension.getId()).thenReturn(extensionId);
        when(repository.resolve(extensionId)).thenReturn(extension);

        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xwiki);

        DocumentReference extensionReference =
            new DocumentReference("xwiki", List.of("Extension", "MyExt"), "WebHome");
        when(this.extensionStore.getExistingExtensionDocumentReferenceById(extensionIdString))
            .thenReturn(extensionReference);

        // A legacy extension page, using dedicated version pages, which still holds version objects in the main page
        XWikiDocument extensionDocument = mock(XWikiDocument.class);
        when(extensionDocument.getDocumentReference()).thenReturn(extensionReference);
        when(extensionDocument.getPageReference()).thenReturn(new PageReference("xwiki", "Extension", "MyExt"));
        when(xwiki.getDocument(extensionReference, xcontext)).thenReturn(extensionDocument);
        when(xwiki.getDocument(any(PageReference.class), eq(xcontext))).thenReturn(mock(XWikiDocument.class));
        BaseObject extensionObject = mock(BaseObject.class);
        when(this.extensionStore.getExtensionObject(extensionDocument)).thenReturn(extensionObject);
        when(this.extensionStore.isVersionPageEnabled(extensionObject)).thenReturn(true);

        BaseObject blankVersionObject = mock(BaseObject.class);
        when(blankVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION)).thenReturn("");
        BaseObject whitespaceVersionObject = mock(BaseObject.class);
        when(whitespaceVersionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION)).thenReturn(" ");
        BaseObject versionObject = mock(BaseObject.class);
        when(versionObject.getStringValue(XWikiRepositoryModel.PROP_VERSION_VERSION)).thenReturn("0.9");
        when(this.extensionStore.getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION))
            .thenReturn("0.9");
        BaseObject versionObjectClone = mock(BaseObject.class);
        when(versionObject.clone()).thenReturn(versionObjectClone);
        List<BaseObject> versionObjects = new ArrayList<>();
        versionObjects.add(blankVersionObject);
        versionObjects.add(null);
        versionObjects.add(whitespaceVersionObject);
        versionObjects.add(versionObject);
        when(extensionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE))
            .thenReturn(versionObjects);

        XWikiDocument versionDocument = mock(XWikiDocument.class);
        XWikiDocument versionDocumentClone = mock(XWikiDocument.class);
        when(versionDocument.clone()).thenReturn(versionDocumentClone);
        when(this.extensionStore.getExtensionVersionDocument(extensionDocument, new DefaultVersion("0.9"), xcontext))
            .thenReturn(versionDocument);

        // Interrupt the import right after the legacy versions migration, when looking for the existing version pages
        when(this.queryManager.createQuery(anyString(), anyString())).thenThrow(QueryException.class);

        assertThrows(QueryException.class,
            () -> this.repositoryManager.importExtension(extensionIdString, repository, STABLE));

        // Only the version object with an actual version is moved to a dedicated version page.
        verify(this.extensionStore, times(1)).getExtensionVersionDocument(any(XWikiDocument.class),
            any(Version.class), any(XWikiContext.class));
        verify(versionDocumentClone).addXObject(versionObjectClone);
        verify(xwiki).saveDocument(versionDocumentClone, "Migrate the extension version", xcontext);
        verify(blankVersionObject, never()).clone();
        verify(whitespaceVersionObject, never()).clone();
    }
}
