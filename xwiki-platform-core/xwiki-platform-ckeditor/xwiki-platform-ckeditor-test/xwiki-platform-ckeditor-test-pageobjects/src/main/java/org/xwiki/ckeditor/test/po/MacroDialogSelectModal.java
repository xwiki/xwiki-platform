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
package org.xwiki.ckeditor.test.po;

import java.util.Optional;
import java.util.function.Consumer;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the macro selection modal.
 *
 * @version $Id$
 * @since 14.9
 */
public class MacroDialogSelectModal extends BaseElement
{
    private final Consumer<Runnable> submitHandler;

    /**
     * Creates a page object for a macro selection modal whose edit modal only clicks its submit button.
     */
    public MacroDialogSelectModal()
    {
        this(Runnable::run);
    }

    /**
     * Creates a page object for a macro selection modal whose edit modal lets the editor that opened it wait for the
     * result of the submission.
     *
     * @param submitHandler passed to the macro edit modal, see
     *            {@link MacroDialogEditModal#MacroDialogEditModal(Consumer)}
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public MacroDialogSelectModal(Consumer<Runnable> submitHandler)
    {
        this.submitHandler = submitHandler;
    }

    /**
     * Wait until the macro selection modal is loaded.
     *
     * @return the current page object
     */
    public MacroDialogSelectModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector(".macro-selector"));
        return this;
    }

    /**
     * Filter the list of macro by text.
     *
     * @param value the text to filter (e.g., "Pages")
     * @param expectedCount the expected number of macros listed after filtering
     */
    public void filterByText(String value, long expectedCount)
    {
        XWikiWebDriver driver = getDriver();
        WebElement macroSelectorFilter = driver.findElement(By.cssSelector(".macro-textFilter"));
        macroSelectorFilter.clear();
        macroSelectorFilter.sendKeys(value);
        // Wait until a single macro is proposed.
        driver.waitUntilCondition(webDriver -> countDisplayedMacros() == expectedCount);
    }

    /**
     * @return the first listed macro
     */
    public Optional<WebElement> getFirstMacro()
    {
        return getDriver().findElements(By.cssSelector(".macro-name"))
            .stream()
            .filter(WebElement::isDisplayed)
            .findFirst();
    }

    /**
     * Click on the macro selection button.
     *
     * @return the page object for the macro dialog edition modal
     */
    public MacroDialogEditModal clickSelect()
    {
        getDriver().findElement(By.cssSelector("[class*=-selector-modal] .modal-footer .btn-primary")).click();
        return new MacroDialogEditModal(this.submitHandler).waitUntilReady();
    }

    private long countDisplayedMacros()
    {
        return getDriver().findElements(By.cssSelector(".macro-name"))
            .stream()
            .filter(WebElement::isDisplayed)
            .count();
    }
}
