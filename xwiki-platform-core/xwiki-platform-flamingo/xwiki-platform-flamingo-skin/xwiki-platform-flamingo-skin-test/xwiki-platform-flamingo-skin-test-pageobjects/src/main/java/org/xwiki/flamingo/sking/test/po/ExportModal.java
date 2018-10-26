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
package org.xwiki.flamingo.sking.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents the Export modal.
 *
 * @version $Id$
 * @since 10.9
 */
public class ExportModal extends BaseModal
{
    private static final String EXPORT_MODAL_ID = "exportModal";
    private static final String OTHER_FORMAT_PANE_LINK = "Other Formats";

    /**.
     * Simple constructor.
     */
    public ExportModal()
    {
        super(By.id(EXPORT_MODAL_ID));
    }

    /**
     * Opens the pane "Other Format".
     * @return the pane for "Other Format" export.
     */
    public OtherFormatPane openOtherFormatPane()
    {
        getDriver().waitUntilElementIsVisible(By.id(EXPORT_MODAL_ID));
        getDriver().findElementByLinkText(OTHER_FORMAT_PANE_LINK).click();

        getDriver().waitUntilElementIsVisible(By.cssSelector("#exportModelOtherCollapse.collapse.in"));
        return new OtherFormatPane();
    }

    /**
     * @return the container of this modal.
     */
    public WebElement getContainer()
    {
        return container;
    }
}
