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
package org.xwiki.xclass.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Represents the page that lists the available data types (XClasses) and allows us to create a new type.
 * 
 * @version $Id$
 * @since 4.5
 */
public class DataTypesPage extends ViewPage
{
    /**
     * The widget used to specify the class to create.
     */
    private final DocumentPicker documentPicker = new DocumentPicker();

    /**
     * The button used to create a new class.
     */
    @FindBy(xpath = "//*[@class = 'button' and @value = 'Create this Class']")
    private WebElement createClassButton;

    /**
     * The Live Data table that lists the existing classes.
     */
    private final TableLayoutElement classesLiveDataTable = (new LiveDataElement("classes")).getTableLayout();

    /**
     * Opens the page that list the available data types.
     * 
     * @return the page that lists the available data types
     */
    public static DataTypesPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiClasses");
        return new DataTypesPage();
    }

    /**
     * Starts the process of creating a new XClass.
     * 
     * @param spaceName the name of the space where to create the class
     * @param className the class name
     * @return the wiki edit mode for the specified class
     */
    public ClassSheetPage createClass(String spaceName, String className)
    {
        this.documentPicker.setParent(spaceName);
        this.documentPicker.setName(className);

        // The create class form is submitted by JavaScript code in case the create button is clicked before the
        // asynchronous validation ends, and in this case Selenium doesn't wait for the new page to load.
        getDriver().addPageNotYetReloadedMarker();
        this.createClassButton.click();
        getDriver().waitUntilPageIsReloaded();

        new WikiEditPage().clickSaveAndView();
        return new ClassSheetPage();
    }

    /**
     * @return the button used to create a new class
     */
    public WebElement getCreateClassButton()
    {
        return createClassButton;
    }

    /**
     * @param spaceName the name of the space were the class document is
     * @param className the name of the class document
     * @return {@code true} if the specified class is listed, {@code false} otherwise
     */
    public boolean isClassListed(String spaceName, String className)
    {
        String classReference = spaceName + "." + className;
        this.classesLiveDataTable.filterColumn("Location", classReference);
        return this.classesLiveDataTable.countRows() > 0;
    }
}
