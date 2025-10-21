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
package org.xwiki.realtime.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The dropdown that lists recent versions of the edited document.
 * 
 * @version $Id$
 * @since 17.9.0
 * @since 17.4.7
 * @since 16.10.13
 */
public class HistoryDropdown extends BaseElement
{
    private static final By DROPDOWN_MENU = By.cssSelector(".realtime-versions.dropdown-menu");

    /**
     * Clicks on the dropdown toggle to open the dropdown.
     * 
     * @return this instance
     */
    public HistoryDropdown open()
    {
        getToggle().click();
        getDriver().waitUntilElementIsVisible(DROPDOWN_MENU);
        return this;
    }

    /**
     * Uses the ESCAPE key to close the dropdown.
     * 
     * @return this instance
     */
    public HistoryDropdown close()
    {
        getToggle().sendKeys(Keys.ESCAPE);
        getDriver().waitUntilElementDisappears(DROPDOWN_MENU);
        return this;
    }

    private WebElement getToggle()
    {
        return getDriver().findElement(By.cssSelector(".realtime-action-history.dropdown-toggle"));
    }

    /**
     * @return the document versions listed in the history dropdown
     */
    public List<VersionElement> getVersions()
    {
        return getDriver().findElements(By.cssSelector(".realtime-versions .realtime-version")).stream()
            .map(VersionElement::new).toList();
    }

    /**
     * Waits for the version with the given version number to be listed in the history dropdown.
     * 
     * @param versionNumber the version number to wait for
     * @return this instance
     */
    public HistoryDropdown waitForVersion(String versionNumber)
    {
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".realtime-version[data-version*=\"\\\"" + versionNumber + "\\\"\"]")));
        return this;
    }

    /**
     * Clicks on the "Summarize Changes" entry from the history dropdown and waits for the corresponding modal to be
     * displayed.
     * 
     * @return the modal used to review and summarize the changes
     */
    public SummaryModal summarizeChanges()
    {
        getDriver().findElement(By.cssSelector(".realtime-versions .realtime-action-summarize")).click();
        return new SummaryModal();
    }
}
