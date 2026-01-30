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

import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.test.ui.WCAGContext;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * The base class for a bootstrap modal.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class BaseModal extends BaseElement
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseModal.class);
    
    protected WebElement container;

    /**
     * Default constructor when we want to manually set the container. This should be used with caution.
     */
    protected BaseModal()
    {
    }

    // This constructor remove the fade effect, but it only takes effect before the modal is opened
    // so take care to create the modal before doing the action to open it.
    public BaseModal(By selector)
    {
        this.container = getDriver().findElement(selector);
        // The fade effect is deleted from the modal because there isn't an easy way for waiting on the modal to be
        // shown. This fade in effect is also not necessary for the test.
        String className = this.container.getAttribute("class");
        className = className.replace("fade", "");
        getDriver().executeScript("arguments[0].setAttribute(\"class\",arguments[1])", this.container, className);
        // Once we're sure the modal is loaded, we can validate its content in regards to accessibility
        className = this.container.getAttribute("class").split(" ")[0];
        validateWCAG(getUtil().getWCAGUtils().getWCAGContext(), true, "." + className);
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

    public void validateWCAG(WCAGContext wcagContext, boolean checkCache, String cssSelector) {
        if (!wcagContext.isWCAGEnabled()) {
            // Block WCAG validation if it is not enabled, in all cases.
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            // Run WCAG tests on the current UI page if the current URL + PO class name are not in the cache, or if checking
            // the cache is disabled.
            XWikiWebDriver driver = this.getDriver();
            if (!checkCache || wcagContext.isNotCached(driver.getCurrentUrl(), this.getClass().getName())) {
                AxeBuilder axeBuilder = wcagContext.getAxeBuilder();
                // Only analyze the modal itself
                axeBuilder.include(cssSelector);
                Results axeResult = axeBuilder.analyze(driver);
                wcagContext.addWCAGResults(driver.getCurrentUrl(), this.getClass().getName(), axeResult);
                long stopTime = System.currentTimeMillis();
                long deltaTime = stopTime - startTime;
                LOGGER.info("[{} : {}] WCAG Validation on this element took [{}] ms.",
                    driver.getCurrentUrl(), this.getClass().getName(), deltaTime);
                wcagContext.addWCAGTime(deltaTime);
            } else {
                // If the identifying pair is already in the cache, don't perform accessibility validation.
                LOGGER.debug("[{} : {}] This combination of URL:class was already WCAG-checked.",
                    driver.getCurrentUrl(), this.getClass().getName());
            }
        } catch (Exception e) {
            if (wcagContext.shouldWCAGStopOnError()) {
                throw e;
            } else {
                LOGGER.debug("Error during WCAG execution, but ignored thanks to wcagStopOnError flag: ", e);
            }
        }
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
