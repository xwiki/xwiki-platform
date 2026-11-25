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
import org.xwiki.realtime.test.po.RealtimeEditPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Represents the realtime WYSIWYG edit page.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeWYSIWYGEditPage extends RealtimeEditPage
{
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
     * @return the editor used to edit the content of the page
     */
    public RealtimeCKEditor getContenEditor()
    {
        return new RealtimeCKEditor();
    }

    /**
     * Leaves this edit page using the given action (e.g. by clicking on a link) and then comes back to it using the
     * browser's Back button. The browser may restore the edit page from its back/forward cache, in which case the edit
     * page reloads itself, because the editing session it was part of is not valid anymore.
     *
     * @param leaveAction the action that makes the browser leave this edit page
     * @return the edit page, loaded again
     * @since 18.10.0
     * @since 18.4.9
     * @since 17.10.16
     */
    public RealtimeWYSIWYGEditPage leaveAndGoBack(Runnable leaveAction)
    {
        // The marker is part of the page state that the back/forward cache keeps, so it's gone only once the edit page
        // has really been loaded again.
        getDriver().addPageNotYetReloadedMarker();
        leaveAction.run();
        getDriver().waitUntilPageIsReloaded();

        getDriver().navigate().back();
        getDriver().waitUntilPageIsReloaded();

        return new RealtimeWYSIWYGEditPage();
    }
}
