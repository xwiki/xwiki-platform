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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.RightsEditPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Represents the common actions possible on all Pages.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class BasePage extends BaseElement
{
    /**
     * Used for sending keyboard shortcuts to.
     */
    @FindBy(id = "xwikimaincontainer")
    private WebElement mainContainerDiv;

    public String getPageTitle()
    {
        return getDriver().getTitle();
    }

    // TODO I think this should be in the AbstractTest instead -cjdelisle
    public String getPageURL()
    {
        return getDriver().getCurrentUrl();
    }

    public String getMetaDataValue(String metaName)
    {
        return getDriver().findElement(By.xpath("//meta[@name='" + metaName + "']")).getAttribute("content");
    }

    /**
     * @return true if we are currently logged in, false otherwise
     */
    public boolean isAuthenticated()
    {
        // Note that we cannot test if the userLink field is accessible since we're using an AjaxElementLocatorFactory
        // and thus it would wait 15 seconds before considering it's not accessible.
        return !getUtil().findElementsWithoutWaiting(getDriver(), By.id("tmUser")).isEmpty();
    }

    /**
     * Determine if the current page is a new document.
     * 
     * @return true if the document is new, false otherwise
     */
    public boolean isNewDocument()
    {
        return (Boolean) getDriver().executeScript("return XWiki.docisnew");
    }

    /**
     * Emulate mouse over on a top menu entry.
     * 
     * @param menuId Menu to emulate the mouse over on
     */
    protected void hoverOverMenu(String menuId)
    {
        // We need to hover over the Wiki menu so that the menu entry is visible before we can click on
        // it. The normal way to implement it is to do something like this:
        //
        // @FindBy(id = "tmWiki")
        // private WebElement spaceMenuDiv;
        // ...
        // ((RenderedWebElement) spaceMenuDiv).hover();
        //
        // However it seems that currently Native Events don't work in FF 3.5+ versions and it seems to be only working
        // on Windows. Thus for now we have to simulate the hover using JavaScript.
        //
        // In addition there's a second bug where a WebElement retrieved using a @FindBy annotation cannot be used
        // as a parameter to JavascriptExecutor.executeScript().
        // See http://code.google.com/p/selenium/issues/detail?id=256
        // Thus FTM we have to use getDriver().findElement().

        // For some unknown reason sometimes the menuId cannot be found so wait for it to be visible before finding it.
        waitUntilElementIsVisible(By.id(menuId));

        WebElement menuDiv = getDriver().findElement(By.id(menuId));
        getDriver().executeScript("showsubmenu(arguments[0])", menuDiv);

        // We wait for the submenu to be visible before carrying on to ensure that after this method returns the
        // calling code can access submenu items.
        waitUntilElementIsVisible(By.xpath("//div[@id = '" + menuId + "']//span[contains(@class, 'submenu')]"));
    }

    /**
     * Perform a click on a "content menu" top entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuTopEntry(String id)
    {
        // Starting with 3.4M1 the floating content/edit action menu is minimized to a thin line when scrolling down the
        // page. The menu is maximized when the thin line at the top of the window (page view port) is hovered with the
        // mouse. As a consequence, we can't click directly on the menu entries; we have to make them visible. Since
        // WebDriver doesn't handle very well the hover action (it doesn't trigger the :hover CSS pseudo-class) a quick
        // fix is to simply scroll the page to the top by pressing the Home key.
        // See http://jira.xwiki.org/browse/XWIKI-6018
        sendKeys(Keys.HOME);
        getDriver().findElement(By.xpath("//div[@id='" + id + "']//strong")).click();
    }

    /**
     * Perform a click on a "content menu" sub-menu entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuEditSubMenuEntry(String id)
    {
        hoverOverMenu("tmEdit");
        getDriver().findElement(By.xpath("//a[@id='" + id + "']")).click();
    }

    /**
     * Performs a click on the "edit" entry of the content menu.
     */
    public void edit()
    {
        clickContentMenuTopEntry("tmEdit");
    }

    /**
     * Gets a string representation of the URL for editing the page.
     */
    public String getEditURL()
    {
        return getDriver().findElement(By.xpath("//div[@id='tmEdit']//a")).getAttribute("href");
    }

    /**
     * Performs a click on the "edit wiki" entry of the content menu.
     */
    public WikiEditPage editWiki()
    {
        clickContentMenuEditSubMenuEntry("tmEditWiki");
        return new WikiEditPage();
    }

    /**
     * Performs a click on the "edit wysiwyg" entry of the content menu.
     */
    public WYSIWYGEditPage editWYSIWYG()
    {
        clickContentMenuEditSubMenuEntry("tmEditWysiwyg");
        return new WYSIWYGEditPage();
    }

    /**
     * Performs a click on the "edit inline" entry of the content menu.
     */
    public <T extends InlinePage> T editInline()
    {
        clickContentMenuEditSubMenuEntry("tmEditInline");
        return createInlinePage();
    }
    
    /**
     * Can be overridden to return extended {@link InlinePage}.
     */
    @SuppressWarnings("unchecked")
    protected <T extends InlinePage> T createInlinePage()
    {
        return (T) new InlinePage();
    }

    /**
     * Performs a click on the "edit acces rights" entry of the content menu.
     */
    public RightsEditPage editRights()
    {
        clickContentMenuEditSubMenuEntry("tmEditRights");
        return new RightsEditPage();
    }

    /**
     * Performs a click on the "edit objects" entry of the content menu.
     */
    public ObjectEditPage editObjects()
    {
        clickContentMenuEditSubMenuEntry("tmEditObject");
        return new ObjectEditPage();
    }

    /**
     * Performs a click on the "edit class" entry of the content menu.
     */
    public ClassEditPage editClass()
    {
        clickContentMenuEditSubMenuEntry("tmEditClass");
        return new ClassEditPage();
    }

    /**
     * @since 3.2M3
     */
    public void sendKeys(CharSequence... keys)
    {
        this.mainContainerDiv.sendKeys(keys);
    }

    /**
     * Waits until the page has loaded. Normally we don't need to call this method since a click in Selenium2 is a
     * blocking call. However there are cases (such as when using a shortcut) when we asynchronously load a page.
     * 
     * @since 3.2M3
     */
    public void waitUntilPageIsLoaded()
    {
        waitUntilElementIsVisible(By.id("footerglobal"));
    }
}
