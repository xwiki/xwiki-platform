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
package org.xwiki.realtime.internal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.LockContext.EditMode;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NetfluxUnlockRule}.
 *
 * @version $Id$
 */
@ComponentTest
class NetfluxUnlockRuleTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("test", "Some", "Page");

    @InjectMockComponents
    private NetfluxUnlockRule unlockRule;

    @MockComponent
    private EntityChannelStore entityChannelStore;

    @Mock
    private LockContext lockContext;

    @BeforeEach
    void setUp()
    {
        when(this.lockContext.getDocumentReferenceWithLocale())
            .thenReturn(new DocumentReference(DOCUMENT_REFERENCE, Locale.ROOT));
        when(this.lockContext.getRealLocale()).thenReturn(Locale.CANADA_FRENCH);
    }

    private EntityChannel createChannel(String locale, String editor, int userCount)
    {
        List<String> path = List.of("translations", locale, "fields", "content", "editors", editor);
        EntityChannel channel = new EntityChannel(DOCUMENT_REFERENCE, path, "");
        channel.setUserCount(userCount);
        when(this.entityChannelStore.getChannel(DOCUMENT_REFERENCE, path)).thenReturn(Optional.of(channel));
        return channel;
    }

    @Test
    void canUnlockWithoutContentEditor()
    {
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
        verifyNoInteractions(this.entityChannelStore);
    }

    @Test
    void canUnlockWithEditorThatDoesntUseNetflux()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn("blocknote");

        // A running CKEditor session must not allow taking over the lock for a user that is going to edit the content
        // with a different WYSIWYG editor, because they wouldn't be able to join that session. This is why the channel
        // store is not even looked at.
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
        verifyNoInteractions(this.entityChannelStore);
    }

    @Test
    void canUnlockWithoutSession()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.INPLACE);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithInactiveSession()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");
        createChannel("fr_CA", "wysiwyg", 0);
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithActiveCKEditorSession()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.INPLACE);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");
        createChannel("fr_CA", "wysiwyg", 1);
        assertTrue(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithActiveWikiEditorSession()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WIKI);
        when(this.lockContext.getContentEditor()).thenReturn("realtime-wiki");
        createChannel("fr_CA", "wiki", 1);
        assertTrue(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockWithSessionOnAnotherTranslation()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");
        createChannel("fr", "wysiwyg", 1);
        assertFalse(this.unlockRule.canUnlock(this.lockContext));
    }

    @Test
    void canUnlockForOriginalTranslation()
    {
        when(this.lockContext.getEditMode()).thenReturn(EditMode.WYSIWYG);
        when(this.lockContext.getContentEditor()).thenReturn("ckeditor");
        // The original document translation is published by the Page REST API with the wiki default locale, so this is
        // the locale used to create the Netflux channel.
        when(this.lockContext.getRealLocale()).thenReturn(Locale.ENGLISH);
        createChannel("en", "wysiwyg", 2);
        assertTrue(this.unlockRule.canUnlock(this.lockContext));
    }
}
