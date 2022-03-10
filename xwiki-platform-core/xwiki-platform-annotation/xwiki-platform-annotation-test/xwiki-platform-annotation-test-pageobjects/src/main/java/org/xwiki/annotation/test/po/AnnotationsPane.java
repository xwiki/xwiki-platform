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
package org.xwiki.annotation.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Pane that opens when you click on the Annotation menu entry.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class AnnotationsPane extends BaseElement
{
    private static final By PANE_CONTAINER = By.className("annotationsettings");

    @FindBy(id = "annotationsdisplay")
    private WebElement checkBox;

    private void clickOnAnnotationMenu()
    {
        // Open the page menu
        getDriver().findElement(By.id("tmMoreActions")).click();
        // Wait for the drop down menu to be displayed
        getDriver().waitUntilElementIsVisible(By.id("tmMoreActions"));
        // Click on the annotations button
        WebElement annotationsPane = getDriver().findElement(By.id("tmAnnotationsTrigger"));
        annotationsPane.click();
    }

    /**
     * Shows the annotations pane from the top of the page.
     */
    public void showAnnotationsPane()
    {
        showAnnotationsPane(true);
    }

    /**
     * Show the annotation settings pane from the top of the page.
     * 
     * @param wait whether to wait for the pane to be visible or not
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public void showAnnotationsPane(boolean wait)
    {
        clickOnAnnotationMenu();
        if (wait) {
            // Verify that the annotationssettings panel is open
            getDriver().waitUntilElementIsVisible(PANE_CONTAINER);
        }
    }

    /**
     * Hides the annotations pane from the top of the page
     */
    public void hideAnnotationsPane()
    {
        clickOnAnnotationMenu();
        // Verify that the annotationssettings panel is close
        getDriver().waitUntilElementDisappears(PANE_CONTAINER);
    }

    /**
     * Checks the "Show Annotations" check box.
     */
    public void clickShowAnnotations()
    {
        if (this.checkBox.isSelected() == false) {
            this.checkBox.click();
        }
    }

    /**
     * Un-Checks the "Show Annotations" checkbox.
     */
    public void clickHideAnnotations()
    {
        if (this.checkBox.isSelected() == true) {
            this.checkBox.click();
        }
    }

    /**
     * Checks if the checkBox within AnnotationsPane is visible
     *
     * @return returns true if the Show Annotations checkbox is displayed
     */
    public boolean checkIfShowAnnotationsCheckboxExists()
    {
        if (getDriver().findElementsWithoutWaiting(By.id("annotationsdisplay")).size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@code true} if the annotation settings pane is displayed, {@code false} otherwise
     * @since 12.10.9
     * @since 13.4.1
     * @since 13.5RC1
     */
    public boolean isDisplayed()
    {
        List<WebElement> panes = getDriver().findElementsWithoutWaiting(PANE_CONTAINER);
        return !panes.isEmpty() && panes.get(0).isDisplayed();
    }
}
