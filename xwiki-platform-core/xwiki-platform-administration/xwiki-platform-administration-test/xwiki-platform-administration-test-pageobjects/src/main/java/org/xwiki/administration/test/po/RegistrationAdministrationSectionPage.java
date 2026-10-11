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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the Registration administration section (Administer Wiki &gt; Users &amp; Rights &gt; Registration).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class RegistrationAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Registration";

    /**
     * The "Use Email Verification" dropdown: "1" means Yes and "0" means No.
     */
    @FindBy(name = "XWiki.XWikiPreferences_0_use_email_verification")
    private WebElement useEmailVerificationSelect;

    @FindBy(css = "#admin-page-content > .bottombuttons input[name='formactionsac']")
    private WebElement saveButton;

    /**
     * Default constructor.
     */
    public RegistrationAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * Open the Registration administration section.
     *
     * @return the Registration administration section
     */
    public static RegistrationAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new RegistrationAdministrationSectionPage();
    }

    /**
     * Set whether new users must validate their email address before their account is activated, and save.
     *
     * @param enabled {@code true} to require the email verification, {@code false} otherwise
     */
    public void setUseEmailVerification(boolean enabled)
    {
        new Select(this.useEmailVerificationSelect).selectByValue(enabled ? "1" : "0");
        // The section holds several forms (the registration preferences and the registration configuration). Their
        // own save buttons are replaced by a single button that saves all the forms asynchronously.
        this.saveButton.click();
        waitForNotificationSuccessMessage("Saved");
    }

    /**
     * @return {@code true} if new users must validate their email address before their account is activated
     */
    public boolean isUseEmailVerification()
    {
        return "1".equals(
            new Select(this.useEmailVerificationSelect).getFirstSelectedOption().getAttribute("value"));
    }
}
