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
 * Provides to operations to go to a document's siblings page, and to consult its content.
 *
 * @version $Id$
 * @since 13.5RC1
 */
public class SiblingsPage extends BaseElement
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
     * Access the siblings page of the current document using the hamburger menu.
     *
     * @return a siblings page object instance
     */
    public static SiblingsPage clickOnSiblingsMenu()
    {
        // Open the page menu.
        XWikiWebDriver driver = getUtil().getDriver();
        driver.findElement(By.id(PAGE_MENU_ID)).click();
        // Wait for the drop down menu to be displayed.
        driver.waitUntilElementIsVisible(By.id(PAGE_MENU_ID));
        // Click on the siblings button.
        driver.findElement(By.id("tmSiblings")).click();
        return new SiblingsPage();
    }

    /**
     * Go to the siblings page of a document.
     *
     * @param documentReference the reference of the document
     * @return a siblings page object instance
     */
    public static SiblingsPage goToPage(DocumentReference documentReference)
    {
        getUtil().gotoPage(documentReference, "view", Collections.singletonMap("viewer", "siblings"));
        return new SiblingsPage();
    }

    /**
     * @return a live data page object listing the siblings of the current page
     */
    public LiveDataElement getLiveData()
    {
        return new LiveDataElement("siblingsIndex");
    }
}
