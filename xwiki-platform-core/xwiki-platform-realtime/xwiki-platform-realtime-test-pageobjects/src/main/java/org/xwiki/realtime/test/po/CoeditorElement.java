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
package org.xwiki.realtime.test.po;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Used to display an user that is participating to the realtime editing session.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public class CoeditorElement extends BaseElement
{
    private final Supplier<WebElement> containerSupplier;

    /**
     * Creates a new instance based on the given coeditor element supplier.
     * <p>
     * The coeditor element is looked up each time it is needed, rather than being kept, because the list of coeditors
     * is updated while the realtime editing session is going on, which can invalidate the element in between two
     * calls.
     *
     * @param containerSupplier provides the WebElement used to display the coeditor
     * @since 18.7.0RC1
     * @since 18.4.4
     * @since 17.10.12
     * @since 16.10.19
     */
    public CoeditorElement(Supplier<WebElement> containerSupplier)
    {
        this.containerSupplier = containerSupplier;
    }

    /**
     * @return {@code true} if the coeditor is displayed, {@code false} otherwise (e.g. if the coeditor is listed in the
     *         dropdown and the dropdown is closed)
     */
    public boolean isDisplayed()
    {
        return readContainer(WebElement::isDisplayed);
    }

    /**
     * @return the coeditor identifier
     */
    public String getId()
    {
        return readContainer(CoeditorElement::getId);
    }

    /**
     * @return the coeditor's displayed name (is empty if the coeditor is not displayed)
     */
    public String getName()
    {
        return readContainer(container -> container.findElement(By.className("realtime-user-name")).getText());
    }

    /**
     * @return the XWiki user reference
     */
    public String getReference()
    {
        return readContainer(container -> container.getDomAttribute("data-reference"));
    }

    /**
     * @return the user profile URL
     */
    public String getURL()
    {
        return readContainer(container -> container.getDomAttribute("href"));
    }

    /**
     * @return the user avatar URL
     */
    public String getAvatarURL()
    {
        return readContainer(container -> getAvatar(container).getDomAttribute("src"));
    }

    /**
     * @return the user avatar hint, usually the user full name
     */
    public String getAvatarHint()
    {
        return readContainer(CoeditorElement::getAvatarHint);
    }

    /**
     * @return the user name abbreviation
     */
    public String getAbbreviation()
    {
        return readContainer(
            container -> container.findElement(By.className("realtime-user-avatar-wrapper")).getDomAttribute(
                "data-abbr"));
    }

    /**
     * Click on the coeditor avatar to see where the user is editing.
     */
    public void click()
    {
        readContainer(container -> {
            container.click();
            return null;
        });
    }

    /**
     * @param coeditor the element used to display the coeditor
     * @return the identifier of the given coeditor
     */
    static String getId(WebElement coeditor)
    {
        return coeditor.getDomAttribute("data-id");
    }

    /**
     * @param coeditor the element used to display the coeditor
     * @return the hint displayed when hovering the avatar of the given coeditor
     */
    static String getAvatarHint(WebElement coeditor)
    {
        return getAvatar(coeditor).getDomAttribute("title");
    }

    /**
     * Reads some information from each coeditor matching the given selector, retrying the whole list while the
     * coeditor list is updated in the middle of it.
     * <p>
     * All the values are read from the same lookup, so that the result is a consistent snapshot of the coeditor list
     * rather than a mix of several updates. This is why the values can't be read through {@link CoeditorElement},
     * which looks up and retries each access on its own and would thus hide the updates from us.
     *
     * @param <T> the type of information to read
     * @param driver the driver used to look up the coeditors
     * @param coeditorsSelector selects the coeditors to read
     * @param reader reads the information from a coeditor element
     * @return the information read from each coeditor, in the order they are displayed
     */
    static <T> List<T> readCoeditors(XWikiWebDriver driver, By coeditorsSelector, Function<WebElement, T> reader)
    {
        return driver.waitUntilCondition(condition -> {
            try {
                return driver.findElements(coeditorsSelector).stream().map(reader).toList();
            } catch (StaleElementReferenceException e) {
                // The coeditor list was updated while we were reading it, retry.
                return null;
            }
        });
    }

    private static WebElement getAvatar(WebElement container)
    {
        return container.findElement(By.className("realtime-user-avatar"));
    }

    /**
     * Reads some information from the coeditor element, retrying while the element can't be resolved.
     * <p>
     * The coeditor list is updated whenever someone joins or leaves the realtime editing session, which replaces the
     * elements we're reading. Since the information we read doesn't depend on when it is read, retrying with a freshly
     * looked up element gives the same result.
     *
     * @param <T> the type of information to read
     * @param reader reads the information from the coeditor element
     * @return the information read from the coeditor element
     */
    private <T> T readContainer(Function<WebElement, T> reader)
    {
        // The result is wrapped in a list because it can be null (e.g. a missing attribute) and the wait would
        // understand a null value as "the condition is not met yet".
        return getDriver().waitUntilCondition(driver -> {
            try {
                return Collections.singletonList(reader.apply(this.containerSupplier.get()));
            } catch (StaleElementReferenceException | IndexOutOfBoundsException e) {
                // The coeditor list was updated while we were reading it: either the element we had was replaced
                // (stale) or it is not at the expected position anymore (out of bounds). Retry.
                return null;
            }
        }).get(0);
    }
}
