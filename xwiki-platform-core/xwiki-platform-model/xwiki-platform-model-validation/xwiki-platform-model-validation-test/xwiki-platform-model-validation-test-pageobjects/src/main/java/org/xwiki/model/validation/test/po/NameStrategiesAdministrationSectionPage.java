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
package org.xwiki.model.validation.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.test.ui.po.Select;

/**
 * Represents the Name Strategies administration section, where the entity name validation strategy can be selected and
 * saved, and where a name can be entered to test the currently selected strategy.
 *
 * @version $Id$
 * @since 17.10.10
 * @since 18.4.3
 * @since 18.5.0RC1
 */
public class NameStrategiesAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String SECTION_ID = "nameStrategies";

    private static final By CONFIG_FORM = By.id("entityNameValidationConfigForm");

    private static final By STRATEGY_SELECT =
        By.id("XWiki.EntityNameValidation.ConfigurationClass_0_currentStrategy");

    private static final By TEST_NAME_INPUT = By.id("testNameStrategy");

    private static final By TEST_VALID_TRUE = By.id("testNameStrategyIsValid_true");

    private static final By TEST_TRANSFORMED_NAME = By.id("testNameStrategyTransformedName");

    /**
     * Open the Name Strategies administration section.
     *
     * @return the Name Strategies administration section
     */
    public static NameStrategiesAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(SECTION_ID);
        return new NameStrategiesAdministrationSectionPage();
    }

    public NameStrategiesAdministrationSectionPage()
    {
        super(SECTION_ID);
    }

    /**
     * Select the entity name validation strategy to use. This only changes the selected value in the form; call
     * {@link #save()} to persist it.
     *
     * @param value the strategy hint to select (e.g. {@code "ReplaceCharacterEntityNameValidation"} or
     *            {@code "SlugEntityNameValidation"})
     */
    public void selectStrategy(String value)
    {
        new Select(getDriver().findElement(STRATEGY_SELECT)).selectByValue(value);
    }

    /**
     * Save the configuration. This persists the selected strategy and its configuration (which is necessary, for the
     * Slug strategy, so that its configuration properties are initialized before the strategy is tested).
     */
    public void save()
    {
        // The save button of this section's form has no name attribute, so the generic AdministrationSectionPage save
        // logic cannot be used. Submit the form's submit button directly instead.
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElement(CONFIG_FORM).findElement(By.cssSelector("input[type='submit']")).click();
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Enter a name in the "test selected strategy" field, then wait until the asynchronously computed result matches the
     * expected transformed name and verify the validity.
     * <p>
     * The name is set and an {@code input} event is dispatched on every polling iteration, which makes the method robust
     * against (a) the asynchronous loading of the section's JavaScript (the {@code input} handler that triggers the test
     * might not be bound yet on the first attempt) and (b) out-of-order responses (all the requests carry the same name,
     * so the displayed result converges to the expected one).
     *
     * @param name the name to test against the selected strategy
     * @param expectedValid the expected validity of the tested name
     * @param expectedTransformedName the expected transformed name
     */
    public void assertTestResult(String name, boolean expectedValid, String expectedTransformedName)
    {
        WebElement input = getDriver().findElement(TEST_NAME_INPUT);
        getDriver().waitUntilCondition(driver -> {
            getDriver().executeJavascript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));", input, name);
            return expectedTransformedName.equals(getTransformedName());
        });
        if (isTestedNameValid() != expectedValid) {
            throw new AssertionError(String.format(
                "The name [%s] was expected to be [%s] but was [%s] (transformed name [%s])", name,
                expectedValid ? "valid" : "invalid", isTestedNameValid() ? "valid" : "invalid",
                expectedTransformedName));
        }
    }

    /**
     * @return {@code true} if the last tested name was reported as valid for the selected strategy
     */
    private boolean isTestedNameValid()
    {
        return getDriver().findElementWithoutWaiting(TEST_VALID_TRUE).isDisplayed();
    }

    /**
     * @return the transformed version of the last tested name, as computed by the selected strategy
     */
    private String getTransformedName()
    {
        return getDriver().findElement(TEST_TRANSFORMED_NAME).getText();
    }
}
