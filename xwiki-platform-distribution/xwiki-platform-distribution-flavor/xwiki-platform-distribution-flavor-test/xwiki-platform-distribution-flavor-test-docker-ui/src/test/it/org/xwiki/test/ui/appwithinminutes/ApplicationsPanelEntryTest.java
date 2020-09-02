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
package org.xwiki.test.ui.appwithinminutes;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
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
 * @since 12.8RC1
 */
@UITest
public class ApplicationsPanelEntryTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * The page being tested.
     */
    private ApplicationHomeEditPage editPage;

    /**
     * The query string parameters passed to the edit action.
     */
    private final Map<String, String> editQueryStringParameters = new HashMap<>();

    @BeforeAll
    void beforeAll(TestUtils testUtils, TestReference testReference)
    {
        testUtils.deletePage(testReference);
        this.editQueryStringParameters.put("editor", "inline");
        this.editQueryStringParameters.put("template", "AppWithinMinutes.LiveTableTemplate");
        this.editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers");
        testUtils.gotoPage(testReference, "edit", this.editQueryStringParameters);
        this.editPage = new ApplicationHomeEditPage().waitUntilPageIsLoaded();
    }

    @Test
    @Order(1)
    void testApplicationPanelEntry(TestUtils testUtils, TestReference testReference)
    {
        String appTitle = "Applications Panel Entry Test";

        // Test the title and the icon remain the same between edits
        this.editPage.setTitle(appTitle);
        this.editPage.setIcon("icon:bell");
        this.editPage.clickSaveAndView();

        testUtils.gotoPage(testReference, "edit");
        assertEquals(appTitle, this.editPage.getTitle());
        assertEquals("icon:bell", this.editPage.getIcon());

        ApplicationsPanel panel = ApplicationsPanel.gotoPage();
        ViewPage page = panel.clickApplication(appTitle);
        // Verify we're on the right page!
        assertEquals(testReference.getLastSpaceReference().getName(), page.getMetaDataValue("space"));
        assertEquals(testReference.getName(), page.getMetaDataValue("page"));
    }
}
