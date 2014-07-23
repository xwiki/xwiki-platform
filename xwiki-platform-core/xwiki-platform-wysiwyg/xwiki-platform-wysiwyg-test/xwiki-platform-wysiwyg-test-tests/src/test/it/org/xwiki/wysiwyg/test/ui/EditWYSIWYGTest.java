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
package org.xwiki.wysiwyg.test.ui;

import java.net.URLDecoder;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;
import org.xwiki.wysiwyg.test.po.EditorElement;
import org.xwiki.wysiwyg.test.po.TableConfigPane;
import org.xwiki.wysiwyg.test.po.UploadImagePane;
import org.xwiki.wysiwyg.test.po.WYSIWYGEditPage;

/**
 * Test WYSIWYG content editing.
 * 
 * @version $Id$
 * @since 3.0M2
 */
public class EditWYSIWYGTest extends AbstractWYSIWYGEditorTest
{
    /**
     * Tests that images are uploaded fine after a preview.
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-5895">XWIKI-5895</a>: Adding an image in the WYSIWYG editor
     *      and previewing it without saving the page first makes the XWiki page corrupt.
     **/
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testUploadImageAfterPreview() throws Exception
    {
        this.editPage.clickPreview().clickBackToEdit();
        // Recreate the page object because the page has been reloaded.
        this.editPage = new WYSIWYGEditPage().waitUntilPageIsLoaded();
        UploadImagePane uploadImagePane = this.editPage.insertAttachedImage().selectFromCurrentPage().uploadImage();
        // URL#getPath() returns the path URL-encoded and the file upload input doesn't expect this. Normally we
        // shouldn't have special characters in the path but some CI jobs have spaces in their names
        // (e.g. "xwiki-platform Quality Checks") which are encoded as %20. The file upload hangs if we pass the path
        // with encoded spaces.
        String path = URLDecoder.decode(this.getClass().getResource("/image.png").getPath(), "UTF-8");
        uploadImagePane.setImageToUpload(path);
        // Fails if the image configuration step doesn't load in a decent amount of time.
        uploadImagePane.configureImage();
    }

    /**
     * @see "XWIKI:7028: Strange behaviour when pressing back and forward on a page that has 2 WYSIWYG editors
     *      displayed."
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testBackForwardCache()
    {
        StringBuilder code = new StringBuilder();
        code.append("; Description\n");
        code.append(": {{html}}<textarea id=\"description\">f&gt;o**o**</textarea>{{/html}}\n");
        code.append("; Summary\n");
        code.append(": {{html}}<textarea id=\"summary\">b&lt;a//r//</textarea>{{/html}}\n\n");

        code.append("{{velocity}}\n");
        code.append("{{html}}\n");
        code.append("#wysiwyg_editProperty($tdoc 'description')\n");
        code.append("#wysiwyg_editProperty($tdoc 'summary')\n");
        code.append("{{/html}}\n");
        code.append("{{/velocity}}");

        // Create a page with 2 WYSIWYG editors.
        getUtil().createPage(getTestClassName(), getTestMethodName(), code.toString(), null);

        EditorElement descriptionEditor = new EditorElement("description").waitToLoad();
        EditorElement summaryEditor = new EditorElement("summary").waitToLoad();

        String description = descriptionEditor.getRichTextArea().getText();
        String summary = summaryEditor.getRichTextArea().getText();
        getDriver().navigate().back();
        getDriver().navigate().forward();

        descriptionEditor = new EditorElement("description").waitToLoad();
        summaryEditor = new EditorElement("summary").waitToLoad();

        Assert.assertEquals(description, descriptionEditor.getRichTextArea().getText());
        Assert.assertEquals(summary, summaryEditor.getRichTextArea().getText());
    }

    /**
     * Test that the content of the rich text area is preserved when the user refreshes the page.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testPreserveUnsavedRichContentAgainstRefresh()
    {
        // Type text and refresh the page.
        this.editPage.getContentEditor().getRichTextArea().sendKeys("2");
        this.editPage.sendKeys(Keys.F5);

        this.editPage = new WYSIWYGEditPage();
        EditorElement editor = this.editPage.getContentEditor();
        editor.waitToLoad();

        // Type more text and check the result.
        RichTextAreaElement textArea = editor.getRichTextArea();
        textArea.sendKeys("1");
        Assert.assertEquals("12", textArea.getText());
    }

    /**
     * Test that the content of the source text area is preserved when the user refreshes the page.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testPreserveUnsavedSourceAgainstRefresh()
    {
        EditorElement editor = this.editPage.getContentEditor();
        editor.switchToSource();

        // Type text and refresh the page.
        editor.getSourceTextArea().sendKeys("1" + Keys.F5);

        this.editPage = new WYSIWYGEditPage();
        editor = this.editPage.getContentEditor();
        editor.waitToLoad();
        editor.switchToSource();

        // Type more text and check the result.
        editor.getSourceTextArea().sendKeys("2");
        Assert.assertEquals("12", editor.getSourceTextArea().getAttribute("value"));
    }

    /**
     * Tests that the currently active editor (WYSIWYG or Source) is preserved when the user refreshes the page.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testPreserveSelectedEditorAgainstRefresh()
    {
        // The WYSIWYG editor should be initially active.
        EditorElement editor = this.editPage.getContentEditor();
        Assert.assertFalse(editor.getSourceTextArea().isEnabled());

        // Switch to Source editor and refresh the page.
        editor.switchToSource();
        editor.getSourceTextArea().sendKeys(Keys.F5);

        this.editPage = new WYSIWYGEditPage();
        editor = this.editPage.getContentEditor();
        editor.waitToLoad();

        // The Source editor should be active now because it was selected before the refresh.
        Assert.assertTrue(editor.getSourceTextArea().isEnabled());

        // Switch to WYSIWYG editor and refresh the page again.
        editor.switchToWysiwyg();
        this.editPage.sendKeys(Keys.F5);

        this.editPage = new WYSIWYGEditPage();
        editor = this.editPage.getContentEditor();
        editor.waitToLoad();

        // The WYSIWYG editor should be active now because it was selected before the refresh.
        Assert.assertFalse(editor.getSourceTextArea().isEnabled());
    }

    /**
     * Test if an undo step reverts only one paste operation from a sequence, and not all of them.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testUndoRepeatedPaste()
    {
        EditorElement editor = this.editPage.getContentEditor();
        RichTextAreaElement textArea = editor.getRichTextArea();
        // Type text, select it (Shift+LeftArrow) and copy it (Control+C).
        // NOTE: We don't use Control+A to select the text because it selects also the BR element.
        textArea.sendKeys("q", Keys.chord(Keys.SHIFT, Keys.ARROW_LEFT), Keys.chord(Keys.CONTROL, "c"));
        // Then paste it 4 times (Control+V).
        for (int i = 0; i < 4; i++) {
            // Release the key after each paste so that the history records an entry for each paste. In case the paste
            // content is cleaned automatically, the editor cleans consecutive paste events (that happen one after
            // another) together and so a single history entry is recorded for such a group of paste events.
            textArea.sendKeys(Keys.chord(Keys.CONTROL, "v"));
        }
        // Undo the last paste.
        editor.getToolBar().clickUndoButton();
        Assert.assertEquals("qqq", textArea.getText());
    }

    /**
     * @see "XWIKI-4230: 'Tab' doesn't work in the Table Dialog in FF 3.5.2"
     */
    @Test
    public void testTabInTableConfigDialog()
    {
        TableConfigPane tableConfig = this.editPage.insertTable();

        // Assert that the row count input has the focus.
        Assert.assertEquals(tableConfig.getRowCountInput(), getDriver().switchTo().activeElement());
        getDriver().switchTo().defaultContent();

        // Press Tab to move the focus to the next input.
        tableConfig.getRowCountInput().sendKeys(Keys.TAB);

        // Assert that the column count input has the focus.
        Assert.assertEquals(tableConfig.getColumnCountInput(), getDriver().switchTo().activeElement());
        getDriver().switchTo().defaultContent();
    }

