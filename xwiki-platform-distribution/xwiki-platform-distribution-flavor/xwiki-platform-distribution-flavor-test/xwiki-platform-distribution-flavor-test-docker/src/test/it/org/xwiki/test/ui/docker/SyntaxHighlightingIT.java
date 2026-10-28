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
package org.xwiki.test.ui.docker;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate the syntax highlighting of the wiki editor, provided by the Syntax Highlighting application shipped with
 * the standard flavor. The application wraps the content text area in a CodeMirror editor whose resources are loaded
 * from the CodeMirror WebJar, so this test also covers the packaging of that WebJar and the way its URL is computed.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
// standardFlavor = true because the Syntax Highlighting application is a contributed extension that reaches the users
// only through the standard flavor, which installs it along with the CodeMirror WebJar it depends on.
@UITest(standardFlavor = true)
class SyntaxHighlightingIT
{
    private static final String CONTENT = "{{html}}test{{/html}}";

    private static final String ADDED_CONTENT = "{{html}}new{{/html}}";

    // The Syntax Highlighting application doesn't provide page objects for its CodeMirror based editor, so we have to
    // locate its DOM here. The application should provide them instead, so that the tests don't have to depend on the
    // internal markup of CodeMirror. Note that CodeMirror hides the content text area and inserts its own widget
    // right after it.
    private static final By LINE = By.cssSelector("#content ~ .CodeMirror .CodeMirror-code pre.CodeMirror-line");

    // The XWiki syntax mode reports the macro markers as "tag" tokens, that CodeMirror renders using the cm-tag class.
    private static final By MACRO_MARKER = By.cssSelector("#content ~ .CodeMirror .CodeMirror-code .cm-tag");

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    void highlightContent(TestUtils setup, TestReference testReference, XWikiWebDriver driver) throws Exception
    {
        setup.createPage(testReference, CONTENT, "Syntax Highlighting");

        WikiEditPage editPage = WikiEditPage.gotoPage(testReference);

        // The macro markers are highlighted only after CodeMirror, the XWiki syntax mode and the WebJar resources they
        // are loaded from have all been fetched successfully.
        driver.waitUntilElementIsVisible(MACRO_MARKER);
        assertEquals(List.of(CONTENT), getLines(driver));
        assertEquals("{{html}}{{/html}}", getMacroMarkers(driver));

        // Type on a new line, to check that the editor is editable and that the typed content is highlighted too.
        driver.findElement(LINE).click();
        driver.createActions().sendKeys(Keys.END).sendKeys(Keys.ENTER).sendKeys(ADDED_CONTENT).perform();
        assertEquals(List.of(CONTENT, ADDED_CONTENT), getLines(driver));
        assertEquals("{{html}}{{/html}}{{html}}{{/html}}", getMacroMarkers(driver));

        // Saving has to push the content of the editor back into the content text area.
        ViewPage viewPage = editPage.clickSaveAndView();
        assertEquals("test\nnew", viewPage.getContent());
        assertEquals(CONTENT + "\n" + ADDED_CONTENT, setup.rest().<Page>get(testReference).getContent());
    }

    private List<String> getLines(XWikiWebDriver driver)
    {
        return driver.findElementsWithoutWaiting(LINE).stream().map(WebElement::getText).toList();
    }

    private String getMacroMarkers(XWikiWebDriver driver)
    {
        return driver.findElementsWithoutWaiting(MACRO_MARKER).stream().map(WebElement::getText)
            .collect(Collectors.joining());
    }
}
