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
package org.xwiki.user;

import org.apache.commons.lang3.StringUtils;

/**
 * The default editor that should be used for a user, when editing content.
 *
 * @version $Id$
 * @since 12.2
 */
public enum Editor
{
    /**
     * Text editor (wiki editor).
     */
    TEXT,

    /**
     * WYSIWYG editor.
     */
    WYSIWYG,

    /**
     * The editor is not explictly defined which means it'll dedcided based on some context information (for example
     * based on the wiki's default editor).
     */
    UNDEFINED;

    /**
     * @param editorAsString the editor represented as a string ("Text", "Wysiwyg"). The case is ignored.
     * @return the {@link Editor} object matching the passed string representation. All values different than
     *         {@code Text} (case ignored) are considered to represent the WYSIWYG editor.
     */
    public static Editor fromString(String editorAsString)
    {
        Editor editor;
        if (StringUtils.isEmpty(editorAsString)) {
            editor = UNDEFINED;
        } else if ("text".equalsIgnoreCase(editorAsString)) {
            editor = TEXT;
        } else {
            editor = WYSIWYG;
        }
        return editor;
    }
}
