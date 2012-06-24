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
package org.xwiki.wysiwyg.server;

import org.xwiki.component.annotation.Role;

/**
 * WYSIWYG editor configuration properties.
 * 
 * @version $Id$
 */
@Role
public interface WysiwygEditorConfiguration
{
    /**
     * @return {@code true} if the WYSIWYG/Source tabs are enabled, {@code null} if this configuration property is not
     *         set, {@code false} otherwise
     */
    Boolean isSourceEditorEnabled();

    /**
     * @return the list of plugins that are loaded by the WYSIWYG editor
     */
    String getPlugins();

    /**
     * @return the list of entries on the WYSIWYG editor menu bar
     */
    String getMenuBar();

    /**
     * @return the list of features available on the WYSIWYG editor tool bar
     */
    String getToolBar();

    /**
     * @return {@code true} if the user is allowed to choose only from the attachments of the edited page when creating
     *         a link to an attachment, {@code null} if this configuration property is not set, {@code false} otherwise
     */
    Boolean isAttachmentSelectionLimited();

    /**
     * @return {@code true} if users are allowed to insert external images, i.e. images that are not attached to a wiki
     *         page, {@code null} if this configuration property is not set, {@code false} otherwise
     */
    Boolean areExternalImagesAllowed();

    /**
     * @return {@code true} if the user is allowed to choose only from the list of images attached to the edited page
     *         when inserting an image, {@code null} if this configuration property is not set, {@code false} otherwise
     * @see #isAttachmentSelectionLimited()
     */
    Boolean isImageSelectionLimited();

    /**
     * @return the colors available in the color picker
     */
    String getColorPalette();

    /**
     * @return the number of colors to display per row in the color picker, or {@code null} if this configuration
     *         property is not set
     */
    Integer getColorsPerRow();

    /**
     * @return the list of font names available in the font picker
     */
    String getFontNames();

    /**
     * @return the list of font sizes available in the font picker
     */
    String getFontSizes();

    /**
     * @return the list of style names available in the style picker
     */
    String getStyleNames();

    /**
     * @return the maximum number of history entries that will be stored
     */
    Integer getHistorySize();
}
