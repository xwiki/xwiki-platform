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

import java.io.File;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the modal used to import an office file in CKEditor (Insert &gt; Import Office File).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Unstable
public class OfficeImporterDialog extends BaseElement
{
    private final WebElement container;

    /**
     * Wait for the modal to be displayed and for its form, which is loaded asynchronously, to be ready.
     */
    public OfficeImporterDialog()
    {
        this.container = getDriver().findElement(By.className("office-importer-modal"));
        getDriver().waitUntilElementIsVisible(this.container, By.name("filePath"));
    }

    /**
     * @param file the office file to import; its path must be valid on the machine running the browser
     * @return this dialog
     */
    public OfficeImporterDialog setFile(File file)
    {
        this.container.findElement(By.name("filePath")).sendKeys(file.getAbsolutePath());
        return this;
    }

    /**
     * Click the Import button. The modal is closed and the import is done asynchronously, so the caller has to wait
     * for the imported content to be inserted in the edited content.
     */
    public void clickImport()
    {
        this.container.findElement(By.cssSelector(".modal-footer .btn-primary")).click();
        getDriver().waitUntilCondition(driver -> !this.container.isDisplayed());
    }
}
