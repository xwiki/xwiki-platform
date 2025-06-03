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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the realtime WYSIWYG edit page.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeWYSIWYGEditPage extends WYSIWYGEditPage
{
    private RealtimeEditToolbar toolbar;

    /**
     * Open the specified page in realtime WYSIWYG edit mode.
     * 
     * @param targetPageReference the page to edit
     * @return the realtime edit page
     */
    public static RealtimeWYSIWYGEditPage gotoPage(EntityReference targetPageReference)
    {
        WYSIWYGEditPage.gotoPage(targetPageReference);
        return new RealtimeWYSIWYGEditPage();
    }

    /**
     * Default constructor. Waits for the realtime connection to be established.
     */
    public RealtimeWYSIWYGEditPage()
    {
        this.toolbar = new RealtimeEditToolbar().waitUntilConnected();
    }

    /**
     * @return the editor used to edit the content of the page
     */
    public RealtimeCKEditor getContenEditor()
    {
        return new RealtimeCKEditor();
    }

    /**
     * @return the edit mode toolbar (holding the button to save the changes)
     * @since 16.10.6
     * @since 17.3.0RC1
     */
    public RealtimeEditToolbar getToolbar()
    {
        return this.toolbar;
    }

    /**
     * Clicks on the Done button to leave the edit mode and waits for the page to be loaded in view mode.
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
}
