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
package org.xwiki.ckeditor.test.po;

import java.util.List;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the CKEditor panel opened from the Emoji List tool bar button.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Unstable
public class EmojiPanel extends BaseElement
{
    private static final String GROUP_ATTRIBUTE = "data-cke-emoji-group";

    private static final By DISPLAYED_EMOJIS = By.cssSelector(".cke_emoji-outer_emoji_block li:not(.hidden) > a");

    private final WebElement frame;

    /**
     * Creates a new instance for the emoji panel displayed in the given frame.
     *
     * @param frame the frame holding the emoji panel
     */
    public EmojiPanel(WebElement frame)
    {
        this.frame = frame;
        // The emoji list is loaded asynchronously.
        executeInPanel(() -> getDriver().waitUntilElementIsVisible(By.cssSelector(".cke_emoji-outer_emoji_block a")));
    }

    /**
     * Click the navigation entry of the given emoji category, which scrolls the emoji list to that category.
     *
     * @param category the category identifier: people, nature, food, travel, activities, objects, symbols or flags
     * @return this panel
     */
    public EmojiPanel selectCategory(String category)
    {
        executeInPanel(() -> {
            getDriver().findElement(By.cssSelector(String.format("nav li[%s='%s'] a", GROUP_ATTRIBUTE, category)))
                .click();
            getDriver().waitUntilElementIsVisible(
                By.cssSelector(String.format("nav li.active[%s='%s']", GROUP_ATTRIBUTE, category)));
        });
        return this;
    }

    /**
     * @return the identifier of the category whose emojis are currently listed at the top of the panel
     */
    public String getActiveCategory()
    {
        return executeInPanel(() -> getDriver().findElementWithoutWaiting(By.cssSelector("nav li.active"))
            .getDomAttribute(GROUP_ATTRIBUTE));
    }

    /**
     * Type in the search input and wait for the emoji list to be filtered.
     *
     * @param text the text to search for, matched against the emoji names and keywords
     * @return this panel
     */
    public EmojiPanel search(String text)
    {
        executeInPanel(() -> {
            getDriver().findElement(By.cssSelector(".cke_emoji-search input")).sendKeys(text);
            // The emojis that don't match are hidden. The filter is throttled and applied while typing, so wait until
            // it is applied for the entire text.
            getDriver().waitUntilCondition(driver -> (Boolean) getDriver().executeScript("""
                const query = arguments[0];
                const emojis = [...document.querySelectorAll('.cke_emoji-outer_emoji_block li > a')];
                const displayedEmojis = emojis.filter(emoji => !emoji.classList.contains('hidden'));
                return displayedEmojis.length < emojis.length && displayedEmojis.every(emoji =>
                  emoji.dataset.ckeEmojiName.includes(query) || emoji.dataset.ckeEmojiKeywords.includes(query));
                """, text));
        });
        return this;
    }

    /**
     * @return the emojis that are currently listed (i.e. not filtered out)
     */
    public List<String> getEmojis()
    {
        return executeInPanel(() -> getDriver().findElementsWithoutWaiting(DISPLAYED_EMOJIS).stream()
            .map(emoji -> emoji.getDomAttribute("data-cke-emoji-symbol")).toList());
    }

    /**
     * Click the given emoji in order to insert it. The panel is closed afterwards.
     *
     * @param emoji the emoji to insert (e.g. "🚀")
     */
    public void selectEmoji(String emoji)
    {
        executeInPanel(() -> getDriver()
            .findElement(By.cssSelector(String.format("a[data-cke-emoji-symbol='%s']", emoji))).click());
        getDriver().waitUntilCondition(driver -> !this.frame.isDisplayed());
    }

    private void executeInPanel(Runnable action)
    {
        executeInPanel(() -> {
            action.run();
            return null;
        });
    }

    private <T> T executeInPanel(Supplier<T> action)
    {
        getDriver().switchTo().frame(this.frame);
        try {
            return action.get();
        } finally {
            getDriver().switchTo().parentFrame();
        }
    }
}
