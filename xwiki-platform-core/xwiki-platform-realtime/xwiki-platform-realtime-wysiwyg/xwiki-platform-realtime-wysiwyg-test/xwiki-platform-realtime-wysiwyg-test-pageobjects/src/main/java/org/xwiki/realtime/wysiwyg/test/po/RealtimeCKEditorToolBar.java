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
package org.xwiki.realtime.wysiwyg.test.po;

import org.xwiki.ckeditor.test.po.CKEditorToolBar;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;

/**
 * Represents the tool bar of the realtime CKEditor.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class RealtimeCKEditorToolBar extends CKEditorToolBar
{
    /**
     * Create a new tool bar instance for the given editor.
     *
     * @param editor the editor that owns the tool bar
     */
    public RealtimeCKEditorToolBar(RealtimeCKEditor editor)
    {
        super(editor);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Switching to Source mode while collaborating also leaves the realtime editing session, so this waits until the
     * user has left it.
     */
    @Override
    public void toggleSourceMode()
    {
        boolean leavingCollaboration = !isInSourceMode() && new RealtimeEditToolbar().isCollaborating();
        super.toggleSourceMode();
        if (leavingCollaboration) {
            // The realtime session is left asynchronously, after the switch to Source mode.
            getDriver().waitUntilCondition(driver -> !new RealtimeEditToolbar().isCollaborating());
        }
    }
}
