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
package org.xwiki.contrib.tour.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id: $
 * @since 0.2
 */
public class PageWithTour extends ViewPage
{
    public static PageWithTour gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page);
        return new PageWithTour();
    }

    public String getUrl()
    {
        return getDriver().getCurrentUrl();
    }
    
    public boolean isTourDisplayed()
    {
        return getUtil().hasElement(By.className("tour"));
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
        return getUtil().hasElementWithoutWaiting(By.xpath("//a[@id='bootstrap_tour_prev']"));
    }

    public boolean hasNextStep()
    {
        return getUtil().hasElementWithoutWaiting(By.xpath("//a[@id='bootstrap_tour_next']"));
    }

    public boolean hasEndButton()
    {
        return getUtil().hasElementWithoutWaiting(
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
        getUtil().waitUntilCondition(
                ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(stepId))));
        // Wait until new step appears
        getUtil().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    public void nextStep()
    {
        // Get the current step id
        String stepId = getStepId();
        // Click
        getDriver().findElement(By.xpath("//a[@id='bootstrap_tour_next']")).click();
        // Wait until current state disappears
        getUtil().waitUntilCondition(
                ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id(stepId))));
        // Wait until new step appear
        getUtil().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }

    public void end()
    {
        getDriver().findElement(
                By.xpath("//div[contains(@class, 'popover-navigation')]//a[@id='bootstrap_tour_end']")).click();
        getUtil().waitUntilCondition(
                ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tour"))));
    }

    public void close()
    {
        getDriver().findElement(
                By.xpath("//a[@id='bootstrap_tour_close']")).click();
        getUtil().waitUntilCondition(
                ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("tour"))));
    }
    
    public boolean hasResumeButton()
    {
        return getUtil().hasElementWithoutWaiting(By.id("tourResume"));
    }

    public void resume()
    {
        getDriver().findElement(By.id("tourResume")).click();
        getUtil().waitUntilCondition(ExpectedConditions.presenceOfElementLocated(By.className("tour")));
    }
}
