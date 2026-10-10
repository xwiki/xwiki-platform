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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.ExtensionId;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The Extension Updater administration section (Administer Wiki &gt; Extensions &gt; Updater).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ExtensionUpdaterPage extends ViewPage
{
    private static final By CHECK_FOR_UPDATES =
        By.xpath("//form[following-sibling::div[1][@class = 'extensionUpdater']]"
            + "//button[@name = 'action' and @value = 'checkForUpdates']");

    /**
     * Opens the Extension Updater administration section.
     *
     * @return the Extension Updater administration section
     */
    public static ExtensionUpdaterPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=XWiki.ExtensionUpdater");
        return new ExtensionUpdaterPage();
    }

    /**
     * Clicks the "Check for updates" button (for the current wiki) and waits for the upgrade plan to be computed.
     *
     * @param timeout the maximum number of seconds to wait for the upgrade plan
     * @return the Extension Updater administration section, listing the outdated and invalid extensions
     */
    public ExtensionUpdaterPage checkForUpdates(int timeout)
    {
        WebElement button = getDriver().findElement(CHECK_FOR_UPDATES);
        // The buttons are disabled synchronously on click, then the updater is refreshed (replaced) until the upgrade
        // plan job is finished, i.e. until the progress bar is gone, and only then the buttons are enabled again.
        button.click();
        getDriver().waitUntilCondition(driver -> button.isEnabled()
            && getDriver().findElementsWithoutWaiting(By.cssSelector(".extensionUpdater > .ui-progress")).isEmpty(),
            timeout);
        return new ExtensionUpdaterPage();
    }

    /**
     * @param extensionId the extension to look for, with the version it can be upgraded to
     * @return the outdated extension, as listed by the updater, or {@code null} if the updater doesn't list it
     */
    public ExtensionPane getOutdatedExtension(ExtensionId extensionId)
    {
        String xpath = String.format("//div[@class = 'outdatedExtensions']/form[contains(@class, 'extension-item') "
            + "and .//*[@class = 'extension-header'][.//*[@class = 'extension-name'][normalize-space() = '%s'] and "
            + ".//*[@class = 'extension-version'][normalize-space() = '%s']]]", extensionId.getId(),
            extensionId.getVersion().getValue());
        List<WebElement> found = getDriver().findElementsWithoutWaiting(By.xpath(xpath));
        return found.size() == 1 ? new ExtensionPane(found.get(0)) : null;
    }
}
