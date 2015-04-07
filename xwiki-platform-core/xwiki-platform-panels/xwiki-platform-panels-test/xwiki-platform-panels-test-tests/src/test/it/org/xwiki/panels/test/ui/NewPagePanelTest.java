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
package org.xwiki.panels.test.ui;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.panels.test.po.NewPagePanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

/**
 * Test page creation using the NewPage Panel.
 * 
 * @version $Id$
 * @since 2.5RC1
 */
public class NewPagePanelTest extends AbstractTest
{
    @Rule
    public AuthenticationRule authenticationRule = new AuthenticationRule("TestUser", "TestPassword", getUtil());

    /**
     * Tests if a new page can be created using the create page panel.
     */
    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testCreatePageFromPanel()
    {
        NewPagePanel newPagePanel = NewPagePanel.gotoPage();

        WYSIWYGEditPage editPage = newPagePanel.createPage(getTestClassName(), getTestMethodName());

        Assert.assertEquals(getTestMethodName(), editPage.getDocumentTitle());
        Assert.assertEquals(getTestMethodName(), editPage.getMetaDataValue("page"));
        Assert.assertEquals(getTestClassName(), editPage.getMetaDataValue("space"));
    }
}
