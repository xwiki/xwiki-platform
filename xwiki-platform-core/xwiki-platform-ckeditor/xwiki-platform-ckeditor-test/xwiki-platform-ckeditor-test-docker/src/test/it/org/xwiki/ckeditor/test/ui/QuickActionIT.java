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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.QuickActionDropdown;

import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;


/**
 * All functional tests for Quick Actions.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@UITest
public class QuickActionIT 
{
    
    /**
     * Test string that will be inserted to check formatting.
     */
    private static final String TEST_TEXT = "Hello, world!";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        createAndLoginStandardUser(setup);
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
        
        // Run the first quick action
        QuickActionDropdown dropdown = new QuickActionDropdown(editor);
        
        dropdown.open();
        dropdown.sendKeys("heading 1");
        dropdown.submit();
        
        editor.getRichTextArea().sendKeys(TEST_TEXT);
        assert dropdown.findElementsWithContent("h1", TEST_TEXT);
        editPage.clickSaveAndView();
    }
}
