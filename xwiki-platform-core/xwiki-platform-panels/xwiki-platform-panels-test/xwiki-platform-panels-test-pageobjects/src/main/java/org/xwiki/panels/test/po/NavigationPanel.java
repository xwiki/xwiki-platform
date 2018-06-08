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
package org.xwiki.panels.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions for the Panels.Navigation panel.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
public class NavigationPanel extends ViewPage
{
    /**
     * @return the page representing the Navigation Panel
     */
    public static NavigationPanel gotoPage()
    {
        getUtil().gotoPage("Panels", "Navigation");
        return new NavigationPanel();
    }

    public static String getURL()
    {
        return getUtil().getURL("Panels", "Navigation");
    }

    public NavigationTreeElement getNavigationTree()
    {
        return (NavigationTreeElement) new NavigationTreeElement(
            getDriver().findElementWithoutWaiting(By.cssSelector("#xwikicontent .panel.Navigation .xtree")))
                .waitForIt();
    }
}
