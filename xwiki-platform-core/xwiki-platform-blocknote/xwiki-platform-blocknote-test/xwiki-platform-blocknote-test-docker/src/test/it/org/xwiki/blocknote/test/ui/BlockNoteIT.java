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
package org.xwiki.blocknote.test.ui;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openqa.selenium.Keys.CONTROL;
import static org.openqa.selenium.Keys.END;

/**
 * Tests of the BlockNote integration.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@UITest
class BlockNoteIT
{
    /**
     * Run the minimal steps to validate the editor is loading: edit an existing page, add some content, save the
     * result.
     */
    @Test
    void minimal(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Make Blocknote the default to not have to explicit it later.
        setup.addObject("XWiki", "XWikiPreferences", "XWiki.EditorBindingClass", "dataType",
            "org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "roleHint", "blocknote");
        // Make the user advance to have access to the edit dropdown.
        setup.createUserAndLogin("U1", "$U1", "editor", "Wysiwyg", "usertype", "Advanced");

        // Create a page with a small content, edit it, add a little basic content, save and observe the content
        // changed.
        String textContent = "Test";
        ViewPage page = setup.createPage(testReference, textContent, "", "markdown/1.2");
        page.edit();
        XWikiWebDriver driver = setup.getDriver();
        WebElement blocknoteEditableContent =
            driver.findElement(By.cssSelector(".xwiki-blocknote .bn-container .bn-editor"));
        assertEquals(textContent, blocknoteEditableContent.getText());
        String addedContent = "123";
        blocknoteEditableContent.click();

        // Move cursor to end using keyboard shortcuts, then insert the content.
        Actions actions = new Actions(driver.getWrappedDriver());
        actions.keyDown(CONTROL).sendKeys(END).keyUp(CONTROL).sendKeys(addedContent).perform();
        ViewPage postSavePage = ((Callable<ViewPage>) () -> new WYSIWYGEditPage().clickSaveAndView()).call();
        assertEquals("""
            %s
            %s""".formatted(textContent, addedContent), postSavePage.getContent());
    }
}
