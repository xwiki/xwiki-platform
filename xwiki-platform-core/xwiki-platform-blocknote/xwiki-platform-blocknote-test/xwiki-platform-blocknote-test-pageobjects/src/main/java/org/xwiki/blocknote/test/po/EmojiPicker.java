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
package org.xwiki.blocknote.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the grid of emoji suggestions that appears when typing a colon in the BlockNote editor.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public class EmojiPicker extends BaseElement
{
    // The picker filters its grid asynchronously and ignores Enter until the selected emoji is rendered at the first
    // position of the first row, so we wait for exactly that to avoid submitting against a stale grid.
    private static final By SELECTED_EMOJI =
        By.cssSelector("[aria-rowindex=\"0\"] .bn-frimousse-emoji[aria-colindex=\"0\"][aria-selected=\"true\"]");

    private static final By NO_RESULTS = By.className("bn-frimousse-empty");

    /**
     * Waits for the emoji picker to show either its first suggestion, selected, or the "no results" message. The emoji
     * data is loaded lazily, so the picker is first displayed empty, with only a loading indicator.
     */
    public EmojiPicker()
    {
        getDriver().waitUntilElementsAreVisible(new By[] {SELECTED_EMOJI, NO_RESULTS}, false);
    }

    /**
     * @return the emoji that gets inserted when submitting the current suggestion
     */
    public String getSelectedEmoji()
    {
        return getDriver().findElement(SELECTED_EMOJI).getText();
    }

    /**
     * @return the message displayed when no emoji matches the typed text
     */
    public String getNoResultsMessage()
    {
        return getDriver().findElement(NO_RESULTS).getText();
    }
}
