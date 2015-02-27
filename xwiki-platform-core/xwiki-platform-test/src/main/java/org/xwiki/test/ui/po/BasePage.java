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

    @FindBy(id = "tmCreatePage")
    private WebElement createPageMenuLink;

    @FindBy(id = "tmCreateSpace")
    private WebElement createSpaceMenuLink;

    @FindBy(xpath = "//div[@id='tmCreate']//button[contains(@class, 'dropdown-toggle')]")
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

    @FindBy(xpath = "//a[@id='tmLogin']")
    private WebElement loginLink;

    @FindBy(xpath = "//li[@id='tmUser']/a[contains(@title, 'Profile:')]")
    private WebElement userLink;

    @FindBy(id = "document-title")
    private WebElement documentTitle;

    @FindBy(id = "tmWatchSpace")
    private WebElement watchSpaceLink;

    @FindBy(id = "tmSpace")
    private WebElement spaceMenu;

    @FindBy(id = "tmWiki")
    private WebElement wikiMenu;

    @FindBy(id = "tmWatchWiki")
    private WebElement watchWikiLink;

    /**
     * Used to scroll the page to the top before accessing the floating menu.
     */
    @FindBy(id = "companylogo")
    protected WebElement logo;

    /**
     * Note: when reusing instances of BasePage, the constructor is not doing the work anymore and the
     * waitUntilPageJSIsLoaded() method needs to be executed manually, when needed.
     * <p/>
     * Note2: Never call the constructor before navigating to the page you need to test first.
     */
    public BasePage()
    {
        super();
        waitUntilPageJSIsLoaded();
    }

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
        return !getDriver().findElementsWithoutWaiting(By.id("tmUser")).isEmpty();
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
     * Perform a click on a "edit menu" sub-menu entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickEditSubMenuEntry(String id)
    {
        // Open the edit menu
        getDriver().findElement(By.xpath("//div[@id='tmEdit']//button")).click();
        // Wait for the submenu entry to be visible
        getDriver().waitUntilElementIsVisible(By.id(id));
        // Click on the specified entry
        getDriver().findElement(By.id(id)).click();
    }

    /**
     * Performs a click on the "edit" button.
     */
    public void edit()
    {
        // The edit button is not the same depending on whether the user is advanced or not
        if (getDriver().hasElementWithoutWaiting(By.xpath("//div[@id='tmEdit']//a"))) {
            getDriver().findElement(By.xpath("//div[@id='tmEdit']//a")).click();
        } else {
            getDriver().findElement(By.xpath("//a[@id='tmEdit']")).click();
        }
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
        clickEditSubMenuEntry("tmEditWiki");
        return new WikiEditPage();
    }

    /**
     * Performs a click on the "edit wysiwyg" entry of the content menu.
     */
    public WYSIWYGEditPage editWYSIWYG()
    {
        clickEditSubMenuEntry("tmEditWysiwyg");
        return new WYSIWYGEditPage();
    }

    /**
     * Performs a click on the "edit inline" entry of the content menu.
     */
    public <T extends InlinePage> T editInline()
    {
        clickEditSubMenuEntry("tmEditInline");
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
        clickEditSubMenuEntry("tmEditRights");
        return new RightsEditPage();
    }

    /**
     * Performs a click on the "edit objects" entry of the content menu.
     */
    public ObjectEditPage editObjects()
    {
        clickEditSubMenuEntry("tmEditObject");
        return new ObjectEditPage();
    }

    /**
     * Performs a click on the "edit class" entry of the content menu.
     */
    public ClassEditPage editClass()
    {
        clickEditSubMenuEntry("tmEditClass");
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
        getDriver().waitUntilElementIsVisible(By.id("footerglobal"));
        return this;
    }

    /**
     * @since 5.4RC1
     */
    public void moveToCreateMenu()
    {
        new Actions(getDriver()).moveToElement(createMenu).perform();
    }

    /**
     * On Flamingo, we have to click to open the menu (hovering it is not enough).
     * 
     * @since 6.2M2
     */
    public void toggleCreateMenu()
    {
        this.createMenu.click();
    }

    /**
     * @since 6.2M2
     */
    public void togglePageMenu()
    {
        toggleTopMenu("tmPage");
    }

    /**
     * @since 6.2M2
     */
    public void toggleUserMenu()
    {
        toggleTopMenu("tmUser");
    }

    /**
     * @since 6.2M2
     */
    public void toggleSpaceMenu()
    {
        toggleTopMenu("tmSpace");
    }

    /**
     * @since 6.2M2
     */
    public void toggleWikiMenu()
    {
        // Depending on if the current wiki is a subwiki or not
        String wikiMenuId = "tmWiki";
        if (!getDriver().hasElement(By.id(wikiMenuId))) {
            wikiMenuId = "tmMainWiki";
        }
        toggleTopMenu(wikiMenuId);
    }

    /**
     * @return {@code true} if the screen is extra small (as defined by Bootstrap), {@code false} otherwise
     */
    private boolean isExtraSmallScreen()
    {
        return getDriver().manage().window().getSize().getWidth() < 768;
    }

    private By getTopMenuToggleSelector(String menuId)
    {
        String side = isExtraSmallScreen() ? "left" : "right";
        return By.xpath("//li[@id='" + menuId + "']//a[contains(@class, 'dropdown-split-" + side + "')]");
    }

    private void toggleTopMenu(String menuId)
    {
        getDriver().findElement(getTopMenuToggleSelector(menuId)).click();
    }

    /**
     * @since 4.5M1
     */
    public CreatePagePage createPage()
    {
        toggleCreateMenu();
        this.createPageMenuLink.click();
        return new CreatePagePage();
    }

    /**
     * @since 4.5M1
     */
    public CreateSpacePage createSpace()
    {
        toggleCreateMenu();
        this.createSpaceMenuLink.click();
        return new CreateSpacePage();
    }

    /**
     * @since 4.5M1
     */
    public CopyPage copy()
    {
        togglePageMenu();
        this.copyPageLink.click();
        return new CopyPage();
    }

    /**
     * @since 4.5M1
     */
    public ConfirmationPage delete()
    {
        togglePageMenu();
        this.deletePageLink.click();
        return new ConfirmationPage();
    }

    /**
     * @since 4.5M1
     */
    public boolean canDelete()
    {
        if (getDriver().hasElement(By.xpath("//li[@id='tmPage']//a[contains(@class, 'dropdown-toggle')]"))) {
            togglePageMenu();
            return getDriver().hasElement(By.id("tmActionDelete"));
        } else {
            return false;
        }
    }

    /**
     * @since 4.5M1
     */
    public void watchDocument()
    {
        togglePageMenu();
        this.watchDocumentLink.click();
    }

    /**
     * @since 4.5M1
     */
    public boolean hasLoginLink()
    {
        // Note that we cannot test if the loginLink field is accessible since we're using an AjaxElementLocatorFactory
        // and thus it would wait 15 seconds before considering it's not accessible.
        return !getDriver().findElementsWithoutWaiting(By.id("tmLogin")).isEmpty();
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
        toggleUserMenu();
        getDriver().findElement(By.id("tmLogout")).click();
        // Update the CSRF token because the context user has changed (it's guest user now). Otherwise, APIs like
        // TestUtils#createUser*(), which expect the currently cached token to be valid, will fail because they would be
        // using the token of the previously logged in user.
        getUtil().recacheSecretToken();
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
        toggleSpaceMenu();
        this.watchSpaceLink.click();
    }

    /**
     * @since 6.0M1
     */
    public void watchWiki()
    {
        toggleWikiMenu();
        this.watchWikiLink.click();
    }

    /**
     * @return the URL of the link representing the current page in the Page top level menu entry
     * @since 4.5M1
     */
    public String getPageMenuLink()
    {
        return this.pageMenu.findElement(By.xpath(".//a[contains(@title, 'Page: ')]")).getAttribute("href");
    }

    /**
     * Waits for the javascript libraries and their plugins that need to load before the UI's elements can be used
     * safely.
     * <p/>
     * Subclassed should override this method and add additional checks needed by their logic.
     * 
     * @since 6.2
     */
    public void waitUntilPageJSIsLoaded()
    {
        // Prototype
        getDriver().waitUntilJavascriptCondition("return window.Prototype != null && window.Prototype.Version != null");

        // JQuery and dependencies
        // JQuery dropdown plugin needed for the edit button's dropdown menu.
        getDriver().waitUntilJavascriptCondition("return window.jQuery != null && window.jQuery().dropdown != null");
    }
}
