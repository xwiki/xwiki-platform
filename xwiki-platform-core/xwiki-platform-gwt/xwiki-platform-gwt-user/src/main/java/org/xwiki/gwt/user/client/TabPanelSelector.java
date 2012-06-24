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
package org.xwiki.gwt.user.client;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Notifies {@link Selectable} tabs within a {@link TabPanel} when they are selected.
 * 
 * @version $Id$
 */
public class TabPanelSelector implements BeforeSelectionHandler<Integer>, SelectionHandler<Integer>
{
    @Override
    public void onBeforeSelection(BeforeSelectionEvent<Integer> event)
    {
        if (!event.isCanceled()) {
            TabPanel tabPanel = (TabPanel) event.getSource();
            int selectedTabIndex = tabPanel.getTabBar().getSelectedTab();
            // Check if there is a selected tab and the tab to be selected is not the same.
            if (selectedTabIndex >= 0 && selectedTabIndex != event.getItem()) {
                Widget selectedTab = tabPanel.getDeckPanel().getWidget(selectedTabIndex);
                if (selectedTab instanceof Selectable && ((Selectable) selectedTab).isSelected()) {
                    // Notify the tab before it is hidden.
                    ((Selectable) selectedTab).setSelected(false);
                }
            }
        }
    }

    @Override
    public void onSelection(SelectionEvent<Integer> event)
    {
        TabPanel tabPanel = (TabPanel) event.getSource();
        // Check if a tab was selected.
        if (event.getSelectedItem() >= 0) {
            Widget selectedTab = tabPanel.getDeckPanel().getWidget(event.getSelectedItem());
            if (selectedTab instanceof Selectable && !((Selectable) selectedTab).isSelected()) {
                // Notify the tab after it is shown.
                ((Selectable) selectedTab).setSelected(true);
            }
        }
    }
}
