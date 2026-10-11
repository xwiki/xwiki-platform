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
package org.xwiki.internal.document.lock;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.LockContext.EditMode;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.edit.EditorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.SyntaxContent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.edit.EditModeResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LockContext}.
 *
 * @version $Id$
 */
class DefaultLockContextTest
{
    private final XWikiDocument document = mock(XWikiDocument.class);

    private final EditModeResolver editModeResolver = mock(EditModeResolver.class);

    private final EditorManager editorManager = mock(EditorManager.class);

    private final LockContext lockContext =
        new DefaultLockContext(this.document, this.editModeResolver, this.editorManager);

    @SuppressWarnings("unchecked")
    private Editor<SyntaxContent> blockNoteEditor = mock(Editor.class, "blockNote");

    @SuppressWarnings("unchecked")
    private Editor<SyntaxContent> textEditor = mock(Editor.class, "text");

    @BeforeEach
    void setUp()
    {
        EditorDescriptor descriptor = mock(EditorDescriptor.class, "blockNote");
        when(this.blockNoteEditor.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getId()).thenReturn("blocknote");

        descriptor = mock(EditorDescriptor.class, "text");
        when(this.textEditor.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getId()).thenReturn("text");
    }

    @Test
    void getDocumentReferenceWithLocale()
    {
        DocumentReference documentReferenceWithLocale = mock(DocumentReference.class);
        when(this.document.getDocumentReferenceWithLocale()).thenReturn(documentReferenceWithLocale);
        assertSame(documentReferenceWithLocale, this.lockContext.getDocumentReferenceWithLocale());
        // The edit mode is resolved only when it is actually needed.
        verifyNoInteractions(this.editModeResolver);
    }

    @Test
    void getRealLocale()
    {
        when(this.document.getRealLocale()).thenReturn(Locale.FRENCH);
        assertEquals(Locale.FRENCH, this.lockContext.getRealLocale());
        // The edit mode is resolved only when it is actually needed.
        verifyNoInteractions(this.editModeResolver);
    }

    @Test
    void getEditModeIsResolvedOnlyOnce()
    {
        when(this.editModeResolver.getEditMode()).thenReturn(EditModeResolver.INPLACE);

        assertEquals(EditMode.INPLACE, this.lockContext.getEditMode());
        assertEquals(EditMode.INPLACE, this.lockContext.getEditMode());

        verify(this.editModeResolver, times(1)).getEditMode();
    }

    @Test
    void getEditModeWhenUnknown()
    {
        when(this.editModeResolver.getEditMode()).thenReturn("object");

        assertNull(this.lockContext.getEditMode());
    }

    @Test
    void getEditModeWhenNone()
    {
        assertNull(this.lockContext.getEditMode());
    }

    @Test
    void getContentEditorIsResolvedOnlyOnce()
    {
        when(this.editModeResolver.getEditMode()).thenReturn(EditModeResolver.INPLACE);
        when(this.editorManager.<SyntaxContent>getDefaultEditor(SyntaxContent.class, "wysiwyg"))
            .thenReturn(this.blockNoteEditor);

        assertEquals("blocknote", this.lockContext.getContentEditor());
        assertEquals("blocknote", this.lockContext.getContentEditor());

        verify(this.editorManager, times(1)).getDefaultEditor(any(), any());
    }

    @Test
    void getContentEditorForWikiEditMode()
    {
        when(this.editModeResolver.getEditMode()).thenReturn(EditModeResolver.WIKI);
        when(this.editorManager.<SyntaxContent>getDefaultEditor(SyntaxContent.class, "text"))
            .thenReturn(this.textEditor);

        assertEquals("text", this.lockContext.getContentEditor());
        assertEquals("text", this.lockContext.getContentEditor());

        verify(this.editorManager, times(1)).getDefaultEditor(any(), any());
    }

    @Test
    void getContentEditorIsResolvedOnlyOnceWhenThereIsNone()
    {
        assertNull(this.lockContext.getContentEditor());
        assertNull(this.lockContext.getContentEditor());

        verify(this.editorManager, times(1)).getDefaultEditor(any(), any());
    }
}
