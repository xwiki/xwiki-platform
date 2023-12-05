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
        return getDriver().findElement(By.className("popover-title")).getText();
    }

    public String getStepDescription()
    {
        return getDriver().findElement(By.className("popover-content")).getText();
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
        getDriver().findElement(By.xpath("//a[@id='bootstrap_tour_prev']")).click();
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
        getDriver().findElement(By.xpath("//a[@id='bootstrap_tour_next']")).click();
        // Wait until current state disappears
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(stepId))));
        // Wait until new step appear
        getDriver().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    public void end()
    {
        getDriver().findElement(By.xpath("//div[contains(@class, 'popover-navigation')]//a[@id='bootstrap_tour_end']"))
            .click();
        getDriver().waitUntilCondition(
            ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tour"))));
    }

    public void close()
    {
        getDriver().findElement(By.xpath("//a[@id='bootstrap_tour_close']")).click();
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
}
