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
package org.xwiki.webjars.test.ui;

import java.net.URI;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the WebJars integration.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@UITest
class WebJarsIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    void webJars(TestUtils setup) throws Exception
    {
        // Delete pages that we create in the test.
        setup.rest().deletePage("WebJarsIT", "webJars");

        // Create a page in which we install a webjar in a wiki namespace, generate a URL to a
        // resource inside it, and link to it. Then follow the link and verify the served content.
        String content = "{{velocity}}\n"
            + "#set ($job = $services.extension.install('org.webjars:AjaxQ', '0.0.2', 'wiki:mywiki'))\n"
            + "#set ($discard = $job.join())\n"
            + "installed: $services.extension.installed.getInstalledExtension('org.webjars:AjaxQ', 'wiki:mywiki').id\n"
            + "[[AjaxQ>>path:$services.webjars.url('org.webjars:AjaxQ', 'ajaxq.js', {'wiki' : 'mywiki'})]]\n"
            + "{{/velocity}}";
        ViewPage vp = setup.createPage("WebJarsIT", "webJars", content, "WebJars Test");

        assertTrue(vp.getContent().contains("installed: org.webjars:AjaxQ/0.0.2"));

        // Click the link! (For why there's a r=1 query string, see WebJarsScriptService#getResourceReference().)
        WebElement link = setup.getDriver().findElementWithoutWaiting(
            By.xpath("//a[@href = '/xwiki/webjars/wiki%3Amywiki/AjaxQ/0.0.2/ajaxq.js?r=1']"));
        link.click();

        assertTrue(setup.getDriver().getPageSource().contains("// AjaxQ jQuery Plugin"));
    }

    @Test
    void pathTraversal(TestUtils setup) throws Exception
    {
        URI uri = new URI(Strings.CS.removeEnd(setup.rest().getBaseURL(), "rest")
            + "webjars/wiki%3Axwiki/..%2F..%2F..%2F..%2F..%2FWEB-INF%2Fxwiki.cfg");

        GetMethod response = setup.rest().executeGet(uri);

        assertNotEquals(200, response.getStatusCode());

        response.releaseConnection();
    }
}
