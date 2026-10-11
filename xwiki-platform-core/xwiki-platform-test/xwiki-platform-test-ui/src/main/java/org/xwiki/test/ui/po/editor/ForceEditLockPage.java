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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the page shown when trying to edit a page that is locked, belongs to an extension, or has a warning
 * about required rights.
 *
 * @version $Id$
 * @since 17.10.9
 * @since 18.4.0RC1
 */
public class ForceEditLockPage extends BasePage
{
    @FindBy(css = "#mainContentArea .panel-title")
    private WebElement title;

    @FindBy(css = "#mainContentArea .panel-body")
    private WebElement body;

    @FindBy(css = "#mainContentArea .panel-body .btn-default")
    private WebElement cancelButton;

    @FindBy(css = "#mainContentArea .panel-body .btn-warning")
    private WebElement forceEditButton;

    /**
     * @return the title of the force edit lock page
     */
    public String getTitle()
    {
        return this.title.getText();
    }

    /**
     * @return the body message of the force edit lock page
     */
    public String getBody()
    {
        return this.body.getText();
    }

    /**
     * Clicks the Cancel button to cancel editing.
     *
     * @return the view page
     */
    public ViewPage clickCancel()
    {
        this.cancelButton.click();

        return new ViewPage();
    }

    /**
     * Clicks the "Force editing" button to force editing.
     *
     * @return the edit page
     */
    public EditPage clickForceEdit()
    {
        this.forceEditButton.click();

        return new EditPage();
    }
}
