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

import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.model.reference.EntityReference;

/**
 * Represents the Inplace Edit Mode with realtime enabled.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public class RealtimeInplaceEditablePage extends InplaceEditablePage
{
    private RealtimeEditToolbar toolbar;

    /**
     * Navigate to the specified in-place editable page.
     *
     * @param reference the reference of the page to navigate to
     * @return the in-place editable page
     * @since 16.10.12
     * @since 17.4.5
     * @since 17.8.0
     */
    public static RealtimeInplaceEditablePage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(reference);
        return new RealtimeInplaceEditablePage();
    }

    /**
     * @return the edit mode toolbar, holding the button to save the changes
     */
    public RealtimeEditToolbar getToolbar()
    {
        return this.toolbar;
    }

    @Override
    public RealtimeInplaceEditablePage editInplace()
    {
        super.editInplace();
        return this;
    }

    @Override
    public RealtimeInplaceEditablePage waitForInplaceEditor()
    {
        super.waitForInplaceEditor();
        this.toolbar = new RealtimeEditToolbar().waitUntilConnected();
        return this;
    }

    /**
     * Click on the "Done" button and wait for the page to return to view mode.
     * 
     * @return this instance
     */
    public RealtimeInplaceEditablePage done()
    {
        this.toolbar.clickDone();
        waitForView();
        return this;
    }
}
