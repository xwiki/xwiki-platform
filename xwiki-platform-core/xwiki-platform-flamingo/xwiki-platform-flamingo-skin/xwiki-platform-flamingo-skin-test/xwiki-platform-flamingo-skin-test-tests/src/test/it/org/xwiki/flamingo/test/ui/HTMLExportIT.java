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
package org.xwiki.flamingo.test.ui;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.LogLevel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verify that the HTML export features works fine.
 *
 * @version $Id$
 */
public class HTMLExportIT extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule adminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Rule
    public AllLogRule logRule = new AllLogRule(LogLevel.WARN);

    private interface PageValidator
    {
        void validate(ZipInputStream zis, ZipEntry entry) throws Exception;

        void assertResult();
    }

    private final class TopPageValidator implements PageValidator
    {
        private boolean result;

        @Override
        public void validate(ZipInputStream zis, ZipEntry entry) throws Exception
        {
            if (entry.getName().equals("pages/xwiki/TopPage/WebHome.html")) {
                String content = IOUtils.toString(zis, Charset.defaultCharset());

                // Verify that the content was rendered properly
                assertTrue("Title should have contained 'Top content'", content.contains("Top content"));
                assertTrue("Content should have contained 'Top title: Creator'",
                    content.contains("Top title: Creator"));
                this.result = true;
            }
        }

        @Override
        public void assertResult()
        {
            assertTrue("Failed to find the pages/xwiki/TopPage/WebHome.html entry", this.result);
        }
    }

    private final class NestedPageValidator implements PageValidator
    {
        private boolean result;

        @Override
        public void validate(ZipInputStream zis, ZipEntry entry) throws Exception
        {
            if (entry.getName().equals("pages/xwiki/TopPage/NestedPage/WebHome.html")) {
                String content = IOUtils.toString(zis, Charset.defaultCharset());

                // Verify that the link to a locally exported page is correct
                assertTrue("Content should have contained a local link to the Top page",
                    content.contains("<a href=\"../../../../pages/xwiki/TopPage/WebHome.html\">top</a>"));
                this.result = true;
            }
        }

        @Override
        public void assertResult()
        {
            assertTrue("Failed to find the pages/xwiki/TopPage/NestedPage/WebHome.html entry", this.result);
        }
    }

    @Test
    public void exportHTML() throws Exception
    {
        // Step 1: Create 2 pages that we'll then export

        EntityReference topReference = getUtil().resolveDocumentReference("TopPage.WebHome");
        getUtil().deletePage(topReference);
        EntityReference nestedReference = getUtil().resolveDocumentReference("TopPage.NestedPage.WebHome");
        getUtil().deletePage(nestedReference);

        // Note: Verify that Velocity is correctly evaluated
        getUtil().createPage(topReference, "Top content", "Top title: $services.localization.render('creator')");
        // Note: we define a link to the top page to verify that the export will resolve the links locally when the
        // page linked is part of the export.
        getUtil().createPage(nestedReference, "[[top>>TopPage.WebHome]]", "Nested Page");

        // Step 2: Call the export URL to get the ZIP and to assert its content, when no "pages" query string param is
        //         used (only the TopPage will be exported)
        assertHTMLExportURL("http://localhost:8080/xwiki/bin/export/TopPage/WebHome?format=html",
            Arrays.asList(new TopPageValidator()));

        // Step 3: Call the export URL to get the ZIP and to assert its content, when a "pages" query string param is
        //         used with some regex
        assertHTMLExportURL("http://localhost:8080/xwiki/bin/export/UnexistingSpace/UnexistingPage?format=html"
            + "&pages=TopPage.%25", Arrays.asList(new TopPageValidator(),  new NestedPageValidator()));

        // Verify that there was no warning or more severe logs output to the console.
        StringBuilder builder = new StringBuilder("Should not have got the following logs [\n");
        for (int i = 0; i < this.logRule.size(); i++) {
            builder.append(" - ["). append(this.logRule.getMessage(i)).append("]\n");
        }
        builder.append("]");
        assertEquals(builder.toString(), 0, this.logRule.size());
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
                // Verify that the skin is correctly going to be applied by verifying the flamingo/style.min.css file is
                // found and is correctly referenced. This fixes https://jira.xwiki.org/browse/XWIKI-9145
                if (entry.getName().equals("skins/flamingo/style.min.css")) {
                    assertSkinIsActive(IOUtils.readLines(zis, Charset.defaultCharset()));
                    foundSkinCSS = true;
                } else {
                    IOUtils.readLines(zis, Charset.defaultCharset());
                }
            } else if (entry.getName().startsWith("webjars")) {
                // We verify here that webjars URLs have been properly exported
                foundWebjars = true;
                IOUtils.readLines(zis, Charset.defaultCharset());
            } else {
                IOUtils.readLines(zis, Charset.defaultCharset());
            }
        }

        for (PageValidator validator : validators) {
            validator.assertResult();
        }
        assertTrue("Failed to find the resources/ directory entry", foundResourcesDirectory);
        assertTrue("Failed to find the skins/ directory entry", foundSkinsDirectory);
        assertTrue("Failed to find the link to colibri.css in style.min.css", foundSkinCSS);
        assertTrue("Failed to find webjar resources in the HTML export", foundWebjars);

        zis.close();
    }

    private void assertSkinIsActive(List<String> content) throws Exception
    {
        assertTrue("style.min.css is not the one output by the flamingo skin", StringUtils.join(content.toArray())
            .contains("skin-flamingo"));
    }
}
