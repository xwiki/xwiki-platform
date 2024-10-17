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

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the WebJars integration.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class WebJarsTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void testWebJars() throws Exception
    {
        // Delete pages that we create in the test
        getUtil().rest().deletePage(getTestClassName(), getTestMethodName());

        // Create a page in which:
        // - we install a webjar in the main wiki
        // - we use the WebJars Script Service to generate a URL for a resource inside that webjar
        // - we create a link to that resource
        // Then we click that link and verify we get the resource content.
        //
        // This test validates both the ability to generate webjars URL, the serving of webjars through the webjars URL
        // and the ability to access a webjars installed in a specific wiki.
        String content = "{{velocity}}\n"
            + "#set ($job = $services.extension.install('org.webjars:AjaxQ', '0.0.2', 'wiki:mywiki'))\n"
            + "#set ($discard = $job.join())\n"
            + "installed: $services.extension.installed.getInstalledExtension('org.webjars:AjaxQ', 'wiki:mywiki').id\n"
            + "[[AjaxQ>>path:$services.webjars.url('org.webjars:AjaxQ', 'ajaxq.js', {'wiki' : 'mywiki'})]]\n"
            + "{{/velocity}}";
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), content, "WebJars Test");

        assertTrue(vp.getContent().contains("installed: org.webjars:AjaxQ/0.0.2"));

        // Click the link!
        // Note: For understanding why there's a r=1 query string, see WebJarsScriptService#getResourceReference()
        WebElement link = getDriver().findElementWithoutWaiting(
            By.xpath("//a[@href = '/xwiki/webjars/wiki%3Amywiki/AjaxQ/0.0.2/ajaxq.js?r=1']"));
        link.click();

        // Verify that the served resource is the one from the webjars
        assertTrue(getDriver().getPageSource().contains("// AjaxQ jQuery Plugin"));
    }

    @Test
    public void pathTraversal() throws Exception
    {
        URI uri = new URI(StringUtils.removeEnd(getUtil().rest().getBaseURL(), "rest")
            + "webjars/wiki%3Axwiki/..%2F..%2F..%2F..%2F..%2FWEB-INF%2Fxwiki.cfg");

        try (CloseableHttpResponse response = getUtil().rest().executeGet(uri)) {
            assertNotEquals(200, response.getCode());
        }
    }
}
