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
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the grid of emoji suggestions that appears when typing a colon in the BlockNote editor.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Unstable
public class EmojiPicker extends BaseElement
{
    private static final By SELECTED_EMOJI = By.cssSelector(".bn-grid-suggestion-menu-item[aria-selected=\"true\"]");

    /**
     * Waits for the emoji picker to show its suggestions. The emoji data is loaded lazily, so the picker is first
     * displayed empty, with only a loading indicator.
     */
    public EmojiPicker()
    {
        getDriver().waitUntilElementIsVisible(SELECTED_EMOJI);
    }

    /**
     * @return the emoji that gets inserted when submitting the current suggestion
     */
    public String getSelectedEmoji()
    {
        return getDriver().findElement(SELECTED_EMOJI).getText();
    }
}
