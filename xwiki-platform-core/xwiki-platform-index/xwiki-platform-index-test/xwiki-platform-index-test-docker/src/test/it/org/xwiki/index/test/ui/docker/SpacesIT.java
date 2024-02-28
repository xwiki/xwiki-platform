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
package org.xwiki.index.test.ui.docker;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.test.po.SpaceIndexPage;
import org.xwiki.index.test.po.SpacesMacroPage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the Spaces Macro.
 *
 * @version $Id$
 * @since 7.0RC1
 */
@UITest
class SpacesIT
{
    @Test
    @Order(1)
    void spacesMacro(TestUtils setup, TestReference testReference)
    {
        // Create a page with special characters in space name
        // See XE-1228: Broken links displayed in the Spaces widget if a space name contains a colon
        // See XE-1298: Spaces macro doesn't list spaces that contain a colon in their name
        String spaceName = setup.serializeReference(testReference.getLastSpaceReference()) + "&";
        String referenceEscapedSpaceName = spaceName.replaceAll("\\.", "\\\\.").replaceAll(":", "\\\\:");
        // Make sure the new space's WebHome page doesn't exist.
        setup.deletePage(spaceName, "WebHome");

        // Create the new space using the UI and verify it leads to the space home page being edited.
        SpacesMacroPage macroPage = SpacesMacroPage.gotoPage();
        macroPage.getSpacesMacroPane().createSpace(spaceName).clickCreate();
        WikiEditPage editPage = new WikiEditPage();

        // Verify that space creation uses the space name as the space home page's title
        assertEquals(spaceName, editPage.getDocumentTitle());

        // Verify that the space created is correct by looking at the generated metadata in the HTML header
        // (they contain the space reference amongst other data).
        // Note: the value will be escaped since it is the space reference, not the space name.
        assertEquals(referenceEscapedSpaceName, editPage.getMetaDataValue("space"));

        // Go back to the Spaces Macro page and verify that the link to space index works
        // First, save the space's home page
        editPage.clickSaveAndContinue();

        macroPage = SpacesMacroPage.gotoPage();
        macroPage.getSpacesMacroPane().clickSpaceIndex(referenceEscapedSpaceName);

        // Assert the content of the space index live table.
        LiveDataElement spaceIndexLiveTable = new SpaceIndexPage().getLiveData();
        assertEquals(1, spaceIndexLiveTable.getTableLayout().countRows());
        spaceIndexLiveTable.getTableLayout().assertRow("Page", "WebHome");
        spaceIndexLiveTable.getTableLayout().assertRow("Space", referenceEscapedSpaceName);

        // Go back to the Spaces Macro page and this time verify that the link to the space home page works
        macroPage = SpacesMacroPage.gotoPage();
        ViewPage spaceHomePage = macroPage.getSpacesMacroPane().clickSpaceHome(referenceEscapedSpaceName);
        assertEquals(spaceName, spaceHomePage.getDocumentTitle());
    }
}
