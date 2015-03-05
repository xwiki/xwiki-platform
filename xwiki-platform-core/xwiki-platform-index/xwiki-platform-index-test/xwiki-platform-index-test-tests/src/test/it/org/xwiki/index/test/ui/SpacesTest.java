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
package org.xwiki.index.test.ui;

import java.lang.String;
import org.junit.Test;
import org.xwiki.index.test.po.SpaceIndexPage;
import org.xwiki.index.test.po.SpacesMacroPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.Assert.*;

/**
 * Tests the Spaces Macro.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public class SpacesTest extends AbstractTest
{
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void spacesMacro()
    {
        // Create a page with special characters in space name
        // See XE-1228: Broken links displayed in the Spaces widget if a space name contains a colon
        // See XE-1298: Spaces macro doesn't list spaces that contain a colon in their name
        String spaceName = getTestClassName() + ":" + getTestMethodName() + "&";
        // Make sure the new space's WebHome page doesn't exist.
        getUtil().deletePage(spaceName, "WebHome");

        // Create the new space using the UI and verify it leads to the page being edited in WYSIWYG mode
        SpacesMacroPage macroPage = SpacesMacroPage.gotoPage();
        WYSIWYGEditPage editPage = macroPage.getSpacesMacroPane().createSpace(spaceName);

        // Verify that space creation uses the space name as the space home page's title
        assertEquals(spaceName, editPage.getDocumentTitle());

        // Verify that the space created is correct by looking at the generate metadata in the HTML header
        // (they contain the space name amongst other data).
        assertEquals(spaceName, editPage.getMetaDataValue("space"));

        // Go back to the Spaces Macro page and verify that the link to space index works
        // First, save the space's home page
        editPage.clickSaveAndContinue();

        macroPage = SpacesMacroPage.gotoPage();
        macroPage.getSpacesMacroPane().clickSpaceIndex(spaceName);

        // Assert the content of the space index live table.
        LiveTableElement spaceIndexLiveTable = new SpaceIndexPage().getLiveTable();
        spaceIndexLiveTable.waitUntilReady();
        assertEquals(1, spaceIndexLiveTable.getRowCount());
        assertTrue(spaceIndexLiveTable.hasRow("Page", "WebHome"));
        assertTrue(spaceIndexLiveTable.hasRow("Space", spaceName));

        // Go back to the Spaces Macro page and this time verify that the link to the space home page works
        macroPage = SpacesMacroPage.gotoPage();
        ViewPage spaceHomePage = macroPage.getSpacesMacroPane().clickSpaceHome(spaceName);
        assertEquals(spaceName, spaceHomePage.getDocumentTitle());
    }
}
