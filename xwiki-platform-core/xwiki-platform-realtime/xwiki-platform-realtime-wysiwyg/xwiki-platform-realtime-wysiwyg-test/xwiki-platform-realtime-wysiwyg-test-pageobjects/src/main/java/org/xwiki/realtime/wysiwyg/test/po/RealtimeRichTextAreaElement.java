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
package org.xwiki.realtime.wysiwyg.test.po;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the realtime rich text area.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeRichTextAreaElement extends RichTextAreaElement
{
    /**
     * Represents the position of a coeditor in the rich text area (displayed on the left side).
     */
    public class CoeditorPosition extends BaseElement
    {
        private String id;

        public CoeditorPosition(String id)
        {
            this(id, false);
        }

        public CoeditorPosition(String id, boolean wait)
        {
            this.id = id;

            // Wait for the specified coeditor position to be available in the DOM.
            if (wait) {
                getDriver().waitUntilCondition(driver -> getFromEditedContent(this::getContainer));
            }
        }

        public String getCoeditorId()
        {
            return StringUtils.removeStart(id, "rt-user-");
        }

        public String getAvatarURL()
        {
            return getFromEditedContent(() -> getContainer().getAttribute("src"));
        }

        public String getAvatarHint()
        {
            return getFromEditedContent(() -> getContainer().getAttribute("title"));
        }

        public Point getLocation()
        {
            return getFromEditedContent(() -> getContainer().getLocation());
        }

        public CoeditorPosition waitForLocation(Point point)
        {
            getDriver().waitUntilCondition(driver -> getFromEditedContent(() -> {
                try {
                    return getContainer().getLocation().equals(point);
                } catch (StaleElementReferenceException e) {
                    // The coeditor position (caret indicator) can be updated while we're waiting for it.
                    return false;
                }
            }));
            return this;
        }

        /**
         * @return {@code true} if this caret indicator is visible in the rich text area
         */
        @SuppressWarnings("unchecked")
        public boolean isVisible()
        {
            return getFromEditedContent(() -> {
                WebElement root = getDriver().findElementWithoutWaitingWithoutScrolling(By.tagName("html"));
                int viewportHeight = Integer.parseInt(root.getDomProperty("clientHeight"));
                int viewportWidth = Integer.parseInt(root.getDomProperty("clientWidth"));
                Map<String, Long> position = (Map<String, Long>) getDriver()
                    .executeScript("return arguments[0].getBoundingClientRect()", getContainer());
                return position.get("y") >= 0 && position.get("x") >= 0
                    && (position.get("y") + position.get("height")) <= viewportHeight
                    && (position.get("x") + position.get("width")) <= viewportWidth;
            });
        }

        private WebElement getContainer()
        {
            // Make sure we don't scroll the page when locating the coeditor position.
            return getDriver().findElementWithoutWaitingWithoutScrolling(By.id(this.id));
        }
    }

    /**
     * Creates a new realtime rich text area element.
     * 
     * @param editor the CKEditor instance that owns this rich text area
     * @param wait whether to wait or not for the content to be editable
     */
    public RealtimeRichTextAreaElement(CKEditor editor, boolean wait)
    {
        super(editor, wait);
    }

    /**
     * @return the position of the coeditors inside the rich text area
     */
    public List<CoeditorPosition> getCoeditorPositions()
    {
        return getFromEditedContent(() -> getDriver().findElementsWithoutWaiting(By.className("rt-user-position")).stream()
            .map(element -> element.getAttribute("id")).map(CoeditorPosition::new).collect(Collectors.toList()));
    }

    /**
     * @param coeditorId the coeditor identifier
     * @return the location where the specified coeditor is currently editing
     */
    public CoeditorPosition getCoeditorPosition(String coeditorId)
    {
        return new CoeditorPosition("rt-user-" + coeditorId, true);
    }
}
