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
package org.xwiki.user.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the "User Profile" administration page, rendered by viewing the {@code XWiki.AdminUserProfileSheet}
 * document. It lets an administrator configure which {@code XWiki.XWikiUsers} class properties are displayed in each
 * user profile section. Note that this configuration UI is not registered as a standard administration section: it is
 * reached by viewing the {@code XWiki.AdminUserProfileSheet} document directly, whose content renders the form (and its
 * own {@code save} submit button) on plain view.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
public class UserProfileAdministrationSectionPage extends ViewPage
{
    /**
     * Opens the "User Profile" administration page.
     *
     * @return the page object for the opened administration page
     */
    public static UserProfileAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "AdminUserProfileSheet");
        return new UserProfileAdministrationSectionPage();
    }

    /**
     * Appends a property to the list of properties displayed by the given profile section.
     *
     * @param sectionNumber the number of the {@code XWiki.UserProfileSectionClass} object (e.g. {@code 0} for the
     *            default "Personal" section)
     * @param property the name of the {@code XWiki.XWikiUsers} class property to add to the section
     */
    public void appendPropertyToSection(int sectionNumber, String property)
    {
        WebElement textArea =
            getDriver().findElement(By.id("XWiki.UserProfileSectionClass_" + sectionNumber + "_properties"));
        // The sheet splits the field value on whitespace, so we append the new property on its own line while keeping
        // the existing ones.
        String currentValue = textArea.getAttribute("value");
        textArea.clear();
        textArea.sendKeys(currentValue + "\n" + property);
    }

    /**
     * Saves the administration section. The section renders its own form (with its own {@code save} submit button) and
     * is submitted with a regular POST that reloads the page, hence we don't rely on the standard admin save button.
     */
    public void clickSave()
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(By.cssSelector("input[type='submit'][name='save']")).click();
        getDriver().waitUntilPageIsReloaded();
    }
}
