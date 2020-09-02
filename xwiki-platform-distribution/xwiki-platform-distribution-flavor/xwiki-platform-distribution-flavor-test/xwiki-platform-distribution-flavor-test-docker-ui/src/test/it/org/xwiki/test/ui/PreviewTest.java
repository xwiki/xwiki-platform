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
package org.xwiki.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.PreviewableEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Various tests for verifying the preview mode of a page.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class PreviewTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    /**
     * @see "XWIKI-2490: Preview doesn't work when the document content has script requiring programming rights"
     */
    @Test
    @Order(1)
    void previewWithProgrammingRights(TestReference testReference)
    {
        String testClassName = testReference.getLastSpaceReference().getName();
        String testMethodName = testReference.getName();
        WikiEditPage wikiEditPage = WikiEditPage.gotoPage(testClassName, testMethodName);
        wikiEditPage.setContent("{{velocity}}$xwiki.hasAccessLevel('programming')"
            + " $tdoc.author $tdoc.contentAuthor $tdoc.creator{{/velocity}}");
        PreviewEditPage previewPage = wikiEditPage.clickPreview();
        assertEquals("true XWiki.Admin XWiki.Admin XWiki.Admin", previewPage.getContent());
    }

    /**
     * @see "XWIKI-9527: Sheets are not applied on preview action if the document is new"
     */
    @Test
    @Order(2)
    void previewWithSheet(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String testClassName = testReference.getLastSpaceReference().getName();
        String testMethodName = testReference.getName();
        // Create the class.
        testUtils.rest().deletePage(testClassName, testMethodName + "Class");
        ClassEditPage classEditor = ClassEditPage.gotoPage(testClassName, testMethodName + "Class");
        classEditor.addProperty("color", "String");

        // Create the sheet.
        testUtils.rest().savePage(new LocalDocumentReference(testClassName, testMethodName + "Sheet"),
            "{{velocity}}$doc.display('color'){{/velocity}}", "");

        // Bind the class to the sheet.
        ObjectEditPage objectEditor = ObjectEditPage.gotoPage(testClassName, testMethodName + "Class");
        ObjectEditPane objectEditPane = objectEditor.addObject("XWiki.ClassSheetBinding");
        SuggestInputElement sheetPicker = objectEditPane.getSuggestInput("sheet");
        sheetPicker.sendKeys(testClassName + "." + testMethodName + "Sheet").waitForSuggestions()
            .sendKeys(Keys.ENTER);
        objectEditor.clickSaveAndContinue();

        // Create the template.
        String classFullName = testClassName + "." + testMethodName + "Class";
        testUtils.rest().deletePage(testClassName, testMethodName + "Template");
        objectEditor = ObjectEditPage.gotoPage(testClassName, testMethodName + "Template");
        objectEditPane = objectEditor.addObject(classFullName);
        objectEditPane.setFieldValue(objectEditPane.byPropertyName("color"), "red");
        objectEditor.clickSaveAndContinue();

        // Create the test instance.
        testUtils.rest().deletePage(testClassName, testMethodName);
        testUtils.gotoPage(testClassName, testMethodName, "edit",
            "template=" + testClassName + "." + testMethodName + "Template");
        objectEditPane = new ObjectEditPane(new InlinePage().getFormLocator(), classFullName, 0);
        objectEditPane.setFieldValue(objectEditPane.byPropertyName("color"), "green");

        // Test the preview when the page is not yet saved.
        PreviewableEditPage editPage = new PreviewableEditPage();
        PreviewEditPage previewPage = editPage.clickPreview();
        assertEquals("green", previewPage.getContent());

        // Test the preview after the page is saved.
        previewPage.clickBackToEdit().clickSaveAndView().editInline().clickPreview();
        assertEquals("green", previewPage.getContent());
    }
}
