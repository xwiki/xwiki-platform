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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the attachment history page.
 *
 * @version $Id$
 * @since 14.2RC1
 */
public class AttachmentHistoryPage extends ViewPage
{
    private static final int VERSION_COL_IDX = 2;

    private static final int SIZE_COL_IDX = 3;

    private static final int AUTHOR_COL_IDX = 4;

    private static final int SUMMARY_COL_IDX = 6;

    /**
     * @param rowIdx the index of the row of the attachment, starting at 1
     * @return the version for the requested attachment version
     */
    public String getVersion(long rowIdx)
    {
        return getCell(rowIdx, VERSION_COL_IDX).getText();
    }

    /**
     * Follow a version url, get its content the comes back to the previous page.
     *
     * @param rowIdx the index of the row of the attachment, starting at 1
     * @return the link to the attachment version content page
     */
    public String geAttachmentContent(long rowIdx)
    {
        String url = getDriver().findElementWithoutWaiting(getCell(rowIdx, VERSION_COL_IDX), By.cssSelector("a"))
            .getAttribute("href");
        getDriver().get(url);
        String content = getDriver().findElementWithoutWaiting(By.xpath("/*")).getText();
        getDriver().navigate().back();
        return content;
    }

    /**
     * @param rowIdx the index of the row of  the attachment, starting at 1
     * @return the size of the requested attachment version
     */
    public long getSize(int rowIdx)
    {
        return Long.parseLong(getCell(rowIdx, SIZE_COL_IDX).getText());
    }

    /**
     * @param rowIdx the index of the row of the attachment, starting at 1
     * @return the pretty name of the author of the requested attachment version
     */
    public String getAuthor(int rowIdx)
    {
        return getCell(rowIdx, AUTHOR_COL_IDX).getText();
    }

    /**
     * @param rowIdx the index of the row of the attachment, starting at 1
     * @return the summary of the requested attachment version
     */
    public String getSummary(int rowIdx)
    {
        return getCell(rowIdx, SUMMARY_COL_IDX).getText();
    }

    private WebElement getCell(long rowIdx, int columnIdx)
    {
        // Add one to skip the header.
        long actualRowIdx = rowIdx + 1;
        By selector = By.cssSelector(String.format("tr:nth-child(%d) td:nth-child(%d)", actualRowIdx, columnIdx));
        return getDriver().findElementWithoutWaiting(selector);
    }
}
