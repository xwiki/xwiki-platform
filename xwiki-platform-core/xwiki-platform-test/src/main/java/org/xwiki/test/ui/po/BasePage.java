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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
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

    /**
     * The top floating content menu bar.
     */
    @FindBy(id = "contentmenu")
    private WebElement contentMenuBar;

    /**
     * The entry on the content menu bar that allows us to edit the current page.
     */
    @FindBy(id = "tmEdit")
    private WebElement editMenu;

    @FindBy(id = "tmCreatePage")
    private WebElement createPageMenuLink;

    @FindBy(id = "tmCreateSpace")
    private WebElement createSpaceMenuLink;

    @FindBy(id = "tmCreate")
    private WebElement createMenu;

    @FindBy(id = "tmActionCopy")
    private WebElement copyPageLink;

    @FindBy(id = "tmActionDelete")
    private WebElement deletePageLink;

    @FindBy(id = "tmWatchDocument")
    private WebElement watchDocumentLink;

    @FindBy(id = "tmPage")
    private WebElement pageMenu;

    @FindBys({@FindBy(id = "tmRegister"), @FindBy(tagName = "a")})
    private WebElement registerLink;

    @FindBys({@FindBy(id = "tmLogin"), @FindBy(tagName = "a")})
    private WebElement loginLink;

    @FindBys({@FindBy(id = "tmLogout"), @FindBy(tagName = "a")})
    private WebElement logoutLink;

    @FindBys({@FindBy(id = "tmUser"), @FindBy(tagName = "a")})
    private WebElement userLink;

    @FindBy(id = "document-title")
    private WebElement documentTitle;

    @FindBy(id = "tmWatchSpace")
    private WebElement watchSpaceLink;

    @FindBy(id = "tmSpace")
    private WebElement spaceMenu;

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
        return (Boolean) ((JavascriptExecutor) getDriver()).executeScript("return XWiki.docisnew");
    }

    /**
     * Perform a click on a "content menu" top entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuTopEntry(String id)
    {
        // Hover the top (floating) content menu bar.
        new Actions(getDriver()).moveToElement(contentMenuBar).perform();
        getDriver().findElement(By.xpath("//div[@id='" + id + "']//strong")).click();
    }

    /**
     * Perform a click on a "content menu" sub-menu entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuEditSubMenuEntry(String id)
    {
        // Hover the top (floating) content menu bar then the edit menu.
        new Actions(getDriver()).moveToElement(contentMenuBar).moveToElement(editMenu).perform();
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
     * @return this page
     * @since 3.2M3
     */
    public BasePage waitUntilPageIsLoaded()
    {
        waitUntilElementIsVisible(By.id("footerglobal"));
        return this;
    }

    /**
     * @since 4.5M1
     */
    public CreatePagePage createPage()
    {
        new Actions(getDriver()).moveToElement(createMenu).perform();
        this.createPageMenuLink.click();
        return new CreatePagePage();
    }

    /**
     * @since 4.5M1
     */
    public CreateSpacePage createSpace()
    {
        new Actions(getDriver()).moveToElement(createMenu).perform();
        this.createSpaceMenuLink.click();
        return new CreateSpacePage();
    }

    /**
     * @since 4.5M1
     */
    public CopyPage copy()
    {
        new Actions(getDriver()).moveToElement(pageMenu).perform();
        this.copyPageLink.click();
        return new CopyPage();
    }

    /**
     * @since 4.5M1
     */
    public ConfirmationPage delete()
    {
        new Actions(getDriver()).moveToElement(pageMenu).perform();
        this.deletePageLink.click();
        return new ConfirmationPage();
    }

    /**
     * @since 4.5M1
     */
    public boolean canDelete()
    {
        if (getUtil().hasElement(By.xpath("//div[@id='tmPage']//span[@class='menuarrow']"))) {
            new Actions(getDriver()).moveToElement(pageMenu).perform();
            return getUtil().hasElement(By.id("tmActionDelete"));
        } else {
            return false;
        }
    }

    /**
     * @since 4.5M1
     */
    public void watchDocument()
    {
        new Actions(getDriver()).moveToElement(pageMenu).perform();
        this.watchDocumentLink.click();
    }

    /**
     * @since 4.5M1
     */
    public boolean hasLoginLink()
    {
        // Note that we cannot test if the loginLink field is accessible since we're using an AjaxElementLocatorFactory
        // and thus it would wait 15 seconds before considering it's not accessible.
        return !getUtil().findElementsWithoutWaiting(getDriver(), By.id("tmLogin")).isEmpty();
    }

    /**
     * @since 4.5M1
     */
    public LoginPage login()
    {
        this.loginLink.click();
        return new LoginPage();
    }

    /**
     * @since 4.5M1
     */
    public String getCurrentUser()
    {
        return this.userLink.getText();
    }

    /**
     * @since 4.5M1
     */
    public void logout()
    {
        this.logoutLink.click();
    }

    /**
     * @since 4.5M1
     */
    public RegistrationPage register()
    {
        this.registerLink.click();
        return new RegistrationPage();
    }

    /**
     * @since 4.5M1
     */
    public String getDocumentTitle()
    {
        return this.documentTitle.getText();
    }

    /**
     * @since 4.5M1
     */
    public void watchSpace()
    {
        new Actions(getDriver()).moveToElement(spaceMenu).perform();
        this.watchSpaceLink.click();
    }

    /**
     * @return the URL of the link representing the current page in the Page top level menu entry
     * @since 4.5M1
     */
    public String getPageMenuLink()
    {
        return this.pageMenu.findElement(By.xpath(".//a[contains(@class, 'tme')]")).getAttribute("href");
    }
}
