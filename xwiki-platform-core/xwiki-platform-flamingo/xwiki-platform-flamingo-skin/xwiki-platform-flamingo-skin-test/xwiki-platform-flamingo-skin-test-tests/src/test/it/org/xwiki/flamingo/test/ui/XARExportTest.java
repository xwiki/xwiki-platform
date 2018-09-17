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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.xwiki.flamingo.sking.test.po.ExportModal;
import org.xwiki.flamingo.sking.test.po.OtherFormatView;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.LogLevel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verify that the XAR export features works fine.
 *
 * @version $Id$
 */
public class XARExportTest extends AbstractTest
{
    @Test
    public void exportXARAfterOpeningTreeAndUnselectedPages() throws Exception
    {
        getUtil().rest().deletePage("Foo", "WebHome");
        getUtil().loginAsAdmin();

        ViewPage viewPage = getUtil().createPage("Foo", "WebHome", "Foo", "Foo");
        viewPage.clickMoreActionsSubMenuEntry("tmExport");

        ExportModal exportModal = new ExportModal();
        //assertTrue(exportModal.isDisplayed());

        OtherFormatView otherFormatView = exportModal.openOtherFormatView();
        assertTrue(otherFormatView.isTreeAvailable());
        assertTrue(otherFormatView.isExportAsXARButtonAvailable());

        TreeElement treeElement = otherFormatView.getTreeElement();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();
        assertEquals(1, topLevelNodes.size());

        assertTrue(topLevelNodes.get(0).getChildren().isEmpty());
        otherFormatView.clickExportAsXARButton();

        assertEquals(getUtil().getURL("Foo", "WebHome", "export", "format=xar&name=Foo.WebHome"), otherFormatView.getForm().getAttribute("action"));
        assertEquals("Foo.WebHome", otherFormatView.getCheckedPagesField().getAttribute("value"));
        assertTrue(otherFormatView.getUncheckedPagesField().getAttribute("value").isEmpty());
        assertEquals("false", otherFormatView.getOtherPagesField().getAttribute("value"));
    }


}
