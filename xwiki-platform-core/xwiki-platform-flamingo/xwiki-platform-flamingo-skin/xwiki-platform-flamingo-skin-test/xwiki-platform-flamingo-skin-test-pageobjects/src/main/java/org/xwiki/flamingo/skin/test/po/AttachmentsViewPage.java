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
package org.xwiki.flamingo.skin.test.po;

import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents a page with the attachments pane active.
 *
 * @version $Id$
 * @since 14.8RC1
 */
public class AttachmentsViewPage extends ViewPage
{
    private static final String ATTACHMENTS = "Attachments";

    /**
     * @return if the attachments pane is available
     * @since 17.7.0RC1
     * @since 17.4.3
     * @since 16.10.10
     */
    public boolean isAttachmentsDocExtraPaneAvailable()
    {
        return hasDocExtraPane(ATTACHMENTS);
    }

    /**
     * Open the attachments docextra tab.
     *
     * @return the element corresponding to the attachments tab
     */
    public AttachmentsPane openAttachmentsDocExtraPane()
    {
        openDocExtraPane(ATTACHMENTS);
        return new AttachmentsPane();
    }

    /**
     * Use shortcut key to open the attachments docextra tab.
     *
     * @return the element corresponding to the attachments tab
     */
    public AttachmentsPane useShortcutKeyForAttachmentPane()
    {
        useShortcutForDocExtraPane(ATTACHMENTS, "a");
        return new AttachmentsPane();
    }
}
