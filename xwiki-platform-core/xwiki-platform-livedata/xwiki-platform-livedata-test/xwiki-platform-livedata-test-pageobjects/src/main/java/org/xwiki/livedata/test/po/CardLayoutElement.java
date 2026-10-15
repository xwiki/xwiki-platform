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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
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
    private static final By CARDS = By.cssSelector(".layout-cards .card");

    private static final By COVER = By.className("card-cover");

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
     * Waits until the card layout is displayed and its entries are loaded.
     *
     * @since 18.10.0RC1
     */
    public void waitUntilReady()
    {
        getDriver().waitUntilCondition(webDriver -> {
            try {
                WebElement layout = getLayout();
                List<String> layoutLoaderClasses =
                    Arrays.asList(layout.findElement(By.className("layout-loader")).getDomAttribute("class")
                        .split("\\s+"));
                return !layoutLoaderClasses.contains("waiting") && !layoutLoaderClasses.contains("loading")
                    && layout.findElements(By.cssSelector(".card .xwiki-loader")).isEmpty();
            } catch (StaleElementReferenceException e) {
                // Vue is (re)mounting the layout, it is not ready yet.
                return false;
            }
        }, 20);
    }

    /**
     * Waits until the number of displayed cards matches the expected count.
     *
     * @param expectedCardCount the number of expected cards
     * @since 18.10.0RC1
     */
    public void waitUntilCardCountEqualsTo(int expectedCardCount)
    {
        getDriver().waitUntilCondition(webDriver -> {
            try {
                return countCards() == expectedCardCount;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /**
     * @return the number of displayed cards
     * @since 18.10.0RC1
     */
    public int countCards()
    {
        return getCards().size();
    }

    /**
     * @param cardNumber the number of the card, starting at 1
     * @return {@code true} if the card displays a cover, {@code false} otherwise
     * @since 18.10.0RC1
     */
    public boolean hasCover(int cardNumber)
    {
        return !getDriver().findElementsWithoutWaiting(getCard(cardNumber), COVER).isEmpty();
    }

    /**
     * Waits until all the displayed cards have a cover, or until none of them have one.
     *
     * @param withCover {@code true} to wait for a cover on all the cards, {@code false} to wait for no cover at all
     * @since 18.10.0RC1
     */
    public void waitUntilCovers(boolean withCover)
    {
        getDriver().waitUntilCondition(webDriver -> {
            try {
                List<WebElement> cards = getCards();
                return !cards.isEmpty() && cards.stream()
                    .allMatch(card -> getDriver().findElementsWithoutWaiting(card, COVER).isEmpty() != withCover);
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /**
     * @param cardNumber the number of the card, starting at 1
     * @return the source of the image displayed by the cover of the card, as set in the page, or {@code null} when
     *     the cover displays some text instead
     * @since 18.10.0RC1
     */
    public String getCoverImageSource(int cardNumber)
    {
        List<WebElement> images = getDriver().findElementsWithoutWaiting(getCover(cardNumber), By.tagName("img"));
        return images.isEmpty() ? null : images.get(0).getDomAttribute("src");
    }

    /**
     * Waits until the cover of the card displays some text, which happens when the cover property doesn't hold an
     * image, or when its image fails to load.
     *
     * @param cardNumber the number of the card, starting at 1
     * @return the text displayed by the cover
     * @since 18.10.0RC1
     */
    public String waitUntilCoverText(int cardNumber)
    {
        By coverText = By.className("card-cover-text-value");
        getDriver().waitUntilCondition(webDriver -> {
            try {
                return !getDriver().findElementsWithoutWaiting(getCover(cardNumber), coverText).isEmpty();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
        return getCover(cardNumber).findElement(coverText).getText();
    }

    /**
     * @param cardNumber the number of the card, starting at 1
     * @return the names of the properties displayed in the body of the card, in the order they are displayed
     * @since 18.10.0RC1
     */
    public List<String> getPropertyNames(int cardNumber)
    {
        return getDriver().findElementsWithoutWaiting(getCard(cardNumber),
                By.cssSelector(".card-property .property-name")).stream()
            .filter(WebElement::isDisplayed)
            // The names are followed by a colon.
            .map(name -> name.getText().replaceFirst(":$", ""))
            .toList();
    }

    private WebElement getCover(int cardNumber)
    {
        return getCard(cardNumber).findElement(COVER);
    }

    private WebElement getCard(int cardNumber)
    {
        return getCards().get(cardNumber - 1);
    }

    private List<WebElement> getCards()
    {
        return getDriver().findElementsWithoutWaiting(getLayout(), CARDS);
    }

    private WebElement getLayout()
    {
        return getDriver().findElement(By.id(this.liveDataId)).findElement(By.className("layout-cards"));
    }
}
