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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the application home page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationHomePage extends ViewPage
{
    @FindBys({@FindBy(id = "actionBox"), @FindBy(className = "add")})
    private WebElement addEntryLink;

    @FindBys({@FindBy(id = "actionBox"), @FindBy(className = "edit")})
    private WebElement editClassLink;

    @FindBy(css = "#actionBox .action.deleteData")
    private WebElement deleteAllEntriesLink;

    @FindBy(css = "#actionBox .action.delete")
    private WebElement deleteApplicationLink;

    /**
     * The live table used to browse application data.
     */
    private LiveTableElement entriesLiveTable;

    /**
     * Loads the home page of the specified application.
     * 
     * @param appName the application name
     * @return the application home page
     * @since 5.1RC1
     */
    public static ApplicationHomePage gotoPage(String appName)
    {
        getUtil().gotoPage(appName, "WebHome");
        return new ApplicationHomePage();
    }

    /**
     * Clicks on the link to add a new application entry.
     * 
     * @return the pane used to input the entry name
     */
    public EntryNamePane clickAddNewEntry()
    {
        addEntryLink.click();
        return new EntryNamePane();
    }

    /**
     * Clicks on the link that leads to the class editor.
     * 
     * @return the page used to edit the application class
     */
    public ApplicationClassEditPage clickEditApplication()
    {
        editClassLink.click();
        return new ApplicationClassEditPage();
    }

    /**
     * @return the live table used to browser application data
     */
    public LiveTableElement getEntriesLiveTable()
    {
        if (entriesLiveTable == null) {
            WebElement table = getDriver().findElement(By.className("xwiki-livetable"));
            entriesLiveTable = new LiveTableElement(table.getAttribute("id"));
        }
        return entriesLiveTable;
    }

    /**
     * @return {@code true} if the entries live table is displayed on the home page, {@code false} otherwise
     */
    public boolean hasEntriesLiveTable()
    {
        return getDriver().findElements(By.className("xwiki-livetable")).size() > 0;
    }

    public ConfirmationPage clickDeleteAllEntries()
    {
        this.deleteAllEntriesLink.click();
        return new ConfirmationPage();
    }

    public ConfirmationPage clickDeleteApplication()
    {
        this.deleteApplicationLink.click();
        return new ConfirmationPage();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends InlinePage> T createInlinePage()
    {
        return (T) new ApplicationHomeEditPage();
    }
}
