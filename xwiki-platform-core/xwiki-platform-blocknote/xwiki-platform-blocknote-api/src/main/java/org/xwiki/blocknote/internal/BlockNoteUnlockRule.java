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
package org.xwiki.blocknote.internal;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.LockContext.EditMode;
import org.xwiki.doc.lock.UnlockRule;
import org.xwiki.yjs.websocket.internal.Room;
import org.xwiki.yjs.websocket.internal.RoomManager;

/**
 * Allows the current user to take over the lock of a document without confirmation when they are going to join an
 * existing BlockNote realtime collaboration session for that document.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Component
@Singleton
@Named(BlockNoteEditor.ROLE_HINT)
public class BlockNoteUnlockRule implements UnlockRule
{
    @Inject
    private RoomManager roomManager;

    @Override
    public boolean canUnlock(LockContext context)
    {
        return isEditModeSupportingCollaboration(context) && hasCollaborationSession(context);
    }

    private boolean isEditModeSupportingCollaboration(LockContext context)
    {
        // We currently unlock only if the user is entering the WYSIWYG edit mode (standalone or in-place) with the
        // BlockNote editor. We don't unlock for the Inline Form edit mode, even if the BlockNote editor can be used
        // there (e.g. for TextArea properties), because only the content edited with the BlockNote editor is
        // synchronized in realtime. The values set on the other form fields will overwrite each other.
        EditMode editMode = context.getEditMode();
        return (editMode == EditMode.WYSIWYG || editMode == EditMode.INPLACE)
            && BlockNoteEditor.ROLE_HINT.equals(context.getContentEditor());
    }

    private boolean hasCollaborationSession(LockContext context)
    {
        // The collaboration room is associated to the edited document translation, identified by its document reference
        // with locale (the locale being Locale.ROOT for the default document translation).
        Room room = this.roomManager.get(context.getDocumentReferenceWithLocale());
        return room != null && !room.isEmpty();
    }
}
