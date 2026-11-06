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
package org.xwiki.edit.internal;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.script.SimpleScriptContext;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.edit.EditException;
import org.xwiki.edit.EditorDescriptorBuilder;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EditorWikiComponent}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList(EditorClassDocumentInitializer.class)
@ReferenceComponentList
class EditorWikiComponentTest
{
    private static final DocumentReference EDITOR_REFERENCE = new DocumentReference("xwiki", "Space", "Editor");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Author");

    private static final String CODE = "editor code";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private EditorDescriptorBuilder descriptorBuilder;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    @Named("document")
    private SheetBinder documentSheetBinder;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @InjectMockComponents
    private EditorWikiComponent<SyntaxContent> editor;

    private XWikiDocument editorDocument;

    private final XWikiDocument currentDocument = mock();

    private final UserReference author = mock();

    private final UserReference contentAuthor = mock();

    private boolean executedAsAuthor;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        this.oldcore.getSpyXWiki().initializeMandatoryDocuments(xcontext);

        // The editor document content is written by a first user.
        XWikiDocument document = new XWikiDocument(EDITOR_REFERENCE);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent("content");
        document.getAuthors().setEffectiveMetadataAuthor(this.contentAuthor);
        document.getAuthors().setOriginalMetadataAuthor(this.contentAuthor);
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);

        // Then another user only adds the editor object, which doesn't change the content author.
        document = this.oldcore.getSpyXWiki().getDocument(EDITOR_REFERENCE, xcontext).clone();
        BaseObject editorObject = document.newXObject(EditorClassDocumentInitializer.REFERENCE, xcontext);
        editorObject.setStringValue("roleHint", "test");
        editorObject.setStringValue("scope", "wiki");
        editorObject.setStringValue("dataType", SyntaxContent.class.getName());
        editorObject.setLargeStringValue("code", CODE);
        document.getAuthors().setEffectiveMetadataAuthor(this.author);
        document.getAuthors().setOriginalMetadataAuthor(this.author);
        this.oldcore.getSpyXWiki().saveDocument(document, xcontext);

        this.editorDocument = this.oldcore.getSpyXWiki().getDocument(EDITOR_REFERENCE, xcontext);
        assertEquals(this.contentAuthor, this.editorDocument.getAuthors().getContentAuthor());
        assertEquals(this.author, this.editorDocument.getAuthors().getEffectiveMetadataAuthor());
        when(this.userReferenceSerializer.serialize(this.author)).thenReturn(AUTHOR_REFERENCE);

        xcontext.setDoc(this.currentDocument);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(new SimpleScriptContext());

        when(this.authorExecutor.call(any(), any(), any())).then(invocation -> {
            this.executedAsAuthor = true;
            try {
                return invocation.<Callable<?>>getArgument(0).call();
            } finally {
                this.executedAsAuthor = false;
            }
        });
        when(this.currentDocument.getRenderedContent(CODE, Syntax.XWIKI_2_1.toIdString(), false, null, xcontext))
            .then(invocation -> this.executedAsAuthor ? "rendered" : "wrong author");
    }

    @Test
    void initialize() throws Exception
    {
        this.editor.initialize(EDITOR_REFERENCE);

        assertEquals(EDITOR_REFERENCE, this.editor.getDocumentReference());
        assertEquals(AUTHOR_REFERENCE, this.editor.getAuthorReference());
        assertEquals(WikiComponentScope.WIKI, this.editor.getScope());
        verify(this.descriptorBuilder).setId("test");
    }

    @Test
    void renderWithTheMetadataAuthor() throws Exception
    {
        this.editor.initialize(EDITOR_REFERENCE);

        assertEquals("rendered", this.editor.render(mock(), Map.of()));

        // The code is executed with the rights of the effective metadata author of the editor document and not with
        // those of its content author, and the secure document set by the author executor isn't overwritten.
        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(EDITOR_REFERENCE));
        verify(this.currentDocument, never()).getRenderedContent(any(String.class), any(String.class), eq(false),
            eq(this.editorDocument), any());
    }

    @Test
    void renderUsesTheAuthorOfTheCurrentEditorDocument() throws Exception
    {
        this.editor.initialize(EDITOR_REFERENCE);

        // The editor document has been modified by another user since the editor was registered.
        UserReference otherAuthor = mock();
        DocumentReference otherAuthorReference = new DocumentReference("xwiki", "XWiki", "Other");
        this.editorDocument.getAuthors().setEffectiveMetadataAuthor(otherAuthor);
        when(this.userReferenceSerializer.serialize(otherAuthor)).thenReturn(otherAuthorReference);

        assertEquals("rendered", this.editor.render(mock(), Map.of()));

        verify(this.authorExecutor).call(any(), eq(otherAuthorReference), eq(EDITOR_REFERENCE));
    }

    @Test
    void renderWithGuestAuthor() throws Exception
    {
        this.editorDocument.getAuthors().setEffectiveMetadataAuthor(GuestUserReference.INSTANCE);
        // Like the real serializer, which represents the guest user with a null document reference.
        when(this.userReferenceSerializer.serialize(GuestUserReference.INSTANCE)).thenReturn(null);
        this.editor.initialize(EDITOR_REFERENCE);

        assertNull(this.editor.getAuthorReference());
        assertEquals("rendered", this.editor.render(mock(), Map.of()));

        verify(this.authorExecutor).call(any(), isNull(), eq(EDITOR_REFERENCE));
        // A null user reference would be resolved to the current user.
        verify(this.userReferenceSerializer, never()).serialize(isNull());
    }

    @Test
    void renderWithoutEditorObject() throws Exception
    {
        this.editor.initialize(EDITOR_REFERENCE);
        this.editorDocument.removeXObjects(EditorClassDocumentInitializer.REFERENCE);

        EditException exception = assertThrows(EditException.class, () -> this.editor.render(mock(), Map.of()));

        assertTrue(exception.getMessage().contains("is missing the XWiki.EditorClass object"));
        verify(this.authorExecutor, never()).call(any(), any(), any());
    }
}
