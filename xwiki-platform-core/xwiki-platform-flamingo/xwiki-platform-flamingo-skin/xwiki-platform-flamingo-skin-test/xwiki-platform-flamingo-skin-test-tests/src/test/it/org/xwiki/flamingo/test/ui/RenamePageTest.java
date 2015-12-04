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

package org.xwiki.flamingo.test.ui;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import java.util.Arrays;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;

public class RenamePageTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    @Before
    public void initialize() throws Exception
    {
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2.3.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("A.B.2.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("A.B.2.3.WebHome"));

        getUtil().createPage(Arrays.asList("1", "2"), "WebHome", "", "");
        getUtil().createPage(Arrays.asList("1", "2", "3"), "WebHome", "", "");

        getUtil().gotoPage(Arrays.asList("1", "2"), "WebHome", "", "");
    }

    @Test
    public void convertNestedPageToTerminalPage() throws Exception
    {
        ViewPage vp = new ViewPage();

        RenamePage renamePage = vp.rename();
        renamePage.setAutoRedirect(false);
        renamePage.setTerminal(true);
        renamePage.clickRenameButton();

        assertTrue("Page 1.2 doesn't exist!", getUtil().pageExists(Arrays.asList("1"), "2"));
        assertFalse("Page 1.2.WebHome exists!", getUtil().pageExists(Arrays.asList("1", "2"), "WebHome"));
    }

    @Test
    public void movePageAndImpliedChildrenWithoutRedirect() throws Exception
    {
        ViewPage vp = new ViewPage();

        RenamePage renamePage = vp.rename();
        renamePage.setDeep(true);
        renamePage.clickLocationActionEditButton();
        renamePage.setTargetParentReference("A.B");
        renamePage.setAutoRedirect(false);
        renamePage.clickRenameButton();

        assertTrue("Page A.B.2.WebHome doesn't exist!", getUtil().pageExists(Arrays.asList("A", "B", "2"), "WebHome"));
        assertTrue("Page A.B.2.3.WebHome doesn't exist!",
            getUtil().pageExists(Arrays.asList("A", "B", "2", "3"), "WebHome"));
        assertFalse("Page 1.2.WebHome exists!", getUtil().pageExists(Arrays.asList("1", "2"), "WebHome"));
        assertFalse("Page 1.2.3.WebHome exists!", getUtil().pageExists(Arrays.asList("1", "2", "3"), "WebHome"));
    }

    @Test
    public void movePageAndImpliedChildrenWithRedirect() throws Exception
    {
        ViewPage vp = new ViewPage();

        RenamePage renamePage = vp.rename();
        renamePage.setDeep(true);
        renamePage.clickLocationActionEditButton();
        renamePage.setTargetParentReference("A.B");
        renamePage.setAutoRedirect(true);
        renamePage.clickRenameButton();

        assertTrue("Page A.B.2.WebHome doesn't exist!", getUtil().pageExists(Arrays.asList("A", "B", "2"), "WebHome"));
        assertTrue("Page A.B.2.3.WebHome doesn't exist!",
            getUtil().pageExists(Arrays.asList("A", "B", "2", "3"), "WebHome"));
        getUtil().gotoPage(Arrays.asList("1", "2"), "WebHome", "view", "");
        assertEquals(vp.getBreadcrumbContent(), "/A/B/2");
        getUtil().gotoPage(Arrays.asList("1", "2", "3"), "WebHome", "view", "");
        assertEquals(vp.getBreadcrumbContent(), "/A/B/2/3");
    }
}
