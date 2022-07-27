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

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xwiki.test.po.xe.HomePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Test Skin Extensions.
 * 
 * @version $Id$
 * @since 4.1
 */
public class SkinxTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    private static final String SCRIPT = "window.document.title = 'script active';";

    @Before
    public void setUp() throws Exception
    {
        getUtil().rest().deletePage("Test", "SkinxTest");
    }

    /** https://jira.xwiki.org/browse/XWIKI-7913 */
    @Test
    public void testJavascriptExtension()
    {
        // Create a doc
        WikiEditPage wep = WikiEditPage.gotoPage("Test", "SkinxTest");
        wep.setContent("this is the content");
        ViewPage vp = wep.clickSaveAndView();

        // Add an XWikiGroups object
        ObjectEditPage oep = vp.editObjects();
        ObjectEditPane objectForm = oep.addObject("XWiki.JavaScriptExtension");
        objectForm.setFieldValue(By.id("XWiki.JavaScriptExtension_0_code"), SCRIPT);
        objectForm.getSelectElement(By.id("XWiki.JavaScriptExtension_0_use")).select("always");
        oep.clickSaveAndView();
        waitForScriptResult();
        HomePage.gotoPage();
        waitForScriptResult();

        oep = ObjectEditPage.gotoPage("Test", "SkinxTest");
        objectForm = oep.getObjectsOfClass("XWiki.JavaScriptExtension").get(0);
        objectForm.displayObject();
        objectForm.getSelectElement(By.id("XWiki.JavaScriptExtension_0_use")).select("currentPage");
        oep.clickSaveAndView();
        waitForScriptResult();
        HomePage.gotoPage();
        try {
            waitForScriptResult();
            Assert.fail("The JSX should be active only on the current page.");
        } catch (TimeoutException e) {
        }

        oep = ObjectEditPage.gotoPage("Test", "SkinxTest");
        objectForm = oep.getObjectsOfClass("XWiki.JavaScriptExtension").get(0);
        objectForm.displayObject();
        objectForm.getSelectElement(By.id("XWiki.JavaScriptExtension_0_use")).select("onDemand");
        oep.clickSaveAndView();
        try {
            waitForScriptResult();
            Assert.fail("The JSX should be active only on demand.");
        } catch (TimeoutException e) {
        }
    }

    /**
     * We need to wait for the script result, especially after clicking Save & View (looks like Selenium is not always
     * waiting for the scripts to be loaded).
     */
    private void waitForScriptResult()
    {
        new WebDriverWait(getDriver(), Duration.ofSeconds(getDriver().getTimeout())).until(
            (ExpectedCondition<Boolean>) driver -> StringUtils.equals("script active", driver.getTitle()));
    }
}
