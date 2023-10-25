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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the realtime rich text area.
 * 
 * @version $Id$
 * @since 14.10.19
 * @since 15.5.4
 * @since 15.9
 */
public class RealtimeRichTextAreaElement extends RichTextAreaElement
{
    /**
     * Represents the position of a coeditor in the rich text area (displayed on the left side).
     */
    public static class CoeditorPosition extends BaseElement
    {
        private WebElement container;

        public CoeditorPosition(WebElement container)
        {
            this.container = container;
        }

        public String getCoeditorId()
        {
            return StringUtils.removeStart(this.container.getAttribute("id"), "rt-user-");
        }

        public String getAvatarURL()
        {
            return this.container.getAttribute("src");
        }

        public String getAvatarHint()
        {
            return this.container.getAttribute("title");
        }

        public Point getLocation()
        {
            return this.container.getLocation();
        }
    }

    /**
     * Creates a new realtime rich text area element.
     * 
     * @param iframe the in-line frame used by the realtime rich text area
     */
    public RealtimeRichTextAreaElement(WebElement iframe)
    {
        super(iframe);
    }

    /**
     * @return the position of the coeditors inside the rich text area
     */
    public List<CoeditorPosition> getCoeditorPositions()
    {
        try {
            getDriver().switchTo().frame(this.iframe);
            return getDriver().findElementsWithoutWaiting(By.className("rt-user-position")).stream()
                .map(CoeditorPosition::new).collect(Collectors.toList());
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * @param coeditorId the coeditor identifier
     * @return the location where the specified coeditor is currently editing
     */
    public CoeditorPosition getCoeditorPosition(String coeditorId)
    {
        return getCoeditorPositions().stream().filter(position -> position.getCoeditorId().equals(coeditorId))
            .findFirst().orElse(null);
    }
}
