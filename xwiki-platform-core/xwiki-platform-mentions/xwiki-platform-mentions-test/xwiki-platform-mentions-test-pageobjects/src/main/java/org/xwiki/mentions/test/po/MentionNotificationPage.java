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
package org.xwiki.mentions.test.po;

import java.util.List;

import org.openqa.selenium.WebElement;
import org.xwiki.platform.notifications.test.po.GroupedNotificationElementPage;

import static org.openqa.selenium.By.cssSelector;
import static org.openqa.selenium.By.xpath;

/**
 * Mentions notifications page object.
 *
 * @version $Id$
 * @since 12.8RC1
 */
public class MentionNotificationPage extends GroupedNotificationElementPage
{
    private static final String SUMMARY_SELECTOR = "../following-sibling::tr/td/blockquote";

    /**
     * Default constructor.
     * @param rootElement The root element of the notification blocks.
     */
    public MentionNotificationPage(WebElement rootElement)
    {
        super(rootElement);
    }

    /**
     *
     *
     * @param notificationNumber The notification group number.
     * @param groupIndex The index of the notification in the group.
     * @return {@code} true if the request element has a summary.
     */
    public boolean hasSummary(int notificationNumber, int groupIndex)
    {
        return !findSummaryElement(notificationNumber, groupIndex).isEmpty();
    }

    /**
     *
     * Returns the nth notification summary.
     *
     * @param notificationNumber The notification group number.
     * @param groupIndex The index of the notification in the group.
     * @return the summary of the mention.
     */
    public String getSummary(int notificationNumber, int groupIndex)
    {
        return findSummaryElement(notificationNumber, groupIndex).get(0).getText();
    }

    private List<WebElement> findSummaryElement(int notificationNumber, int groupIndex)
    {
        WebElement notifications =
            findNotifications().get(notificationNumber);
        WebElement element = notifications.findElements(cssSelector(TEXT_SELECTOR)).get(groupIndex);
        return element.findElements(xpath(SUMMARY_SELECTOR));
    }
}
