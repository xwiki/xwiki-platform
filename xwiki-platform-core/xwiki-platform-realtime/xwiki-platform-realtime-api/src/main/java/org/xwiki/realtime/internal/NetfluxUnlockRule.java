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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.doc.lock.LockContext;
import org.xwiki.doc.lock.LockContext.EditMode;
import org.xwiki.doc.lock.UnlockRule;
import org.xwiki.netflux.EntityChannelStore;

/**
 * Allows the current user to take over the lock of a document without confirmation when they are going to join an
 * existing Netflux based realtime editing session for that document.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Component
@Singleton
@Named("netflux")
public class NetfluxUnlockRule implements UnlockRule
{
    @Inject
    private EntityChannelStore entityChannelStore;

    @Override
    public boolean canUnlock(LockContext context)
    {
        return isEditModeSupportingCollaboration(context) && hasCollaborationSession(context);
    }

    private boolean isEditModeSupportingCollaboration(LockContext context)
    {
        // We unlock if the user is entering the Wiki edit mode (with the 'realtime-wiki' editor) or the WYSIWYG edit
        // mode (standalone or in-place, with CKEditor). We don't unlock for the Inline Form edit mode, even if CKEditor
        // could be used there (e.g. for TextArea properties), because only the content edited with CKEditor is
        // currently synchronized in realtime. The values set on the other form fields will overwrite each other.
        return isRealtimeWiki(context) || isRealtimeCKEditor(context);

    }

    private boolean isRealtimeWiki(LockContext context)
    {
        return context.getEditMode() == EditMode.WIKI && "realtime-wiki".equals(context.getContentEditor());
    }

    private boolean isRealtimeCKEditor(LockContext context)
    {
        EditMode editMode = context.getEditMode();
        return (editMode == EditMode.WYSIWYG || editMode == EditMode.INPLACE)
            && "ckeditor".equals(context.getContentEditor());
    }

    private boolean hasCollaborationSession(LockContext context)
    {
        // The Netflux channels are bound to the type of editor used to edit the document content field (e.g.
        // "wiki" or "wysiwyg"), rather than to a specific editor widget (e.g. "ckeditor").
        EditMode editMode = context.getEditMode();
        if (editMode == EditMode.INPLACE) {
            // The Inplace edit mode uses the same Netflux channels as the standalone WYSIWYG edit mode (when the
            // WYSIWYG editor is used to edit the document content).
            editMode = EditMode.WYSIWYG;
        }
        String channelEditor = String.valueOf(editMode).toLowerCase();

        // We use Locale.toString() instead of Locale.toLanguageTag() in order to match the output of the Page REST
        // API (see ModelFactory#toRestPage()), which is used by the JavaScript code to determine the locale of the
        // edited document and create the associated Netflux channel.
        List<String> contentChannelPath =
            List.of("translations", context.getRealLocale().toString(), "fields", "content", "editors", channelEditor);
        return this.entityChannelStore
            .getChannel(context.getDocumentReferenceWithLocale().withoutLocale(), contentChannelPath)
            .map(contentChannel -> contentChannel.getUserCount() > 0).orElse(false);
    }
}
