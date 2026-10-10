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
import org.xwiki.test.ui.po.ViewPage;

/**
 * The Extension History administration section (Administer Wiki &gt; Extensions &gt; History).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class ExtensionHistoryPage extends ViewPage
{
    /**
     * Matches the history records of the local history, most recent first. The "more" link and the "no records"
     * message are also list items with the {@code extension-history-record} class, but without the job type class.
     */
    private static final By RECORDS = By.xpath("//form[contains(@class, 'extension-history-records-form')]"
        + "/ul[@class = 'extension-history-records']/li[@class != 'extension-history-record']");

    /**
     * Opens the Extension History administration section.
     *
     * @return the Extension History administration section
     */
    public static ExtensionHistoryPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin", "section=XWiki.ExtensionHistory");
        return new ExtensionHistoryPage();
    }

    /**
     * @return the history records displayed in the page, the most recent first
     */
    public List<ExtensionHistoryRecordPane> getRecords()
    {
        return getDriver().findElementsWithoutWaiting(RECORDS).stream().map(ExtensionHistoryRecordPane::new)
            .toList();
    }
}
