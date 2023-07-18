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
package org.xwiki.ckeditor.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page Object for the macro edition modal.
 *
 * @version $Id$
 * @since 14.9
 */
public class MacroDialogEditModal extends BaseElement
{
    /**
     * Wait until the macro selection edition is loaded.
     *
     * @return the current page object
     */
    public MacroDialogEditModal waitUntilReady()
    {
        getDriver().waitUntilElementIsVisible(
                // We match *-editor-modal so the page object can be used in dashboard and ckeditor tests.
                By.cssSelector("[class*=-editor-modal] .macro-name"));
        return this;
    }

    /**
     * Click on the macro submission button.
     */
    public void clickSubmit()
    {
        getDriver().findElement(
             // We match *-editor-modal so the page object can be used in dashboard and ckeditor tests.
                By.cssSelector("[class*=-editor-modal] .modal-footer .btn-primary")).click();
    }
}
