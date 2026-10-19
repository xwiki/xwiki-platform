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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @version $Id$
 * @since 15.9RC1
 */
public class TourHomePage extends ViewPage
{
    @FindBy(xpath = "//a[@href='#AddNewEntry']")
    private WebElement addNewEntry;

    @FindBy(xpath = "//div[@id='entryNamePopup']/input[@type='text']")
    private WebElement entryNameInput;

    @FindBy(xpath = "//div[@id='entryNamePopup']/input[@type='image']")
    private WebElement entryNameButton;

    @FindBy(xpath = "//tbody[@id='tour-display']//tr")
    private List<WebElement> tours;

    /**
     * Opens the home page.
     */
    public static TourHomePage gotoPage()
    {
        getUtil().gotoPage("Tour", "WebHome");
        return new TourHomePage();
    }

    public TourEditPage addNewEntry(String entryName)
    {
        addNewEntry.click();
        entryNameInput.sendKeys(entryName);
        entryNameButton.click();
        return new TourEditPage();
    }

    private TourFromLivetable getTourFromRow(WebElement row)
    {
        WebElement name = row.findElement(By.xpath("td[contains(@class, 'doc_title')]/a"));
        WebElement isActive = row.findElement(By.className("isActive"));
        WebElement targetPage = row.findElement(By.className("targetPage"));
        WebElement targetClass = row.findElement(By.className("targetClass"));
        return new TourFromLivetable(name.getText(), targetPage.getText(), "Yes".equals(isActive.getText()),
            targetClass.getText());
    }

    public List<TourFromLivetable> getTours()
    {
        List<TourFromLivetable> results = new ArrayList<>();
        for (WebElement tour : tours) {
            results.add(getTourFromRow(tour));
        }
        return results;
    }

    public ViewPage getTourPage(String tourName)
    {
        for (WebElement row : tours) {
            WebElement name = row.findElement(By.xpath("td[contains(@class, 'doc_title')]/a"));
            if (tourName.equals(name.getText())) {
                name.click();
                return new ViewPage();
            }
        }

        return null;
    }

    public PageWithTour startTour(String tourName)
    {
        for (WebElement row : tours) {
            TourFromLivetable tour = getTourFromRow(row);
            if (tourName.equals(tour.getName())) {
                WebElement launch =
                    row.findElement(By.xpath("//td[contains(@class, 'actions')]/a[contains(@class, 'actionLaunch')]"));
                String tourHomeURL = getDriver().getCurrentUrl();
                launch.click();
                waitUntilTourPageIsLoaded(tourHomeURL);
                return new PageWithTour();
            }
        }

        return null;
    }

    /**
     * The launch link opens the target page of the tour with {@code startTour=true} in the query string, and then the
     * tour's JavaScript redirects to the same page without that parameter, since that's the URL recorded in the tour.
     * Wait until the current URL is the one of this second page load, so that the page object we return looks for its
     * elements in the final document. Otherwise a lookup can run against a document that is being replaced and fail
     * (e.g. with "Node with given id does not belong to the document" on Chrome).
     *
     * @param tourHomeURL the URL of the tour home page, from which the tour is launched
     */
    private void waitUntilTourPageIsLoaded(String tourHomeURL)
    {
        getDriver().waitUntilCondition(driver -> {
            String currentURL = driver.getCurrentUrl();
            return !tourHomeURL.equals(currentURL) && !currentURL.contains("startTour=true");
        });
    }
}
