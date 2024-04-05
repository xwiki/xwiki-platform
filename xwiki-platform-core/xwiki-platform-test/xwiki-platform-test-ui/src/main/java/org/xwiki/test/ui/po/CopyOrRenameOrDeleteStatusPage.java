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
 * Represents the page that displays the status of a copy operation. This page is normally loaded after the user clicks
 * on the Copy button (i.e. after the copy parameters have been submitted).
 * 
 * @version $Id$
 * @since 7.4.1
 * @since 8.0M1
 */
public class CopyOrRenameOrDeleteStatusPage extends RefactoringStatusPage
{
    @FindBy(css = ".job-status .col-lg-6:first-child .breadcrumb > li:last-child a")
    private WebElement oldPage;

    @FindBy(css = ".job-status .col-lg-6:last-child .breadcrumb > li:last-child a")
    private WebElement newPage;

    public ViewPage gotoOriginalPage()
    {
        this.oldPage.click();
        return new ViewPage();
    }

    public ViewPage gotoNewPage()
    {
        this.newPage.click();
        return new ViewPage();
    }

    @Override
    public CopyOrRenameOrDeleteStatusPage waitUntilFinished()
    {
        super.waitUntilFinished();
        return this;
    }
}
