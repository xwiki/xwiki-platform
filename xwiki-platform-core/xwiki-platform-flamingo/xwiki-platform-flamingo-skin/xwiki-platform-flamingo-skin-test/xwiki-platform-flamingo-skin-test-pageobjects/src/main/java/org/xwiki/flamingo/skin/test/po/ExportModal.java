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

import java.net.URI;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseModal;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.XWikiSelectWidget;

/**
 * Represents the Export modal.
 *
 * @version $Id$
 * @since 10.9
 */
public class ExportModal extends BaseModal
{
    private XWikiSelectWidget exportFormatSelect;

    /**
     * Default constructor.
     */
    public ExportModal()
    {
        super(By.id("exportModal"));
        this.exportFormatSelect =
            new XWikiSelectWidget(this.container.findElement(By.className("xwiki-export-formats")), "exportFormat");
    }

    /**
     * Opens the export modal for the given page.
     * 
     * @param viewPage the page for which to open the export modal
     * @return the export modal
     * @since 14.10
     */
    public static ExportModal open(ViewPage viewPage)
    {
        // The export modal is present but hidden on page load. We instantiate the page object before opening the modal
        // in order to prevent the fade effect (see BaseModal).
        ExportModal exportModal = new ExportModal();

        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        return exportModal;
    }

    /**
     * @return the widget used to select the export format
     * @since 14.10
     */
    public XWikiSelectWidget getExportFormatSelect()
    {
        return this.exportFormatSelect;
    }

    /**
     * Select the specified export format.
     * 
     * @param format the export format to select
     * @since 14.10
     */
    public void exportAs(String format)
    {
        getExportFormatSelect().selectByLabel(format);
    }

    /**
     * Selecting a single page export format (e.g. ODT) makes the browser download the export, which tests can't read.
     * This method returns the URL that the browser would be sent to, so that tests can fetch the export themselves.
     *
     * @param format the label of the export format (e.g. "ODT"), which must be listed (i.e. enabled)
     * @return the absolute URL of the export of the current page in the given format
     * @since 18.9.0RC1
     */
    public String getExportURL(String format)
    {
        String exportURL = this.container
            .findElement(By.xpath(".//li[contains(@class, 'xwiki-select-option')][.//label[. = '" + format
                + "']]//input[@name = 'exportFormat']"))
            .getAttribute("data-url");
        return URI.create(getDriver().getCurrentUrl()).resolve(exportURL).toString();
    }
}
