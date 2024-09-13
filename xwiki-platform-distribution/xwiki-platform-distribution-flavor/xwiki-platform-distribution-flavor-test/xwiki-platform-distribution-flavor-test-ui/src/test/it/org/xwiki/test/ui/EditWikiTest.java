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

import org.junit.Assert;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage.Editor;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Test wiki editing.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class EditWikiTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * Page used for testing.
     */
    private WikiEditPage editPage;

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        this.editPage = WikiEditPage.gotoPage(getTestClassName(), getTestMethodName());
    }

    /** Test that save and continue saves as a minor version. */
    @Test
    public void testSaveAndContinueSavesAsMinorEdit()
    {

        Assert.assertTrue(this.editPage.isNewDocument());
        this.editPage.setContent("abc1");
        this.editPage.clickSaveAndView();
        Assert.assertEquals("1.1", this.editPage.getMetaDataValue("version"));

        this.editPage = WikiEditPage.gotoPage(getTestClassName(), getTestMethodName());
        Assert.assertFalse(this.editPage.isNewDocument());
        this.editPage.setContent("abc2");
        this.editPage.setMinorEdit(false);
        this.editPage.clickSaveAndContinue();
        this.editPage.clickCancel();
        Assert.assertEquals("1.2", this.editPage.getMetaDataValue("version"));
    }

    /**
     * Tests that the warning about loosing some of the page content when switching to the WYSIWYG editor is not
     * displayed if the page syntax is xwiki/2.0.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testSwitchToWysiwygWithAdvancedContent()
    {
        // Place some HTML in the page content.
        this.editPage.setContent("{{html}}<hr/>{{/html}}");
        // If we are asked to confirm the editor switch then we choose to remain on the wiki editor.
        getDriver().makeConfirmDialogSilent(false);
        // Switch to WYSIWYG editor.
        WYSIWYGEditPage wysiwygEditPage = this.editPage.editWYSIWYG();
        // Check that we are indeed in WYSIWYG edit mode.
        Assert.assertEquals(Editor.WYSIWYG, wysiwygEditPage.getEditor());
    }

    /**
     * @see XWIKI-6934: Preview action doesn't displays the page's title
     */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146")
    public void testPreviewDisplaysPageTitle()
    {
        String title = insecure().nextAlphanumeric(3);
        this.editPage.setTitle(title);
        this.editPage.clickPreview();
        // The preview page has the action buttons but otherwise it is similar to a view page.
        Assert.assertEquals(title, new ViewPage().getDocumentTitle());
    }
}
