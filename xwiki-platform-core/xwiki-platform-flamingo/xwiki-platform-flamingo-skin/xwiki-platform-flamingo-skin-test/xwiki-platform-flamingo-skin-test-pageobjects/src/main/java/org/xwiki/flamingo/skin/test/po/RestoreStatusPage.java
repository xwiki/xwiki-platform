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
package org.xwiki.flamingo.skin.test.po;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.RefactoringStatusPage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the status page of the restore refactoring action.
 *
 * @version $Id$
 * @since 15.8RC1
 */
public class RestoreStatusPage extends RefactoringStatusPage
{
    @FindBy(xpath = "//div[@id='document-title']/h1/a")
    private WebElement viewLink;

    /**
     * @return The restored page.
     */
    public ViewPage gotoRestoredPage()
    {
        this.viewLink.click();
        return new ViewPage();
    }

    @Override
    public RestoreStatusPage waitUntilFinished()
    {
        super.waitUntilFinished();
        return this;
    }
}
