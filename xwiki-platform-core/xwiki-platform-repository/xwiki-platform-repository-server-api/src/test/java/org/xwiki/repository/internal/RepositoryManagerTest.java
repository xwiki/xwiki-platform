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

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

        XWikiDocument existingDocument = mock(XWikiDocument.class);
        XWikiDocument clonedDocument = mock(XWikiDocument.class);
        DocumentReference extensionReference =
            new DocumentReference("xwiki", List.of("Extension", "MyExt"), "WebHome");
        when(this.extensionStore.getExistingExtensionDocumentById(extensionIdString)).thenReturn(existingDocument);
        when(existingDocument.clone()).thenReturn(clonedDocument);
        when(clonedDocument.getDocumentReference()).thenReturn(extensionReference);

        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.EDIT, extensionReference);

        assertThrows(AccessDeniedException.class,
            () -> this.repositoryManager.importExtension(extensionIdString, repository, STABLE));

        // The edit right must be checked on the imported document before performing any side effect on the wiki.
        verify(this.authorization).checkAccess(Right.EDIT, extensionReference);
        verify(xwiki, never()).saveDocument(any(XWikiDocument.class), anyString(), any(XWikiContext.class));
        verify(xwiki, never()).deleteDocument(any(XWikiDocument.class), any(XWikiContext.class));
    }
}
