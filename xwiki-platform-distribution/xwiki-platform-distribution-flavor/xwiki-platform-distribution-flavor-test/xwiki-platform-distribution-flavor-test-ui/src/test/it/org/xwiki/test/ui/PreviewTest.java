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

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.PreviewableEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Various tests for verifying the preview mode of a page.
 * 
 * @version $Id$
 * @since 5.2
 */
public class PreviewTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * @see "XWIKI-2490: Preview doesn't work when the document content has script requiring programming rights"
     */
    @Test
    public void previewWithProgrammingRights()
    {
        WikiEditPage wikiEditPage = WikiEditPage.gotoPage(getTestClassName(), getTestMethodName());
        wikiEditPage.setContent("{{velocity}}$xwiki.hasAccessLevel('programming')"
            + " $tdoc.author $tdoc.contentAuthor $tdoc.creator{{/velocity}}");
        PreviewEditPage previewPage = wikiEditPage.clickPreview();
        assertEquals("true XWiki.Admin XWiki.Admin XWiki.Admin", previewPage.getContent());
    }

    /**
     * @see "XWIKI-9527: Sheets are not applied on preview action if the document is new"
     */
    @Test
    public void previewWithSheet() throws Exception
    {
        // Create the class.
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName() + "Class");
        ClassEditPage classEditor = ClassEditPage.gotoPage(getTestClassName(), getTestMethodName() + "Class");
        classEditor.addProperty("color", "String");

        // Create the sheet.
        getUtil().rest().savePage(new LocalDocumentReference(getTestClassName(), getTestMethodName() + "Sheet"),
            "{{velocity}}$doc.display('color'){{/velocity}}", "");

        // Bind the class to the sheet.
        ObjectEditPage objectEditor = ObjectEditPage.gotoPage(getTestClassName(), getTestMethodName() + "Class");
        ObjectEditPane objectEditPane = objectEditor.addObject("XWiki.ClassSheetBinding");
        SuggestInputElement sheetPicker = objectEditPane.getSuggestInput("sheet");
        sheetPicker.sendKeys(getTestClassName() + "." + getTestMethodName() + "Sheet").waitForSuggestions()
            .sendKeys(Keys.ENTER);
        objectEditor.clickSaveAndContinue();

        // Create the template.
        String classFullName = getTestClassName() + "." + getTestMethodName() + "Class";
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName() + "Template");
        objectEditor = ObjectEditPage.gotoPage(getTestClassName(), getTestMethodName() + "Template");
        objectEditPane = objectEditor.addObject(classFullName);
        objectEditPane.setFieldValue(objectEditPane.byPropertyName("color"), "red");
        objectEditor.clickSaveAndContinue();

        // Create the test instance.
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit",
            "template=" + getTestClassName() + "." + getTestMethodName() + "Template");
        objectEditPane = new ObjectEditPane(new InlinePage().getForm(), classFullName, 0);
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
