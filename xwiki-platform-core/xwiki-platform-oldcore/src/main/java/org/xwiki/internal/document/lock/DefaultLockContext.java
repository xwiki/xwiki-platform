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

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.edit.EditModeResolver;

/**
 * Default implementation of {@link LockContext}.
 * <p>
 * The content editor is resolved lazily, on the first call, because not all unlock rules need it and computing it is
 * not free.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public class DefaultLockContext implements LockContext
{
    private final XWikiDocument document;

    private final EditModeResolver editModeResolver;

    private final EditorManager editorManager;

    private EditMode editMode;

    private boolean editModeResolved;

    private String contentEditor;

    private boolean contentEditorResolved;

    /**
     * @param document the document translation whose lock is about to be taken over
     * @param editModeResolver used to determine, on demand, how the given document is going to be edited
     * @param editorManager used to determine, on demand, which editor is going to be used to edit the document content
     */
    public DefaultLockContext(XWikiDocument document, EditModeResolver editModeResolver, EditorManager editorManager)
    {
        this.document = document;
        this.editModeResolver = editModeResolver;
        this.editorManager = editorManager;
    }

    @Override
    public DocumentReference getDocumentReferenceWithLocale()
    {
        return this.document.getDocumentReferenceWithLocale();
    }

    @Override
    public Locale getRealLocale()
    {
        return this.document.getRealLocale();
    }

    @Override
    public EditMode getEditMode()
    {
        if (!this.editModeResolved) {
            String editModeString = this.editModeResolver.getEditMode();
            // The resolved edit mode is not necessarily one of the modes we know about: the editor request parameter
            // can name any edit mode, including one contributed by an extension (e.g. "object" or "class").
            this.editMode = EnumUtils.getEnum(EditMode.class, StringUtils.upperCase(editModeString));
            this.editModeResolved = true;
        }
        return this.editMode;
    }

    @Override
    public String getContentEditor()
    {
        if (!this.contentEditorResolved) {
            getEditMode();
            String category = this.editMode == EditMode.WIKI ? "text" : "wysiwyg";
            // This takes into account the SyntaxContent.wysiwyg.editor request parameter, the editor bindings and
            // the edit.defaultEditor configuration properties.
            Editor<SyntaxContent> editor = this.editorManager.getDefaultEditor(SyntaxContent.class, category);
            this.contentEditor = editor == null ? null : editor.getDescriptor().getId();
            this.contentEditorResolved = true;
        }
        return this.contentEditor;
    }
}
