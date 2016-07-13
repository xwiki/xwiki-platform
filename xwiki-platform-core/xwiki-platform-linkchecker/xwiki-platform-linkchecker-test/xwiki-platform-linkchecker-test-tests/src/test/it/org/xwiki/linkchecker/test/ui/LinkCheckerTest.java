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
package org.xwiki.linkchecker.test.ui;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.linkchecker.test.po.LinkCheckerAllDocsPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * UI tests for the Link Checker feature.
 *
 * @version $Id$
 * @since 3.4M1
 */
public class LinkCheckerTest extends AbstractTest
{
    @Test
    public void testLinkChecker()
    {
        getUtil().deletePage(getClass().getSimpleName(), getTestMethodName());

        // Create a page with a URL so that it appears in the Link Checker livetable.
        getUtil().createPage(getClass().getSimpleName(), getTestMethodName(), "http://doesntexist", getTestClassName());

        // Navigate to the Index page and click on the "External Links" tab
        final LinkCheckerAllDocsPage page = LinkCheckerAllDocsPage.gotoPage();

        // Since the LinkChecker works asynchronously there's small possibility that the link hasn't been added
        // before the livetable displays, thus we wait till we get the link state.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                LiveTableElement livetable = page.clickLinkCheckerTab();
                if (livetable.hasRow("Link", "http://doesntexist")) {
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
}
