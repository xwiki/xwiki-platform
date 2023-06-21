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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.QuickActionsDropdown;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;


/**
 * All functional tests for Quick Actions.
 *
 * @version $Id$
 * @since 15.5
 */
@UITest
public class QuickActionsIT 
{
    
    /**
     * Test string that will be inserted to check formatting.
     */
    private static final String TEST_TEXT = "Hello, world!";

    @BeforeAll
    void setUp(TestUtils setup)
    {
        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        createAndLoginStandardUser(setup);
    }
    
    @BeforeEach
    void cleanUp(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
    }
    
    
    private static void createAndLoginStandardUser(TestUtils setup)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }
    
    
    @Test
    @Order(1)
    void heading1(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();
        
        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        QuickActionsDropdown qa = editor.openQuickActionsDropdown();
        textArea.sendKeys("hea");
        qa.waitForQuickActionSelected("Heading 1");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForQuickActionSubmitted();
        
        // Write some text
        textArea.sendKeys(TEST_TEXT);
        
        assert textArea.getContent().contains("<h1>" + TEST_TEXT + "<br></h1>");
        editPage.clickSaveAndView();
    }
    
    @Test
    @Order(2)
    void heading2(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();
        
        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        QuickActionsDropdown qa = editor.openQuickActionsDropdown();
        textArea.sendKeys("hea");
        qa.waitForQuickActionSelected("Heading 1");
        textArea.sendKeys(Keys.DOWN);
        qa.waitForQuickActionSelected("Heading 2");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForQuickActionSubmitted();
        
        // Write some text
        textArea.sendKeys(TEST_TEXT);
        
        assert textArea.getContent().contains("<h2>" + TEST_TEXT + "<br></h2>");
        editPage.clickSaveAndView();
    }
    
    @Test
    @Order(3)
    void heading3(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();
        
        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        QuickActionsDropdown qa = editor.openQuickActionsDropdown();
        textArea.sendKeys("hea");
        qa.waitForQuickActionSelected("Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN);
        qa.waitForQuickActionSelected("Heading 3");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForQuickActionSubmitted();
        
        // Write some text
        textArea.sendKeys(TEST_TEXT);
        
        assert textArea.getContent().contains("<h3>" + TEST_TEXT + "<br></h3>");
        editPage.clickSaveAndView();
    }
    
    @Test
    @Order(4)
    void paragraph(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();
        
        RichTextAreaElement textArea = editor.getRichTextArea();

        // Switch to another style
        QuickActionsDropdown qa = editor.openQuickActionsDropdown();
        textArea.sendKeys("hea");
        qa.waitForQuickActionSelected("Heading 1");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForQuickActionSubmitted();
        
        // Write some text
        textArea.sendKeys(TEST_TEXT);
        
        // Switch back to paragraph
        qa = editor.openQuickActionsDropdown();
        textArea.sendKeys("parag");
        qa.waitForQuickActionSelected("Paragraph");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForQuickActionSubmitted();
        
        assert textArea.getContent().contains("<p>" + TEST_TEXT + "<br></p>");
        editPage.clickSaveAndView();
    }
}
