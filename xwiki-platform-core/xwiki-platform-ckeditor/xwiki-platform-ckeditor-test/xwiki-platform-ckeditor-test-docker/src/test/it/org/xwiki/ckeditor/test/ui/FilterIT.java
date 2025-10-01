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
package org.xwiki.ckeditor.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.text.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests how CKEditor filters the content.
 * 
 * @version $Id$
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",
        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    resolveExtraJARs = true
)
class FilterIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        // Ensure that raw HTML is allowed.
        setup.loginAsSuperAdmin();
    }

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference);
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    @Test
    @Order(1)
    void escapeStyleContent()
    {
        StringBuilder source = new StringBuilder();
        source.append("before\n");
        source.append("\n");
        source.append("{{html clean=\"false\"}}\n");
        source.append("<style>\n");
        source.append("p::before {\n");
        source.append("  content: '<';\n");
        source.append("}\n");
        source.append("</style>\n");
        source.append("<div>inside</div>\n");
        source.append("<style id=\"second\">\n");
        source.append("div::after {\n");
        source.append("  content: '<';\n");
        source.append("}\n");
        source.append("</style>\n");
        source.append("{{/html}}\n");
        source.append("\n");
        source.append("after");

        setSource(source.toString());
        String html = this.textArea.getContent();
        // Check that the style tag is in the macro output.
        assertThat(html, containsString("</div><style>"));
        // Check that the escaped style content is in the macro output.
        assertThat(html, containsString("content: '\\3C'"));
        // Check that the comment-escaped output is in the HTML comment.
        try {
            // The macro source is kept in an attribute, so it should be escaped. This is the behavior we see at runtime
            // when getting the inner HTML of the editing area.
            assertThat(html, containsString("content: '&lt;\\'"));
        } catch (AssertionError e) {
            // The version of Chrome used to run the Docker tests (or maybe the Chrome WebDriver) doesn't escape '<' in
            // the attribute value, as it happens at runtime.
            assertThat(html, containsString("content: '<\\'"));
        }

        this.textArea.sendKeys(" end");
        // Verify that the origial style content is preserved.
        assertSourceContains(source.toString() + " end");
    }

    @Test
    @Order(2)
    void listUnbreakableSpaceMerge()
    {
        // See https://jira.xwiki.org/browse/XWIKI-22024.
        this.editor.getToolBar().clickNumberedList();

        // Write the triggering text: any text between round brackets, followed by more text, a space and more text
        // again.
        this.textArea.sendKeys("(test)s test");

        // Move to inside the round brackets and change the content inside the bracket.
        this.textArea.sendKeys(StringUtils.repeat(Keys.ARROW_LEFT.toString(), 7));
        this.textArea.sendKeys(Keys.BACK_SPACE);

        // Move back to after the round brackets and erase the text right after the closing bracket.
        this.textArea.sendKeys(Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.BACK_SPACE);

        this.assertSourceEquals("1. (tes) test");
    }
}
