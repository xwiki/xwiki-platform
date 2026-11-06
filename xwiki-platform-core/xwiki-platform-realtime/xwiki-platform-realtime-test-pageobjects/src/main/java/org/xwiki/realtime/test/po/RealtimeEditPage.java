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
package org.xwiki.realtime.test.po;

import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewableEditPage;

/**
 * Represents the standalone edit mode with realtime enabled. The standard edit toolbar (Save &amp; View, Save &amp;
 * Continue, Cancel) is replaced by the realtime edit toolbar.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class RealtimeEditPage extends PreviewableEditPage
{
    private final RealtimeEditToolbar toolbar;

    /**
     * Default constructor. Waits for the realtime connection to be established.
     */
    public RealtimeEditPage()
    {
        this.toolbar = new RealtimeEditToolbar().waitUntilConnected();
    }

    /**
     * @return the realtime edit toolbar, holding the actions to save the changes
     */
    public RealtimeEditToolbar getToolbar()
    {
        return this.toolbar;
    }

    /**
     * Clicks on the Done button of the realtime edit toolbar, which saves the changes and leaves the edit mode, and
     * waits for the page to be loaded in view mode.
     *
     * @return the view page
     */
    public ViewPage clickDone()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.toolbar.clickDone();
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    /**
     * Saves the changes using the Save &amp; Continue shortcut key, and waits for the save to complete.
     *
     * @return this edit page
     */
    public RealtimeEditPage save()
    {
        this.toolbar.sendSaveShortcutKey();
        return this;
    }

    /**
     * Leaves the edit mode without saving, using the Cancel shortcut key (the realtime edit toolbar doesn't have a
     * Cancel button), and waits for the page to be loaded in view mode.
     *
     * @return the view page
     */
    public ViewPage cancel()
    {
        getUtil().leaveEditMode();
        return new ViewPage();
    }
}
