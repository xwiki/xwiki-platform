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
package org.xwiki.appwithinminutes.test.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the applications panel entry. This test needs its own class because it needs to be in a separated space in the
 * wiki. In the other test classes we create one application per method, in the same space, which leads to duplicate
 * entries in the panel.
 *
 * @version $Id$
 * @since 4.3RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class ApplicationsPanelEntryIT
{
    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference, true);
    }

    @Test
    @Order(1)
    void applicationPanelEntry(TestUtils setup, TestReference testReference)
    {
        setup.gotoPage(testReference, "edit", "editor", "inline",
            "template", "AppWithinMinutes.LiveTableTemplate",
            "AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers");
        ApplicationHomeEditPage editPage = new ApplicationHomeEditPage();

        String appTitle = "Applications Panel Entry Test";

        // Test the title and the icon remain the same between edits.
        editPage.setTitle(appTitle);
        editPage.setIcon("icon:bell");
        editPage.clickSaveAndView();

        setup.gotoPage(testReference, "edit");
        editPage = new ApplicationHomeEditPage();
        assertEquals(appTitle, editPage.getTitle());
        assertEquals("icon:bell", editPage.getIcon());

        ApplicationsPanel panel = ApplicationsPanel.gotoPage();
        ViewPage page = panel.clickApplication(appTitle);
        // Verify we're on the right page!
        assertEquals(setup.serializeLocalReference(testReference.getLastSpaceReference()),
            page.getMetaDataValue("space"));
        assertEquals(testReference.getName(), page.getMetaDataValue("page"));
    }
}
