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

package org.xwiki.platform.notifications.test.po;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents the modal allowing to change the watch settings.
 *
 * @version $Id$
 * @since 16.5.0RC1
 */
public class NotificationsWatchModal extends BaseModal
{
    /**
     * Default constructor.
     */
    public NotificationsWatchModal()
    {
        super(By.id("watchModal"));
    }

    /**
     * The various options a user can select in the modal.
     */
    public enum WatchOptions
    {
        /**
         * For watching a page.
         */
        WATCH_PAGE("watchPage"),
        /**
         * For watching a space.
         */
        WATCH_SPACE("watchSpace"),
        /**
         * For watching a wiki.
         */
        WATCH_WIKI("watchWiki"),
        /**
         * For blocking a page.
         */
        BLOCK_PAGE("blockPage"),
        /**
         * For blocking a space.
         */
        BLOCK_SPACE("blockSpace"),
        /**
         * For removing the watch filter on the page.
         */
        UNWATCH_PAGE("unwatchPage"),

        /**
         * For removing the watch filter on a page and instead watch the entire space
         */
        UNWATCH_PAGE_WATCH_SPACE("unwatchPageWatchSpace"),
        /**
         * For removing the watch filter on the space.
         */
        UNWATCH_SPACE("unwatchSpace"),
        /**
         * For removing the watch filter on the wiki.
         */
        UNWATCH_WIKI("unwatchWiki"),
        /**
         * For removing the block filter on the page.
         */
        UNBLOCK_PAGE("unblockPage"),

        /**
         * For removing the block filter on a page and instead block the entire space
         */
        UNBLOCK_PAGE_BLOCK_SPACE("unblockPageBlockSpace"),
        /**
         * For removing the block filter on the space.
         */
        UNBLOCK_SPACE("unblockSpace"),
        /**
         * For removing the block filter on the wiki.
         */
        UNBLOCK_WIKI("unblockWiki");

        private final String optionValue;

        WatchOptions(String optionValue)
        {
            this.optionValue = optionValue;
        }

        String getOptionValue()
        {
            return optionValue;
        }
    }

    /**
     * @return the available options in the modal.
     */
    public List<WatchOptions> getAvailableOptions()
    {
        return this.container.findElements(By.name("watch-option")).stream().map(element ->
            Arrays.stream(WatchOptions.values())
            .filter(option -> element.getAttribute("value").equals(option.getOptionValue()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Option " + element.getAttribute("value") + " not found"))
        ).toList();
    }

    /**
     * Select an option, save it and wait for the page to be reloaded.
     * @param option the option to be saved.
     */
    public void selectOptionAndSave(WatchOptions option)
    {
        getDriver().addPageNotYetReloadedMarker();
        this.container.findElement(By.cssSelector("input[type='radio'][value='" + option.getOptionValue() + "']"))
            .click();
        this.container.findElement(By.className("btn-primary")).click();
        getDriver().waitUntilPageIsReloaded();
    }
}
