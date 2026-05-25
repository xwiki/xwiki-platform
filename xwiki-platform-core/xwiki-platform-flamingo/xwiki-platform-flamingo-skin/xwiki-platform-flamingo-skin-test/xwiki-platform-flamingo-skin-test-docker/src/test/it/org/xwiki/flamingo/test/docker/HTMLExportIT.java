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
package org.xwiki.flamingo.test.docker;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verify that the HTML export features works fine.
 *
 * @version $Id$
 */
// Note: vnc is set to false since otherwise our test framework fails to take a video since no page has been accessed
// by the test via Selenium. Since we don't need VNC, we turn it off and thus no video is taken.
@UITest(vnc = false)
class HTMLExportIT
{
    private String baseURL;

    private interface PageValidator
    {
        void validate(ZipInputStream zis, ZipEntry entry) throws Exception;

        void assertResult();
    }

    private static final class TopPageValidator implements PageValidator
    {
        private boolean result;

        @Override
        public void validate(ZipInputStream zis, ZipEntry entry) throws Exception
        {
            if (entry.getName().equals("pages/xwiki/TopPage/WebHome.html")) {
                String content = IOUtils.toString(zis, Charset.defaultCharset());
                assertTrue(content.contains("Top content"), "Title should have contained 'Top content'");
                assertTrue(content.contains("Top title: Creator"),
                    "Content should have contained 'Top title: Creator'");
                this.result = true;
            }
        }

        @Override
        public void assertResult()
        {
            assertTrue(this.result, "Failed to find the pages/xwiki/TopPage/WebHome.html entry");
        }
    }

    private static final class NestedPageValidator implements PageValidator
    {
        private boolean result;

        @Override
        public void validate(ZipInputStream zis, ZipEntry entry) throws Exception
        {
            if (entry.getName().equals("pages/xwiki/TopPage/NestedPage/WebHome.html")) {
                String content = IOUtils.toString(zis, Charset.defaultCharset());
                assertTrue(
                    content.contains("<a href=\"../../../../pages/xwiki/TopPage/WebHome.html\">top</a>"),
                    "Content should have contained a local link to the Top page");
                this.result = true;
            }
        }

        @Override
        public void assertResult()
        {
            assertTrue(this.result, "Failed to find the pages/xwiki/TopPage/NestedPage/WebHome.html entry");
        }
    }

    @BeforeEach
    void setUp(TestUtils setup)
    {
        this.baseURL = setup.getCurrentExecutor().getHttpClientBaseURL();
        setup.loginAsSuperAdmin();

        setup.deletePage("TopPage", "WebHome");
        setup.deletePage(new DocumentReference("xwiki", List.of("TopPage", "NestedPage"), "WebHome"));

        // Note: Verify that Velocity is correctly evaluated
        setup.createPage("TopPage", "WebHome", "Top content",
            "Top title: $services.localization.render('creator')");
        // Note: we define a link to the top page to verify that the export will resolve the links locally when the
        // page linked is part of the export.
        setup.createPage(List.of("TopPage", "NestedPage"), "WebHome", "[[top>>TopPage.WebHome]]", "Nested Page");
    }

    @Test
    void exportHTML() throws Exception
    {
        // Step 1: Call the export URL to get the ZIP and to assert its content, when no "pages" query string param is
        //         used (only the TopPage will be exported)
        assertHTMLExportURL(this.baseURL + "bin/export/TopPage/WebHome?format=html",
            List.of(new TopPageValidator()));

        // Step 2: Call the export URL to get the ZIP and to assert its content, when a "pages" query string param is
        //         used with some regex
        assertHTMLExportURL(
            this.baseURL + "bin/export/UnexistingSpace/UnexistingPage?format=html&pages=TopPage.%25",
            List.of(new TopPageValidator(), new NestedPageValidator()));
    }

    private void assertHTMLExportURL(String htmlExportURL, List<PageValidator> validators) throws Exception
    {
        URL url = new URL(htmlExportURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        ZipInputStream zis = new ZipInputStream(is);

        boolean foundResourcesDirectory = false;
        boolean foundSkinsDirectory = false;
        boolean foundSkinCSS = false;
        boolean foundWebjars = false;

        // We must read the full stream as otherwise if we close it before we've fully read it
        // then the server side will get a broken pipe since it's still trying to send data on it.
        for (ZipEntry entry; (entry = zis.getNextEntry()) != null; zis.closeEntry()) {
            for (PageValidator validator : validators) {
                validator.validate(zis, entry);
            }
            if (entry.getName().endsWith(".vm")) {
                fail("There shouldn't be any *.vm files in the generated zip!");
            } else if (entry.getName().endsWith(".less")) {
                fail("There shouldn't be any *.less files in the generated zip!");
            } else if (entry.getName().equals("xwiki.properties")) {
                fail("There shouldn't be any xwiki.properties file in the generated zip!");
            } else if (entry.getName().startsWith("resources/")) {
                foundResourcesDirectory = true;
                IOUtils.readLines(zis, Charset.defaultCharset());
            } else if (entry.getName().startsWith("skins/")) {
                foundSkinsDirectory = true;
                if (entry.getName().equals("skins/flamingo/style.min.css")) {
                    assertSkinIsActive(IOUtils.readLines(zis, Charset.defaultCharset()));
                    foundSkinCSS = true;
                } else {
                    IOUtils.readLines(zis, Charset.defaultCharset());
                }
            } else if (entry.getName().startsWith("webjars")) {
                foundWebjars = true;
                IOUtils.readLines(zis, Charset.defaultCharset());
            } else {
                IOUtils.readLines(zis, Charset.defaultCharset());
            }
        }

        for (PageValidator validator : validators) {
            validator.assertResult();
        }
        assertTrue(foundResourcesDirectory, "Failed to find the resources/ directory entry");
        assertTrue(foundSkinsDirectory, "Failed to find the skins/ directory entry");
        assertTrue(foundSkinCSS, "Failed to find the link to colibri.css in style.min.css");
        assertTrue(foundWebjars, "Failed to find webjar resources in the HTML export");

        zis.close();
    }

    private void assertSkinIsActive(List<String> content)
    {
        assertTrue(StringUtils.join(content.toArray()).contains("skin-flamingo"),
            "style.min.css is not the one output by the flamingo skin");
    }
}
