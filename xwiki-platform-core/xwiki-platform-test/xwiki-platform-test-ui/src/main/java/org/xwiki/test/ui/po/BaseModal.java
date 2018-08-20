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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

/**
 * getTitle(), close() and isVisible() (with protected waitForVisible() and waitForClosed())
 */

public class BaseModal extends BaseElement
{
    protected WebElement container;

    public BaseModal(By selector)
    {
        this.container = getDriver().findElement(selector);
        // The fade effect is deleted from the modal because there isn't an easy
        // way for waiting on the modal to be shown.
        // This fade in effect is also not necessary for the test.
        String className = this.container.getAttribute("class");
        className = className.replace("fade", "");
        getDriver().executeScript("arguments[0].setAttribute(\"class\",arguments[1])", this.container, className);
    }

    public String getTitle()
    {
        return this.container.findElement(By.className("modal-title")).getText();
    }

    public boolean isDisplayed()
    {
        return this.container.isDisplayed();
    }

    public void close()
    {
        this.container.findElement(By.cssSelector(".modal-header .close")).click();
        waitForClosed();
    }

    /**
     * The modal may have a fade out effect on close which means it may not disappear instantly. It's safer to wait for
     * the modal to disappear when closed, before proceeding with next actions.
     * 
     * @return this model
     */
    protected BaseModal waitForClosed()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return !isDisplayed();
            }
        });
        return this;
    }

}
