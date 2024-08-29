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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;

/**
 * Tests the applications panel entry. This test needs its own class because it needs to be in a separated space in the
 * wiki. In the other test classes we create one application per method, in the same space, which leads to duplicate
 * entries in the panel.
 * 
 * @version $Id$
 * @since 4.3RC1
 */
public class ApplicationsPanelEntryTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * The page being tested.
     */
    private ApplicationHomeEditPage editPage;

    /**
     * The query string parameters passed to the edit action.
     */
    private final Map<String, String> editQueryStringParameters = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());
        editQueryStringParameters.put("editor", "inline");
        editQueryStringParameters.put("template", "AppWithinMinutes.LiveTableTemplate");
        editQueryStringParameters.put("AppWithinMinutes.LiveTableClass_0_class", "XWiki.XWikiUsers");
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit", editQueryStringParameters);
        editPage = new ApplicationHomeEditPage();
    }

    @Test
    public void testApplicationPanelEntry()
    {
        String appTitle = "Applications Panel Entry Test";

        // Test the title and the icon remain the same between edits
        editPage.setTitle(appTitle);
        editPage.setIcon("icon:bell");
        editPage.clickSaveAndView();

        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit");
        assertEquals(appTitle, editPage.getTitle());
        assertEquals("icon:bell", editPage.getIcon());

        ApplicationsPanel panel = ApplicationsPanel.gotoPage();
        ViewPage page = panel.clickApplication(appTitle);
        // Verify we're on the right page!
        assertEquals(getTestClassName(), page.getMetaDataValue("space"));
        assertEquals(getTestMethodName(), page.getMetaDataValue("page"));
    }
}
