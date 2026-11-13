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
package org.xwiki.livedata.test.po;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Provides the operations to interact with a Live Data when displayed with a card layout.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class CardLayoutElement extends BaseElement
{
    /**
     * Same timeout as {@link TableLayoutElement#waitUntilReady()}.
     */
    private static final int READY_TIMEOUT_SECONDS = 20;

    private static final String PROPERTY_NAME_SUFFIX = ":";

    private final String liveDataId;

    /**
     * Default constructor. Initializes a live data card layout page object.
     *
     * @param liveDataId the live data id
     */
    public CardLayoutElement(String liveDataId)
    {
        this.liveDataId = liveDataId;
    }

    /**
     * Waits until the cards are displayed, with the content of all their properties.
     *
     * @since 18.10.0RC1
     */
    public void waitUntilReady()
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                WebElement layout = getLayout();
                boolean loading = StringUtils.defaultString(
                    getRoot().findElement(By.className("layout-loader")).getAttribute("class")).contains("waiting");
                return !loading && !getCards().isEmpty()
                    && getDriver().findElementsWithoutWaiting(layout, By.cssSelector(".card .xwiki-loader")).isEmpty();
            } catch (NoSuchElementException | StaleElementReferenceException e) {
                // The layout is not displayed yet, or is being replaced.
                return false;
            }
        }, READY_TIMEOUT_SECONDS);
    }

    /**
     * @return the number of cards currently displayed, one per entry
     * @since 18.10.0RC1
     */
    public int countCards()
    {
        return getCards().size();
    }

    /**
     * @param cardNumber the card number, starting at 1
     * @param propertyLabel the label of a property, for instance {@code "Title"}
     * @return the text displayed for that property in the card
     * @throws NoSuchElementException if the card does not display that property
     * @since 18.10.0RC1
     */
    public String getPropertyValue(int cardNumber, String propertyLabel)
    {
        String propertyName = propertyLabel + PROPERTY_NAME_SUFFIX;
        return getCards().get(cardNumber - 1).findElements(By.className("card-property")).stream()
            .filter(WebElement::isDisplayed)
            .filter(property -> propertyName.equals(property.findElement(By.className("property-name")).getText()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                String.format("Card [%d] does not display the property [%s].", cardNumber, propertyLabel)))
            .findElement(By.className("value"))
            .getText();
    }

    private List<WebElement> getCards()
    {
        return getDriver().findElementsWithoutWaiting(getLayout(), By.cssSelector(".layout-table-root > .card"));
    }

    private WebElement getLayout()
    {
        return getDriver().findElementWithoutWaiting(getRoot(), By.className("layout-cards"));
    }

    private WebElement getRoot()
    {
        return getDriver().findElementWithoutWaiting(By.id(this.liveDataId));
    }
}
