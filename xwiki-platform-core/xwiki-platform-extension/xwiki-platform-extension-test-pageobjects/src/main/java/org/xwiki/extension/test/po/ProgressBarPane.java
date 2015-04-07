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
package org.xwiki.extension.test.po;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays a generic progress bar.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ProgressBarPane extends BaseElement
{
    /**
     * The pattern used to extract the progress percent. Note that the process percent is a positive integer.
     */
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)%");

    /**
     * The progress bar container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the progress bar container
     */
    public ProgressBarPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the progress percent
     */
    public int getPercent()
    {
        WebElement progressBar = getDriver().findElementWithoutWaiting(container, By.className("ui-progress-bar"));
        String style = progressBar.getAttribute("style");
        Matcher matcher = PERCENT_PATTERN.matcher(style);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /**
     * @return the progress message, displayed below the progress bar
     */
    public String getMessage()
    {
        return getDriver().findElementWithoutWaiting(container, By.className("ui-progress-message")).getText();
    }
}
