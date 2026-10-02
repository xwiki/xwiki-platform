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

import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the Authentication administration section (Administer Wiki &gt; Users &amp; Rights &gt;
 * Authentication), and more specifically its "Authentication Security" form, used to configure what happens when
 * repeated authentication failures occur.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class AuthenticationAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "Authentication";

    @FindBy(id = "XWiki.Authentication.ConfigurationClass_0_isAuthenticationSecurityEnabled")
    private WebElement securityEnabledSelect;

    @FindBy(id = "XWiki.Authentication.ConfigurationClass_0_failureStrategy")
    private WebElement failureStrategySelect;

    @FindBy(id = "XWiki.Authentication.ConfigurationClass_0_maxAuthorizedAttempts")
    private WebElement maxAuthorizedAttemptsInput;

    @FindBy(css = "#authenticationSecurityConfigForm input[type='submit']")
    private WebElement saveButton;

    /**
     * Default constructor.
     */
    public AuthenticationAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * @return the Authentication administration section of the main wiki
     */
    public static AuthenticationAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new AuthenticationAdministrationSectionPage();
    }

    /**
     * @return {@code true} if the authentication security (i.e. the failure strategies) is enabled
     */
    public boolean isAuthenticationSecurityEnabled()
    {
        return "1".equals(new Select(this.securityEnabledSelect).getFirstSelectedOption().getAttribute("value"));
    }

    /**
     * Enables or disables the authentication security. The other fields of the form can only be modified when it is
     * enabled.
     *
     * @param enabled {@code true} to enable the authentication security, {@code false} to disable it
     */
    public void setAuthenticationSecurityEnabled(boolean enabled)
    {
        new Select(this.securityEnabledSelect).selectByValue(enabled ? "1" : "0");
        getDriver().waitUntilCondition(driver -> this.failureStrategySelect.isEnabled() == enabled);
    }

    /**
     * @return the identifiers of the selected failure strategies (e.g. {@code captcha}, {@code disableAccount})
     */
    public List<String> getFailureStrategies()
    {
        return new Select(this.failureStrategySelect).getAllSelectedOptions().stream()
            .map(option -> option.getAttribute("value")).toList();
    }

    /**
     * Replaces the selected failure strategies.
     *
     * @param strategies the identifiers of the failure strategies to select (e.g. {@code captcha},
     *     {@code disableAccount})
     */
    public void setFailureStrategies(String... strategies)
    {
        Select select = new Select(this.failureStrategySelect);
        select.deselectAll();
        for (String strategy : strategies) {
            select.selectByValue(strategy);
        }
    }

    /**
     * @return the maximum number of authorized authentication failures before the failure strategies are activated
     */
    public int getMaxAuthorizedAttempts()
    {
        return Integer.parseInt(this.maxAuthorizedAttemptsInput.getAttribute("value"));
    }

    /**
     * @param maxAuthorizedAttempts the maximum number of authorized authentication failures before the failure
     *     strategies are activated
     */
    public void setMaxAuthorizedAttempts(int maxAuthorizedAttempts)
    {
        this.maxAuthorizedAttemptsInput.clear();
        this.maxAuthorizedAttemptsInput.sendKeys(String.valueOf(maxAuthorizedAttempts));
    }

    /**
     * Saves the Authentication Security form. Its own save button submits the form to the configuration page, which
     * then redirects back to this section.
     */
    @Override
    public void clickSave()
    {
        getDriver().addPageNotYetReloadedMarker();
        this.saveButton.click();
        getDriver().waitUntilPageIsReloaded();
    }
}
