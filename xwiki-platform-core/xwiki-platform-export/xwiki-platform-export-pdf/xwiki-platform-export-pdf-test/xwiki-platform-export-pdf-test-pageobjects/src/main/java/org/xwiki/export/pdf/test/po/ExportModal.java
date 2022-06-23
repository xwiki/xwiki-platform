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
package org.xwiki.export.pdf.test.po;

import org.xwiki.flamingo.skin.test.po.OtherFormatPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The PDF Export Application is currently customizing the Export Modal, thus we're extending the standard
 * {@link org.xwiki.flamingo.skin.test.po.ExportModal}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class ExportModal extends org.xwiki.flamingo.skin.test.po.ExportModal
{
    /**
     * Opens the export modal for the given page.
     * 
     * @param viewPage the page for which to open the export modal
     * @return the export modal
     */
    public static ExportModal open(ViewPage viewPage)
    {
        // The export modal is present but hidden on page load. We instantiate the page object before opening the modal
        // in order to prevent the fade effect (see BaseModal).
        ExportModal exportModal = new ExportModal();

        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        // The Export Modal was modified to not show the accordion, so the "Other formats" pane is visible by default.
        // Wait for the tree to be ready, if present.
        OtherFormatPane otherFormatPane = new OtherFormatPane();
        if (otherFormatPane.isTreeAvailable()) {
            otherFormatPane.getTreeElement();
        }

        return exportModal;
    }

    /**
     * Clicks on the "Export as PDF" button and return the "PDF Export Options" modal.
     * 
     * @return the "PDF Export Options" modal
     */
    public PDFExportOptionsModal clickExportAsPDFButton()
    {
        new OtherFormatPane().clickExportButton("Export as PDF");
        return new PDFExportOptionsModal();
    }
}
