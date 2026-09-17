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

import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.TestUtils.RestTestUtils;
import org.xwiki.test.ui.po.DocExtraPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the ability to add custom document extra tabs.
 *
 * @version $Id$
 * @since 17.9.0-rc-1
 * @since 17.4.6
 * @since 16.10.13
 */
@UITest(properties = {
    // The slow document extra tab of openDocExtraTabWhileAnotherOneIsStillLoading uses the Groovy macro, which needs
    // Programming Rights, and functional tests deny that right to wiki content unless the page is excluded here.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:DocExtraTabsIT\\..*"
})
class DocExtraTabsIT
{
    @Test
    @Order(1)
    void createAndLoadCustomDocExtraTab(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        createCustomDocExtraTab(setup, reference, "Content of test tab.",
            Map.of("show", "true", "title", "Test", "name", "test", "shortcut", "alt+shift+t"));
        String tabId = reference.toString();

        ViewPage viewPage = setup.gotoPage(reference);
        assertTrue(viewPage.hasDocExtraPane(tabId));

        DocExtraPane docExtraPane = viewPage.openDocExtraPane(tabId);
        assertEquals("Content of test tab.", docExtraPane.getText());

        viewPage.openCommentsDocExtraPane();
        assertFalse(viewPage.isDocExtraPaneActive(tabId));

        docExtraPane = viewPage.useShortcutForDocExtraPane(tabId, Keys.chord(Keys.ALT, Keys.SHIFT, "t"));
        assertEquals("Content of test tab.", docExtraPane.getText());
    }

    @Test
    @Order(2)
    void openDocExtraTabWhileAnotherOneIsStillLoading(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        // This tab takes several seconds to load, which leaves us the time to open another tab before its response
        // arrives.
        createCustomDocExtraTab(setup, reference,
            "{{groovy}}Thread.sleep(5000); println 'Slow tab content.'{{/groovy}}",
            Map.of("show", "true", "title", "Slow", "name", "slow"));
        String slowTabId = reference.toString();

        ViewPage viewPage = setup.gotoPage(reference);
        viewPage.waitForDocExtraPaneActive("Comments");

        // Start loading the slow tab and, without waiting for it, open the history tab, which loads fast. The
        // response of the slow tab thus arrives while the history tab is displayed.
        setup.getDriver().findElement(By.id(slowTabId + "link")).click();
        viewPage.openHistoryDocExtraPane();

        // Wait for the content of the slow tab to arrive. We look at the text content because the pane is hidden.
        WebElement slowPane = setup.getDriver().findElement(By.id(slowTabId + "pane"));
        setup.getDriver().waitUntilCondition(driver -> !slowPane.getAttribute("textContent").isBlank());
        assertEquals("Slow tab content.", slowPane.getAttribute("textContent").trim());

        // The slow tab must not have taken the place of the history tab.
        assertTrue(viewPage.isDocExtraPaneActive("History"));
        assertFalse(viewPage.isDocExtraPaneActive(slowTabId));

        // The slow tab has been loaded, so opening it now displays it without loading it again (its content would be
        // duplicated otherwise).
        assertEquals("Slow tab content.", viewPage.openDocExtraPane(slowTabId).getText());
    }

    private void createCustomDocExtraTab(TestUtils setup, DocumentReference reference, String content,
        Map<String, String> parameters) throws Exception
    {
        // Recreate the page to make sure it doesn't have any previous UI Extension object.
        setup.rest().delete(reference);
        setup.rest().savePage(reference, "", "");

        // Add the UI Extension object that defines the new document extra tab.
        org.xwiki.rest.model.jaxb.Object object = setup.rest().object(reference, "XWiki.UIExtensionClass", 0);
        object.getProperties().add(RestTestUtils.property("content", content));
        object.getProperties().add(RestTestUtils.property("extensionPointId", "org.xwiki.plaftorm.template.docextra"));
        object.getProperties().add(RestTestUtils.property("name", reference.toString()));
        object.getProperties().add(RestTestUtils.property("parameters", parameters.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue()).reduce((a, b) -> a + "\n" + b).orElse("")));
        object.getProperties().add(RestTestUtils.property("scope", "wiki"));
        setup.rest().add(object);
    }
}
