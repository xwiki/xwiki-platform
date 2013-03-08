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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.EditRightsPane;

/**
 * Represents the actions possible on the Global Rights Administration Page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class GlobalRightsAdministrationSectionPage extends AdministrationSectionPage
{
    @FindBy(id = "guest_comment_requires_captcha")
    private WebElement captchaCheckBox;

    @FindBy(id = "authenticate_view")
    WebElement forceAuthenticatedViewLink;

    private EditRightsPane editRightsPane = new EditRightsPane();

    public GlobalRightsAdministrationSectionPage()
    {
        super("Rights");
    }

    /**
     * @since 4.2M1
     */
    public static GlobalRightsAdministrationSectionPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "editor=globaladmin&section=Rights");
        return new GlobalRightsAdministrationSectionPage();
    }

    /** Checks the "always authenticate user for view" option. */
    public void forceAuthenticatedView()
    {
        if (!this.forceAuthenticatedViewLink.getAttribute("alt").equals("yes")) {
            this.forceAuthenticatedViewLink.click();
            waitUntilElementHasAttributeValue(By.id(this.forceAuthenticatedViewLink.getAttribute("id")), "alt", "yes");
        }
    }

    /** Unchecks the "always authenticate user for view" option. */
    public void unforceAuthenticatedView()
    {
        if (!this.forceAuthenticatedViewLink.getAttribute("alt").equals("no")) {
            this.forceAuthenticatedViewLink.click();
            waitUntilElementHasAttributeValue(By.id(this.forceAuthenticatedViewLink.getAttribute("id")), "alt", "no");
        }
    }

    public void disableCAPTCHA()
    {
        if (this.captchaCheckBox.isSelected()) {
            this.captchaCheckBox.click();
        }
    }

    public void enableCAPTCHA()
    {
        if (!this.captchaCheckBox.isSelected()) {
            this.captchaCheckBox.click();
        }
    }

    public EditRightsPane getEditRightsPane()
    {
        return this.editRightsPane;
    }
}