    /**
     * Test that hitting the . (dot) key at the end of a list item does not act as delete.
     * 
     * @see <a href="http://jira.xwiki.org/jira/browse/XWIKI-3304">XWIKI-3304</a>
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146")
    public void testDotAtEndDoesNotDelete()
    {
        EditorElement editor = this.editPage.getContentEditor();

        // Create a list with two items.
        editor.switchToSource();
        WebElement sourceTextArea = editor.getSourceTextArea();
        sourceTextArea.clear();
        sourceTextArea.sendKeys("* foo\n* bar");
        editor.switchToWysiwyg();

        // Place the caret at the end of the first item and type dot.
        RichTextAreaElement textArea = editor.getRichTextArea();
        textArea.sendKeys(Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, ".");

        Assert.assertEquals("foo.\nbar", textArea.getText());
    }

    /**
     * @see "XWIKI-3039: Changes are lost if an exception is thrown during saving"
     * @see "XWIKI-9750: User Input in WYSIWYG Editor is lost after conversion error"
     */
    @Test
    public void testRecoverAfterConversionException()
    {
        EditorElement editor = this.editPage.getContentEditor();

        // We removed the startwikilink comment to force a parsing failure.
        String html = "<span class=\"wikiexternallink\"><a href=\"mailto:x@y.z\">xyz</a></span><!--stopwikilink-->";
        editor.getRichTextArea().setContent(html);

        // Test to see if the HTML was accepted by the rich text area.
        Assert.assertEquals("xyz", editor.getRichTextArea().getText());

        // Save & Continue should notify us about the conversion error.
        this.editPage.clickSaveAndContinue(false);
        this.editPage.waitForNotificationErrorMessage("Failed to save the document. "
            + "Reason: content: Exception while parsing HTML");

        // Save & View should redirect us back to the edit mode, but the unsaved changes shouldn't be lost.
        this.editPage.clickSaveAndView();
        this.editPage = new WYSIWYGEditPage().waitUntilPageIsLoaded();
        Assert.assertEquals("xyz", this.editPage.getContentEditor().getRichTextArea().getText());
    }
}
