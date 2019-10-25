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
package org.xwiki.test.selenium.framework;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Assert;
import org.openqa.selenium.By;

/**
 * Implementation of skin-related actions for the Flamingo skin.
 *
 * @version $Id$
 * @since 6.2M2
 */
public class FlamingoSkinExecutor extends ColibriSkinExecutor
{
    public FlamingoSkinExecutor(AbstractXWikiTestCase test)
    {
        super(test);
    }
    
    private void openDrawer()
    {
        getTest().waitPage();
        getDriver().findElementById("tmDrawerActivator").click();
        getDriver().waitUntilElementIsVisible(By.id("tmDrawer"));
    }

    private void closeDrawer()
    {
        // Click outside the drawer to close it
        getTest().clickLinkWithXPath("//div[@id = 'companylogo']/a[contains(@class, 'navbar-brand')]");
        getDriver().waitUntilElementDisappears(By.id("tmDrawer"));
    }

    @Override
    public boolean isAuthenticated()
    {
        return getTest().isElementPresentWithoutWaiting(By.xpath(
                "//div[@id='xwikimainmenu']//li[contains(@class, 'navbar-avatar')]"));
    }

    @Override
    public boolean isAuthenticated(String username)
    {
        return getTest().isElementPresentWithoutWaiting(By.xpath(
                "//div[@id='xwikimainmenu']//li[contains(@class, 'navbar-avatar')]"
                        + "//a[contains(@href, '/xwiki/bin/view/XWiki/" + username + "')]"));
    }

    @Override
    public boolean isAuthenticationMenuPresent()
    {
        return getTest().isElementPresentWithoutWaiting(By.xpath("//a[@id = 'tmLogin']"))
            || getTest().isElementPresentWithoutWaiting(By.xpath("//a[@id = 'tmLogout']"));
    }

    @Override
    public void clickShowComments()
    {
        getTest().clickLinkWithLocator("//a[@id = 'Commentslink']", false);
    }

    @Override
    public void clickShowAttachments()
    {
        getTest().clickLinkWithLocator("//a[@id = 'Attachmentslink']", false);
    }

    @Override
    public void clickShowHistory()
    {
        getTest().clickLinkWithLocator("//a[@id = 'Historylink']", false);
    }

    @Override
    public void clickShowInformation()
    {
        getTest().clickLinkWithLocator("//a[@id = 'Informationlink']", false);
    }

    @Override
    public void logout()
    {
        Assert.assertTrue("User wasn't authenticated.", isAuthenticated());
        openDrawer();
        getTest().clickLinkWithLocator("//a[@id = 'tmLogout']");
        Assert.assertFalse("The user is still authenticated after a logout.", isAuthenticated());
    }

    @Override
    public void clickLogin()
    {
        openDrawer();
        getTest().clickLinkWithLocator("//a[@id = 'tmLogin']");
        assertIsLoginPage();
    }

    @Override
    public void clickRegister()
    {
        openDrawer();
        getTest().clickLinkWithLocator("//a[@id = 'tmRegister']");
        assertIsRegisterPage();
    }

    @Override
    protected void clickEditMenuItem(String menuItemId)
    {
        // Click on the arrow in the edit button
        getTest().clickLinkWithLocator("//div[@id='tmEdit']/*[contains(@class, 'dropdown-toggle')]");
        getTest().clickLinkWithLocator(menuItemId);
    }

    private void clickMoreActionsMenuItem(String menuItemId)
    {
        // Click on the arrow in the edit button
        getTest().clickLinkWithLocator("//div[@id = 'tmMoreActions']/a[contains(@role, 'button')]");
        getTest().clickLinkWithLocator(menuItemId);
    }

    private void clickAdminActionsSubMenuEntry(String menuItemId)
    {
        // Click on the arrow in the edit button
        getTest().clickLinkWithLocator("//div[@id = 'tmAdminActions']/a[contains(@role, 'button')]");
        getTest().clickLinkWithLocator(menuItemId);
    }

    @Override
    public void clickCopyPage()
    {
        clickAdminActionsSubMenuEntry("tmActionCopy");
    }

    @Override
    public void clickEditPageAccessRights()
    {
        clickEditMenuItem("tmEditRights");
    }

    @Override
    public void clickAdministerWiki()
    {
        openDrawer();
        // Click the "Administer Wiki" link.
        getTest().getSelenium().click("tmAdminWiki");
    }

    @Override
    public boolean copyPage(String spaceName, String pageName, String targetSpaceName, String targetPageName)
    {
        StringBuilder queryString = new StringBuilder("xpage=copy");
        queryString.append("&form_token=").append(encodeURLParameter(getTest().getSecretToken()));
        queryString.append("&sourceSpaceName=").append(encodeURLParameter(spaceName));
        queryString.append("&sourcePageName=").append(encodeURLParameter(pageName));
        queryString.append("&targetSpaceName=").append(encodeURLParameter(targetSpaceName));
        queryString.append("&targetPageName=").append(encodeURLParameter(targetPageName));
        String copyURL = getTest().getUrl(spaceName, pageName, "view", queryString.toString());
        getTest().open(copyURL);
        return getDriver().hasElement(By.cssSelector(".xcontent .successmessage"));
    }

    private String encodeURLParameter(String value)
    {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen.
            return null;
        }
    }
}
