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
package org.xwiki.guidedtour.test.po;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

public class TourWidget extends ViewPage
{
    private static final Pattern PROGRESS_STYLE_WIDTH_PATTERN = Pattern.compile("width:\\s*(\\d+(?:\\.\\d+)?)%.*");

    @FindBy(css = ".guidedtour-widget")
    private WebElement widget;

    /**
     * @return the ids of the tours currently visible in the widget.
     */
    public List<String> getTourIds()
    {
        return widget.findElements(By.cssSelector("section.guidedtour-tour")).stream()
            .filter(this::isTourVisible)
            .map(this::getElementId)
            .collect(Collectors.toList());
    }

    /**
     * @return the ids of all visible tasks for every visible tour.
     */
    public List<String> getVisibleTaskIds()
    {
        if (isWidgetCollapsed()) {
            return Collections.emptyList();
        }

        return widget.findElements(By.cssSelector("section.guidedtour-tour")).stream()
            .filter(this::isTourExpanded)
            .flatMap(tour -> tour.findElements(By.cssSelector(".guidedtour-task")).stream())
            .filter(WebElement::isDisplayed)
            .map(this::getElementId)
            .collect(Collectors.toList());
    }

    /**
     * @param tourId the tour id
     * @return the ids of all visible tasks for the requested tour.
     */
    public List<String> getVisibleTaskIds(String tourId)
    {
        if (isWidgetCollapsed()) {
            return Collections.emptyList();
        }

        try {
            WebElement tour = widget.findElement(By.xpath(".//section[contains(@class,'guidedtour-tour') and @id=" + escapeXPathString(tourId) + "]"));
            if (!isTourVisible(tour) || tour.getAttribute("class").contains("collapsed")) {
                return Collections.emptyList();
            }

            return tour.findElements(By.cssSelector(".guidedtour-task")).stream()
                .filter(WebElement::isDisplayed)
                .map(this::getElementId)
                .collect(Collectors.toList());
        } catch (NoSuchElementException e) {
            return Collections.emptyList();
        }
    }

    public boolean isWidgetCollapsed()
    {
        return widget.getAttribute("class").contains("collapsed");
    }

    public void toggleWidgetCollapsed()
    {
        widget.findElement(By.cssSelector("button#widget-close")).click();
    }

    public boolean isTourCollapsed(String tourId)
    {
        try {
            WebElement tour = getTourElement(tourId);
            return tour.getAttribute("class").contains("collapsed");
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void toggleTourCollapsed(String tourId)
    {
        getTourHeader(tourId).click();
    }

    public void clickTask(String taskId)
    {
        widget.findElement(By.xpath(".//div[contains(@class,'guidedtour-task') and @id=" + escapeXPathString(taskId) + "]")).click();
    }

    public void skipTask(String taskId)
    {
        clickTaskButton(taskId, "fa-solid fa-x");
    }

    public void resetTask(String taskId)
    {
        clickTaskButton(taskId, "fa fa-rotate-right");
    }

    public void skipTour(String tourId)
    {
        clickTourButton(tourId, "fa-solid fa-x");
    }

    public void resetTour(String tourId)
    {
        clickTourButton(tourId, "fa fa-rotate-right");
    }

    public float getProgress()
    {
        String style = widget.findElement(By.cssSelector(".completeness-progress")).getAttribute("style");
        Matcher matcher = PROGRESS_STYLE_WIDTH_PATTERN.matcher(style);
        if (matcher.matches()) {
            return Float.parseFloat(matcher.group(1)) / 100.0f;
        }

        return 0.0f;
    }

    private WebElement getTourElement(String tourId)
    {
        return widget.findElement(By.xpath(".//section[contains(@class,'guidedtour-tour') and @id=" + escapeXPathString(tourId) + "]"));
    }

    private WebElement getTourHeader(String tourId)
    {
        return widget.findElement(By.xpath(".//section[contains(@class,'guidedtour-tour') and @id=" + escapeXPathString(tourId) + "]//div[contains(@class,'guidedtour-tour-header')]"));
    }

    private void clickTaskButton(String taskId, String iconClass)
    {
        widget.findElement(By.xpath(".//div[contains(@class,'guidedtour-task') and @id=" + escapeXPathString(taskId) + "]//button[contains(@class,'post-btn')]//i[contains(@class,'" + iconClass + "')]/.."))
            .click();
    }

    private void clickTourButton(String tourId, String iconClass)
    {
        widget.findElement(By.xpath(".//section[contains(@class,'guidedtour-tour') and @id=" + escapeXPathString(tourId) + "]//button[contains(@class,'post-btn')]//i[contains(@class,'" + iconClass + "')]/.."))
            .click();
    }

    private boolean isTourVisible(WebElement tourElement)
    {
        String cssClass = tourElement.getAttribute("class");
        return cssClass != null && !cssClass.contains("hidden");
    }

    private String getElementId(WebElement element)
    {
        return element.getAttribute("id");
    }

    private static String escapeXPathString(String value)
    {
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        String[] parts = value.split("\"");
        return "concat(" + Arrays.stream(parts)
            .map(part -> "\"" + part + "\"")
            .collect(Collectors.joining(", '"', ")) + ")";
    }
}
