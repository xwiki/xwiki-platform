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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for the Save plugin.
 * 
 * @version $Id$
 * @since 1.13
 */
@UITest
public class SaveTest
{
    @BeforeAll
    public static void configure(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();

        // Set default edit mode to WYSIWYG.
        testUtils.setWikiPreference("editor", "Wysiwyg");

        // Set default WYSIWYG editor to CKEditor.
        Map<String, String> editorBinding = new HashMap<>();
        editorBinding.put("dataType", "org.xwiki.rendering.syntax.SyntaxContent#wysiwyg");
        editorBinding.put("roleHint", "ckeditor");
        testUtils.addObject(new LocalDocumentReference("XWiki", "XWikiPreferences"), "XWiki.EditorBindingClass",
            editorBinding);

        // Run the tests as a simple user.
        testUtils.createUserAndLogin(SaveTest.class.getSimpleName(), "password");
    }

    @Test
    public void save(TestInfo testInfo) throws Exception
    {
        WYSIWYGEditPage editPage = WYSIWYGEditPage.gotoPage(testInfo.getTestClass().get().getSimpleName(),
            testInfo.getTestMethod().get().getName());
        CKEditor editor = new CKEditor().waitToLoad();
        RichTextAreaElement textArea = editor.getRichTextArea();
        textArea.clear();
        textArea.sendKeys("xyz");
        editPage.clickSaveAndView().edit();
        editPage.waitUntilPageJSIsLoaded();
        assertEquals("<p>xyz</p>", editor.waitToLoad().getRichTextArea().getContent());
    }
}
