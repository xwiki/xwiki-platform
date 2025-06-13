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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the modal dialog for managing required rights.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
public class RequiredRightsModal extends BaseModal
{
    private static final String VALUE_ATTRIBUTE = "value";

    /**
     * Represents a required right as displayed in the required rights modal.
     *
     * @param name the internal name of the required right
     * @param label the displayed label
     * @param enabled if the right can be selected
     * @param suggestionClass the suggestion class, this can be one of enough, maybe-enough, required, maybe-required
     * @param suggestionText the suggestion text that is displayed to the user
     * @param suggestionTooltip the suggestion tooltip that explains the suggestion
     */
    public record RequiredRight(String name, String label, boolean enabled, String suggestionClass,
                                String suggestionText, String suggestionTooltip)
    {
    }

    /**
     * Default constructor.
     */
    public RequiredRightsModal()
    {
        super(By.id("required-rights-dialog"));
        getDriver().waitUntilCondition(driver -> isDisplayed());
    }

    /**
     * @return if enforcing required rights is enabled
     */
    public boolean isEnforceRequiredRights()
    {
        return this.container.findElement(By.cssSelector("input[name='enforceRequiredRights'][value='1']"))
            .isSelected();
    }

    /**
     * @param enforceRequiredRights set the enforced status
     */
    public void setEnforceRequiredRights(boolean enforceRequiredRights)
    {
        int enforcementValue = enforceRequiredRights ? 1 : 0;
        this.container.findElement(
                By.cssSelector("input[name='enforceRequiredRights'][value='" + enforcementValue + "']"))
            .click();
    }

    /**
     * @return get internal name of the currently enforced required right
     */
    public String getEnforcedRequiredRight()
    {
        return this.container.findElement(By.cssSelector("input[name='rights']:checked"))
            .getDomAttribute(VALUE_ATTRIBUTE);
    }

    /**
     * @param enforcedRequiredRight the internal name of the required right to enforce
     * @return the internal name of the currently enforced required right after setting
     */
    public String setEnforcedRequiredRight(String enforcedRequiredRight)
    {
        this.container.findElement(
                By.cssSelector("label:has(input[name='rights'][value='" + enforcedRequiredRight + "'])"))
            .click();
        return this.getEnforcedRequiredRight();
    }

    /**
     * @return the list of required rights that are available in the modal
     */
    public List<RequiredRight> getRequiredRights()
    {
        return this.container.findElements(By.cssSelector(".rights-selection li")).stream()
            .map(li -> {
                WebElement requiredRightInput = li.findElement(By.cssSelector("input[name='rights']"));
                String name = requiredRightInput.getDomAttribute(VALUE_ATTRIBUTE);
                String label = li.findElement(By.cssSelector(".label-wrapper label")).getText().trim();
                boolean enabled = requiredRightInput.isEnabled();
                String suggestionClass = li.getDomAttribute("class");
                List<WebElement> suggestionElement = getDriver().findElementsWithoutWaiting(li, By.tagName("p"));
                String suggestionText;
                String suggestionTooltip;
                if (suggestionElement.isEmpty()) {
                    suggestionText = null;
                    suggestionTooltip = null;
                } else {
                    suggestionText = suggestionElement.get(0).getText();
                    WebElement tooltipButton = suggestionElement.get(0).findElement(By.tagName("button"));
                    // Remove the text inside the button from the suggestion text as it's just the label for screen
                    // readers that explains the button's purpose.
                    suggestionText = suggestionText.replace(tooltipButton.getText(), "").trim();
                    suggestionTooltip = tooltipButton.getDomAttribute("data-original-title");
                }
                return new RequiredRight(name, label, enabled, suggestionClass, suggestionText, suggestionTooltip);
            })
            .toList();
    }

    /**
     * Saves the required rights.
     *
     * @param wait if true, wait for the notification success message
     */
    public void clickSave(boolean wait)
    {
        this.container.findElement(By.cssSelector(".modal-footer button.btn-primary")).click();

        if (wait) {
            waitForNotificationSuccessMessage("Saved");
        }
    }

    /**
     * Clicks the cancel button to dismiss this dialog without saving.
     */
    public void clickCancel()
    {
        this.container.findElement(By.cssSelector(".modal-footer button.btn-default")).click();
    }

    /**
     * @return if required rights analysis details are available and can be displayed by toggling them
     */
    public boolean hasAnalysisDetails()
    {
        return getAdvancedToggle().isDisplayed();
    }

    /**
     * Toggles the display of the required rights analysis details.
     */
    public void toggleAnalysisDetails()
    {
        getAdvancedToggle().click();
    }

    /**
     * @return if required rights analysis details are displayed
     */
    public boolean isAnalysisDetailsDisplayed()
    {
        return this.container.findElement(By.id("required-rights-results")).isDisplayed();
    }

    private WebElement getAdvancedToggle()
    {
        return this.container.findElement(By.cssSelector(".required-rights-advanced-toggle"));
    }
}
