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

import java.util.Collections;

import org.openqa.selenium.By;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Provides to operations to go to a document's children page, and to consult its content.
 *
 * @version $Id$
 * @since 13.5RC1
 */
public class ChildrenPage extends BaseElement
{
    /**
     * Name of the title Live Data column.
     */
    public static final String LIVE_DATA_TITLE = "Title";

    /**
     * Name of the action Live Data column.
     */
    public static final String LIVE_DATA_ACTIONS = "Actions";

    /**
     * Name of the date Live Data column.
     */
    public static final String LIVE_DATA_DATE = "Date";

    /**
     * Name of the last author Live Data column.
     */
    public static final String LIVE_DATA_LAST_AUTHOR = "Last Author";
    
    private static final String PAGE_MENU_ID = "tmMoreActions";

    /**
     * Access the children page of the current document using the hamburger menu.
     *
     * @return a children page object instance
     */
    public static ChildrenPage clickOnChildrenMenu()
    {
        // Open the page menu.
        XWikiWebDriver driver = getUtil().getDriver();
        driver.findElement(By.id(PAGE_MENU_ID)).click();
        // Wait for the drop down menu to be displayed.
        driver.waitUntilElementIsVisible(By.id(PAGE_MENU_ID));
        // Click on the children button.
        driver.findElement(By.id("tmChildren")).click();
        return new ChildrenPage();
    }

    /**
     * Go to the children page of a document.
     *
     * @param documentReference the reference of the document
     * @return a children page object instance
     */
    public static ChildrenPage goToPage(DocumentReference documentReference)
    {
        getUtil().gotoPage(documentReference, "view", Collections.singletonMap("viewer", "children"));
        ChildrenPage result = new ChildrenPage();
        result.waitUntilPageIsReady();
        return result;
    }

    /**
     * @return a live data page object listing the children of the current page
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("childrenIndex");
    }

    /**
     * @return {@code true} if the viewer has tabs.
     * @since 17.2.0RC1
     * @since 16.10.5
     * @since 16.4.7
     */
    public boolean hasTabs()
    {
        return getDriver().hasElementWithoutWaiting(By.className("xwikitabbar"));
    }

    /**
     * Open the pinned child pages tab.
     * @return an instance of {@link PinnedChildPagesTab} after loading it.
     * @since 17.2.0RC1
     * @since 16.10.5
     * @since 16.4.7
     */
    public PinnedChildPagesTab openPinnedChildPagesTab()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElementWithoutWaiting(By.id("xwikipinnedChildPages")).click();
        getDriver().waitUntilPageIsReloaded();
        return new PinnedChildPagesTab(getDriver().findElementWithoutWaiting(
            By.cssSelector(".xform.navigationPanelConfiguration")));
    }

    /**
     * Open the main tab.
     * @since 17.2.0RC1
     * @since 16.10.5
     * @since 16.4.7
     */
    public void openChildrenTab()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElementWithoutWaiting(By.id("xwikichildren")).click();
        getDriver().waitUntilPageIsReloaded();
    }
}
