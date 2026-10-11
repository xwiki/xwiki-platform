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
package org.xwiki.officeimporter.test.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.flamingo.skin.test.po.ExportModal;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tika.internal.TikaUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for the office exporter.
 *
 * @version $Id$
 */
@UITest(office = true, servletEngine = ServletEngine.TOMCAT,
    forbiddenEngines = {
        // These tests need to have XWiki running inside a Docker container (we chose Tomcat since it's the most
        // used one), because they need LibreOffice to be installed, and we cannot guarantee that it is installed on the
        // host machine.
        ServletEngine.JETTY_STANDALONE
    })
class OfficeExporterIT
{
    private static final String DOCUMENT_CONTENT = "This is a test document.";

    private static final String IMAGE_NAME = "image.png";

    @BeforeEach
    public void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Connect the wiki to the office server if it is not already done
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        administrationPage.clickSection("Content", "Office Server");
        OfficeServerAdministrationSectionPage officeServerAdministrationSectionPage =
            new OfficeServerAdministrationSectionPage();
        if (!"Connected".equals(officeServerAdministrationSectionPage.getServerState())) {
            officeServerAdministrationSectionPage.startServer();
        }
    }

    /**
     * Export to ODT, from the Export modal, a page holding some text and an image.
     */
    @Test
    @Order(1)
    void exportODT(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.rest().delete(testReference);
        setup.rest().savePage(testReference, String.format("%s%n%n[[image:%s]]", DOCUMENT_CONTENT, IMAGE_NAME),
            "Export");
        setup.attachFile(testReference, IMAGE_NAME, new ByteArrayInputStream(createImage()), true);
        ViewPage viewPage = setup.gotoPage(testReference);

        // Selecting ODT in the Export modal makes the browser download the file, so we fetch it ourselves.
        ExportModal exportModal = ExportModal.open(viewPage);
        String exportURL = exportModal.getExportURL("ODT");
        exportModal.close();

        String mimeType = null;
        String content = null;
        List<String> pictures = new ArrayList<>();
        try (ZipInputStream odt = new ZipInputStream(postExport(setup.toHttpClientUri(exportURL)))) {
            for (ZipEntry entry = odt.getNextEntry(); entry != null; entry = odt.getNextEntry()) {
                if ("mimetype".equals(entry.getName())) {
                    mimeType = new String(odt.readAllBytes(), StandardCharsets.UTF_8);
                } else if ("content.xml".equals(entry.getName())) {
                    content = new String(odt.readAllBytes(), StandardCharsets.UTF_8);
                } else if (entry.getName().startsWith("Pictures/")) {
                    pictures.add(entry.getName());
                }
            }
        }
        assertEquals("application/vnd.oasis.opendocument.text", mimeType);
        assertThat("The exported file has no content.xml", content, notNullValue());
        assertThat(content, containsString(DOCUMENT_CONTENT));
        // The image displayed by the page is embedded in the export.
        assertThat("The exported file has no picture", pictures, not(empty()));
    }

    @Test
    @Order(2)
    void exportRTF(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.createPage(testReference, "content", "title");
        String exportURL = setup.toHttpClientUri(setup.getURL(testReference, "export", "format=rtf"));
        try (InputStream inputStream = postExport(exportURL)) {
            assertEquals("application/rtf", TikaUtils.detect(inputStream));
        }
    }

    /**
     * @param exportURL the export URL
     * @return the exported file
     */
    private static InputStream postExport(String exportURL) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) URI.create(exportURL).toURL().openConnection();
        // The export action only performs an export on POST requests.
        connection.setRequestMethod("POST");
        return connection.getInputStream();
    }

    /**
     * @return a small PNG image
     */
    private static byte[] createImage() throws IOException
    {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
