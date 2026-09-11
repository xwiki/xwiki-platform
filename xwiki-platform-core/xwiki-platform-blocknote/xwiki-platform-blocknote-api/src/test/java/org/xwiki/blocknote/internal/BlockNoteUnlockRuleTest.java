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

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.LockContext.EditMode;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.yjs.websocket.internal.Room;
import org.xwiki.yjs.websocket.internal.RoomManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlockNoteUnlockRule}.
 *
 * @version $Id$
 */
@ComponentTest
class BlockNoteUnlockRuleTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("test", "Some", "Page");

    @InjectMockComponents
    private BlockNoteUnlockRule unlockRule;

    @MockComponent
    private RoomManager roomManager;

    @Mock
    private LockContext lockContext;

    @Mock
    private Room room;

    /**
     * @param locale the locale of the edited document translation, {@link Locale#ROOT} for the original translation
     * @return the reference of the collaboration room associated to the edited document translation
     */
    private DocumentReference editing(Locale locale)
    {
        DocumentReference referenceWithLocale = new DocumentReference(DOCUMENT_REFERENCE, locale);
        when(this.lockContext.getDocumentReferenceWithLocale()).thenReturn(referenceWithLocale);
        return referenceWithLocale;
    }

    @Test
    void canUnlockWithAnotherContentEditor()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");

        assertFalse(this.unlockRule.canUnlock(this.lockContext));
        verifyNoInteractions(this.roomManager);
    }

    @Test
    void canUnlockWithoutContentEditor()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.INPLACE);
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
        verifyNoInteractions(this.roomManager);
    }

    @Test
    void canUnlockWithoutRoom()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn(BlockNoteEditor.ROLE_HINT);
        editing(Locale.ROOT);

        assertFalse(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithEmptyRoom()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.INPLACE);
        when(this.lockContext.getContentEditor()).thenReturn(BlockNoteEditor.ROLE_HINT);
        when(this.roomManager.get(editing(Locale.ROOT))).thenReturn(this.room);
        when(this.room.isEmpty()).thenReturn(true);

        assertFalse(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithActiveRoomOnTheOriginalTranslation()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        // The original document translation is edited in a collaboration room keyed by the root locale, because this
        // is the locale the client sends when it joins the room.
        when(this.lockContext.getContentEditor()).thenReturn(BlockNoteEditor.ROLE_HINT);
        when(this.roomManager.get(editing(Locale.ROOT))).thenReturn(this.room);

        assertTrue(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithActiveRoomOnAnotherTranslation()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.INPLACE);
        when(this.lockContext.getContentEditor()).thenReturn(BlockNoteEditor.ROLE_HINT);
        when(this.roomManager.get(editing(Locale.FRENCH))).thenReturn(this.room);

        assertTrue(this.unlockRule.canUnlock(this.lockContext));
    }
}
