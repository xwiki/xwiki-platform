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
package org.xwiki.extension.security.test.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.test.po.ExtensionAdministrationPage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.Matchers.hasItem;
import static org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage.getToExtensionVulnerabilitiesAdmin;

/**
 * Overall Extension Security UI tests.
 *
 * @version $Id$
 * @since 15.5
 */
@UITest
class ExtensionSecurityIT
{
    @Test
    void extensionVulnerabilitiesAdmin(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String internalRestURL = createSecurityVulnerabilitiesSource(setup, testReference);

        getToExtensionVulnerabilitiesAdmin()
            .setScanURL(internalRestURL)
            .saveConfig();

        ExtensionAdministrationPage.gotoPage().startIndex();

        LiveDataElement liveData = getToExtensionVulnerabilitiesAdmin().getLiveData();

        // Wait for a row to be present in the Live Data (meaning that the indexation is done).
        TableLayoutElement tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(1, true);

        tableLayout.assertRow("Name", "org.xwiki.platform:xwiki-platform-administration-ui");
        tableLayout.assertRow("Extension Id",
            hasItem(new WebElementStartsWith("org.xwiki.platform:xwiki-platform-administration-ui/")));
        tableLayout.assertRow("Wikis", "xwiki");
        tableLayout.assertRow("Max CVSS", "9.9");
        tableLayout.assertRow("CVE IDs", "GHSA-4v38-964c-xjmw (9.9)\n"
            + "GHSA-9j36-3cp4-rh4j (9.9)\n"
            + "GHSA-mgjw-2wrp-r535 (8.8)");
        tableLayout.assertRow("Latest Fix Version", "140.10.2");
    }

    private String createSecurityVulnerabilitiesSource(TestUtils setup, TestReference testReference) throws IOException
    {
        DocumentReference testDocumentReference =
            new DocumentReference(testReference.getWikiReference().getName(), List.of("XWiki", "Extension", "Security"),
                "Test");
        setup.createPage(testDocumentReference, "");
        WikiEditPage wikiEditPage = new ViewPage().editWiki();
        // Can't create the content using setup.createPage as the content is too large.
        wikiEditPage.setContent("{{velocity wiki='false'}}\n"
            + IOUtils.toString(getClass().getClassLoader().getResourceAsStream("testcontent.vm"),
            Charset.defaultCharset())
            + "{{/velocity}}");
        wikiEditPage.clickSaveAndView();
        return getLocalRestURL(setup, testDocumentReference);
    }

    private static String getLocalRestURL(TestUtils setup, DocumentReference testDocumentReference)
        throws MalformedURLException
    {
        String externalURL = setup.getURL(testDocumentReference, "get", "outputSyntax=plain");
        URL originalURL = new URL(externalURL);
        URL newURL = new URL(originalURL.getProtocol(), "localhost", originalURL.getPort(), originalURL.getFile());
        return newURL.toString();
    }

    /**
     * Custom hamcrest matcher to assert if a {@link WebElement} text starts with the provided prefix.
     */
    private static class WebElementStartsWith extends TypeSafeMatcher<WebElement>
    {
        private final String prefix;

        public WebElementStartsWith(String prefix)
        {
            this.prefix = prefix;
        }

        @Override
        protected boolean matchesSafely(WebElement item)
        {
            return item.getText().startsWith(this.prefix);
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendValue(this.prefix);
        }
    }
}
