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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test template handling.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class TemplateTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(testUtils);

    /**
     * Test that velocity is rendered
     */
    @Test
    @Order(1)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testHelloVelocity(TestUtils testUtils)
    {
        String hello = "Hello Velocity Test Test Test";
        saveVelocity(hello);
        assertTrue(testUtils.getDriver().findElement(By.id("xwikicontent")).getText().contains(hello));
    }

    /**
     * Test that an included existing template is displayed correctly
     */
    @Test
    @Order(2)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testCorrectTemplate(TestUtils testUtils)
    {
        saveVelocity(includeTemplate("code.vm"), true);
        assertNotNull(testUtils.getDriver().findElement(By.xpath("//div[@id='xwikicontent']//.[@class='wiki-code']")));
    }

    /**
     * See XWIKI-2580
     */
    @Test
    @Order(3)
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    void testWrongTemplate(TestUtils testUtils)
    {
        saveVelocity(includeTemplate("../../"));
        assertEquals(0, testUtils.getDriver().findElement(By.id("xwikicontent")).getText().length(), "root directory");

        saveVelocity(includeTemplate("asdfasdf"));
        assertEquals(0, testUtils.getDriver().findElement(By.id("xwikicontent")).getText().length(),
            "not existing template");

        saveVelocity(includeTemplate("../redirect"));
        assertEquals(0, testUtils.getDriver().findElement(By.id("xwikicontent")).getText().length(),
            "file in the parent directory");

        saveVelocity(includeTemplate("../WEB-INF/version.properties"));
        assertEquals(0, testUtils.getDriver().findElement(By.id("xwikicontent")).getText().length(),
            "file in the wrong directory");

        saveVelocity(includeTemplate("/chw/../../WEB-INF/../WEB-INF/lib/../version.properties"));
        assertEquals(0, testUtils.getDriver().findElement(By.id("xwikicontent")).getText().length(),
            "file in the wrong directory, not normalized path");


        // TODO: how to validate console with docker tests?
        // this.validateConsole.getLogCaptureConfiguration().registerExpected("Possible break-in attempt!",
        //     "Error getting resource [null]");
    }

    /**
     * @see #saveVelocity(String, boolean)
     */
    private void saveVelocity(String code)
    {
        saveVelocity(code, false);
    }

    /**
     * Save a page with given velocity code and switch to view. Encloses <code>code</code> into the {{velocity}} macro
     * and optionally also {{html}} macro.
     *
     * @param code velocity code to save
     * @param html additionally enclose <code>code</code> in {{html}} if true
     */
    private void saveVelocity(String code, boolean html)
    {
        /* Page used for testing: Main.TemplateTest */
        WikiEditPage editPage = WikiEditPage.gotoPage("Main", "TemplateTest");
        if (html) {
            code = "{{html wiki=\"false\"}}\n" + code + "\n{{/html}}";
        }
        editPage.setContent("{{velocity filter=\"none\"}}\n" + code + "\n{{/velocity}}\n");
        editPage.clickSaveAndView();
    }

    /**
     * Return velocity code to include a velocity template <code>name</code>
     *
     * @param name template to use
     */
    private String includeTemplate(String name)
    {
        return "#template(\"" + name + "\")\n";
    }
}
