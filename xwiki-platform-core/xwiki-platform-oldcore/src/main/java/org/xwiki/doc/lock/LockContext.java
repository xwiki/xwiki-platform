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
package org.xwiki.doc.lock;

import java.util.Locale;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Provides the information needed to determine if the current user can unlock a document that is currently being edited
 * by someone else. {@link UnlockRule}s are evaluated against this information.
 * 
 * @version $Id$
 * @since 18.8.0RC1
 */
@Unstable
public interface LockContext
{
    /**
     * The supported edit modes.
     */
    enum EditMode
    {
        /**
         * The edit mode that allows the user to edit directly the wiki syntax of the document content.
         */
        WIKI,

        /**
         * The standalone WYSIWYG edit mode, using the edit action and showing the edit panels.
         */
        WYSIWYG,

        /**
         * The Inplace edit mode is used to edit the document title and content directly from the view mode. The content
         * is currently being edited using the configured WYSIWYG editor.
         */
        INPLACE,

        /**
         * The Inline Form edit mode is used by documents that are displayed using a sheet. Most of the time these are
         * documents that have structured data (i.e. objects) attached to them, and this structured data needs a
         * dedicated sheet to be displayed and edited.
         */
        INLINE
    }

    /**
     * @return the reference of the document translation that is currently locked for editing and whose lock the current
     *         user wants to acquire
     */
    DocumentReference getDocumentReferenceWithLocale();

    /**
     * @return the actual locale of the document translation specified by {@link #getDocumentReferenceWithLocale()},
     *         which may be different from the locale specified on the document reference (e.g. in case of the default
     *         document translation)
     */
    Locale getRealLocale();

    /**
     * @return the edit mode that is going to be used to edit the specified document translation
     */
    EditMode getEditMode();

    /**
     * @return the identifier (component hint) of the editor (widget) that is going to be used to edit the document
     *         content in the current edit mode (as returned by {@link #getEditMode()}
     */
    String getContentEditor();
}
