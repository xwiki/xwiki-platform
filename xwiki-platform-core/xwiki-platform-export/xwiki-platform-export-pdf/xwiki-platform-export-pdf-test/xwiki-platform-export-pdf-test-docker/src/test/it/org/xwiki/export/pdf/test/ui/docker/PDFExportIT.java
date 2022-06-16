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
package org.xwiki.export.pdf.test.ui.docker;

import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.export.pdf.test.po.PDFDocument;
import org.xwiki.export.pdf.test.po.PDFExportOptionsModal;
import org.xwiki.flamingo.skin.test.po.ExportModal;
import org.xwiki.flamingo.skin.test.po.OtherFormatPane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for PDF export.
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5RC1
 */
@UITest
class PDFExportIT
{
    @Test
    void exportAsPDF(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.createUserAndLogin("John", "pass");

        PDFExportOptionsModal exportOptions =
            exportDocumentAsPDF(setup, new LocalDocumentReference(Arrays.asList("PDFExportIT", "Parent"), "WebHome"));

        try (PDFDocument pdf = exportOptions.export(getHostURL(testConfiguration))) {
            assertEquals(3, pdf.getNumberOfPages());
        }
    }

    private URL getHostURL(TestConfiguration testConfiguration) throws Exception
    {
        return new URL(String.format("http://%s:%d", testConfiguration.getServletEngine().getHostIP(),
            testConfiguration.getServletEngine().getPort()));
    }

    private PDFExportOptionsModal exportDocumentAsPDF(TestUtils setup, LocalDocumentReference documentReference)
    {
        ViewPage viewPage = setup.gotoPage(documentReference);

        // The export modal is present but hidden on page load. We instantiate the page object before opening the modal
        // in order to prevent the fade effect (see BaseModal).
        new ExportModal();
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        new OtherFormatPane().clickExportButton("Export as PDF");

        return new PDFExportOptionsModal();
    }
}
