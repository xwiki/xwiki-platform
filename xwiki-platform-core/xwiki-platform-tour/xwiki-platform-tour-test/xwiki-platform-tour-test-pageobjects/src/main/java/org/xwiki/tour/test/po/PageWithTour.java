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
package org.xwiki.tour.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The page with tour elements.
 * 
 * @version $Id$
 * @since 15.9RC1
 */
public class PageWithTour extends ViewPage
{
    public static PageWithTour gotoPage(String space, String page)
    {
        return gotoPage(new LocalDocumentReference(space, page));
    }

    public static PageWithTour gotoPage(EntityReference entityReference)
    {
        String url = getUtil().getURL(entityReference, "view", null);
        // Remove the "WebHome" suffix from the URL of nested pages in order to match the URL expected by the tour.
        // Otherwise resuming the tour will "reload" the page in order to have the expected URL.
        if (url.endsWith("/WebHome")) {
            url = url.substring(0, url.length() - "WebHome".length());
        }
        getUtil().gotoPage(url);
        return new PageWithTour();
    }

    public String getUrl()
    {
        return getDriver().getCurrentUrl();
    }

    public boolean isTourDisplayed()
    {
        return getDriver().hasElement(By.className("tour"));
    }

    public String getStepTitle()
    {
        return getShownStepElement(By.className("popover-title"));
    }

    public String getStepDescription()
    {
        return getShownStepElement(By.className("popover-content"));
    }

    /**
     * Read the text of an element of the step popover, waiting for the popover to be displayed first.
     * <p>
     * The text is read again on every poll until it is not empty, for two reasons:
     * <ul>
     * <li>The step popover is shown by the tour's JavaScript right after the target page has been loaded, so a lookup
     * for it is typically issued while that navigation is still settling. A single {@code findElement} spanning a
     * navigation keeps polling the document we're leaving and fails after its full timeout, whereas each poll runs a
     * new lookup against the current document. Note that when we start a new tour from the tour home page, there are
     * actually two page loads: one that contains startTour=true and then the tour's JavaScript code redirects to the
     * same URL without that parameter as this is the URL recorded in the tour.</li>
     * <li>When the tour scrolls the page to the element of a step, it shows the step popover twice: once at the end of
     * the scroll animation and once more from its (debounced) window scroll listener. Each time, the popover is
     * replaced by a new one that fades in, and {@code getText()} returns an empty string while it's still invisible
     * (or fails if the element we found has just been replaced).</li>
     * </ul>
     *
     * @param locator the locator of the popover element to read
     * @return the text of the element, once it is displayed
     */
    private String getShownStepElement(By locator)
    {
        return getDriver().waitUntilCondition(driver -> {
            try {
                String text = driver.findElement(locator).getText();
                return text.isEmpty() ? null : text;
            } catch (StaleElementReferenceException e) {
                return null;
            }
        });
    }

    public boolean hasPreviousStep()
    {
        return getDriver().hasElementWithoutWaiting(By.xpath("//a[@id='bootstrap_tour_prev']"));
    }

    public boolean hasNextStep()
    {
        return getDriver().hasElementWithoutWaiting(By.xpath("//a[@id='bootstrap_tour_next']"));
    }

    public boolean hasEndButton()
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[contains(@class, 'popover-navigation')]//a[@id='bootstrap_tour_end']"));
    }

    private String getStepId()
    {
        return getDriver().findElement(By.className("tour")).getAttribute("id");
    }

    public void previousStep()
    {
        // Get the current step id
        String stepId = getStepId();
        // Click
        clickStepButton(By.xpath("//a[@id='bootstrap_tour_prev']"));
        // Wait until current state disappears
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(stepId))));
        // Wait until new step appears
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    public void nextStep()
    {
        // Get the current step id
        String stepId = getStepId();
        // Click
        clickStepButton(By.xpath("//a[@id='bootstrap_tour_next']"));
        // Wait until current state disappears
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(stepId))));
        // Wait until new step appear
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    public void end()
    {
        clickStepButton(By.xpath("//div[contains(@class, 'popover-navigation')]//a[@id='bootstrap_tour_end']"));
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tour"))));
    }

    public void close()
    {
        clickStepButton(By.xpath("//a[@id='bootstrap_tour_close']"));
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tour"))));
    }

    public boolean hasResumeButton()
    {
        return getDriver().hasElementWithoutWaiting(By.id("tourResume")) 
            && (getDriver().findElement(By.id("tourResume")).getAttribute("hidden") == null);
    }

    public void resume()
    {
        getDriver().findElement(By.id("tourResume")).click();
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    /**
     * Forgets the state of all the tours (current step, ended), which the tours keep in the local storage of the
     * browser, then reloads the page so that its tours start again from their first step, as for a new visitor.
     *
     * @return the reloaded page
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public PageWithTour clearToursState()
    {
        getDriver().executeJavascript("Object.keys(window.localStorage).filter(key => key.startsWith('tour_'))"
            + ".forEach(key => window.localStorage.removeItem(key));");
        getDriver().navigate().refresh();
        return new PageWithTour();
    }

    /**
     * @param linkLabel the label of a link in the description of the displayed step
     * @return the URL targeted by that link
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public String getStepDescriptionLinkURL(String linkLabel)
    {
        // Look for the link on every poll, for the same reasons as in getShownStepElement(): the link text can't be
        // matched while the popover is invisible, and the popover can be replaced by a new one meanwhile.
        return getDriver().waitUntilCondition(driver -> {
            try {
                return driver.findElement(By.cssSelector(".tour .popover-content")).findElement(By.linkText(linkLabel))
                    .getDomAttribute("href");
            } catch (StaleElementReferenceException e) {
                return null;
            }
        });
    }

    /**
     * @return the hint displayed next to the resume button when a tour ends, even once it has been hidden
     * @since 17.10.14
     * @since 18.4.7
     * @since 18.9.0RC1
     */
    public String getResumeHint()
    {
        return getDriver().findElement(By.cssSelector("#tourResumeContainer .popover-content"))
            .getDomProperty("textContent").trim();
    }

    /**
     * Click a button of the displayed step popover, once that popover has stopped moving.
     * <p>
     * When the tour scrolls the page to the element of a step, the step popover can be displayed while the page is
     * still scrolling (it's shown both at the end of the scroll animation and from the tour's debounced window scroll
     * listener), so it keeps moving for a moment after it becomes visible. WebDriver computes where to click before
     * dispatching the click, so a click on a moving popover can land on the tour backdrop instead of the button, and
     * then the tour silently stays on the same step. Hence we wait until the popover is fully shown and keeps the same
     * position in the viewport between two polls.
     *
     * @param buttonLocator the locator of the button to click
     */
    private void clickStepButton(By buttonLocator)
    {
        String[] lastPosition = new String[1];
        getDriver().waitUntilCondition(driver -> {
            Object position = getDriver().executeJavascript("const popover = document.querySelector('.tour.in');"
                + "return popover && getComputedStyle(popover).opacity === '1'"
                + " ? JSON.stringify(popover.getBoundingClientRect()) : null;");
            boolean settled = position != null && position.equals(lastPosition[0]);
            lastPosition[0] = (String) position;
            return settled;
        });
        getDriver().findElement(buttonLocator).click();
    }
}
