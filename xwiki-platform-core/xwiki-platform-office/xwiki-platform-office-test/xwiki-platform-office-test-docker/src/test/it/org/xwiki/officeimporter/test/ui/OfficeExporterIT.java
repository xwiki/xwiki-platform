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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.tika.internal.TikaUtils;

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

    @Test
    void exportODT(TestUtils setup, TestConfiguration testConfiguration, TestReference testReference) throws Exception
    {
        export(setup, testConfiguration, testReference, "odt", "application/vnd.oasis.opendocument.text");
    }

    @Test
    void exportRTF(TestUtils setup, TestConfiguration testConfiguration, TestReference testReference) throws Exception
    {
        export(setup, testConfiguration, testReference, "rtf", "application/rtf");
    }

    private static void export(TestUtils setup, TestConfiguration testConfiguration, TestReference testReference,
        String format, String expectedTikaDetect)
        throws IOException, URISyntaxException
    {
        setup.createPage(testReference, "content", "title");
        String exportURL = setup.getURL(testReference, "export", "format=" + format);
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        // Replace the host and port to match the one allowing to access the servlet externally.
        URI externalURI = new URIBuilder(exportURL)
            .setHost(servletEngine.getIP())
            .setPort(servletEngine.getPort())
            .build();
        try (InputStream inputStream = externalURI.toURL().openStream()) {
            assertEquals(expectedTikaDetect, TikaUtils.detect(inputStream));
        }
    }
}
