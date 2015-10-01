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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;


/**
 * Resubmission page is used to ask user for confirmation when the CSRF protection detects a request with invalid secret
 * token. The user can {@link #resubmit()} the request with correct token or {@link #cancel()} and return to view mode.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class ResubmissionPage extends BasePage
{
    @FindBy(xpath = "//form//input[@type='submit']")
    private WebElement resubmitButton;

    @FindBy(xpath = "//form//a[@class='secondary button']")
    private WebElement cancelButton;

    /**
     * @return true if we are viewing the resubmission page, false otherwise
     */
    public boolean isOnResubmissionPage()
    {
        return getPageURL().contains("xpage=resubmit");
    }

    /**
     * Resubmit the request
     */
    public void resubmit()
    {
        this.resubmitButton.click();
    }

    /**
     * Cancel the request. Returns to view mode on some page (do not rely to which exactly, the heuristic might have
     * somewhat surprising results when the request was something other than a simple page save).
     * <p>
     * It is safe to ignore the resubmission warning and directly navigate to some other page instead of canceling.
     */
    public void cancel()
    {
        this.cancelButton.click();
    }
}
