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
package org.xwiki.uiextension.test.ui;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.uiextension.internal.HelloWorldUIExtension;

import org.junit.Assert;

/**
 * UI tests for the UI Extension feature.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class UIExtensionTest extends AbstractTest
{
    private static final String HELLOWORLD_UIX_PAGE = "HelloWorld";

    private static final String HELLOWIKIWORLD_UIX_PAGE = "HelloWikiWorld";

    @Before
    public void setUp()
    {
        // Login as superadmin to have delete rights.
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();

        // Delete pages that we create in the test
        getUtil().deletePage(getTestClassName(), HELLOWORLD_UIX_PAGE);
        getUtil().deletePage(getTestClassName(), HELLOWIKIWORLD_UIX_PAGE);

        getUtil().gotoPage("Main", "WebHome");
    }

    @Test
    public void testUIExtension()
    {
        // Test Java UI extensions
        final ViewPage testPage = getUtil().createPage(getTestClassName(), HELLOWORLD_UIX_PAGE, "{{velocity}}\n"
            + "\n"
            + "{{html}}\n"
            + "#foreach($uix in $services.uix.getExtensions('hello', {'sortByParameter' : 'HelloWorldKey'}))"
            + "$services.rendering.render($uix.execute(), "
            + "'xhtml/1.0')#end\n"
            + "{{/html}}\n"
            + "\n"
            + "#foreach($uix in $services.uix.getExtensions('hello', {'sortByParameter' : 'HelloWorldKey'}))\n"
            + "$uix.parameters\n"
            + "#end\n"
            + "{{/velocity}}\n", "Hello World");
        Assert.assertEquals("HelloWorld\n"
            + "{HelloWorldKey=HelloWorldValue}", testPage.getContent());

        // Test Wiki UI extension

        getUtil().addObject(getTestClassName(), HELLOWIKIWORLD_UIX_PAGE, "XWiki.UIExtensionClass",
            "name", "helloWikiWorld2",
            "extensionPointId", "hello",
            "content", "HelloWikiWorld2",
            "parameters", "HelloWikiWorldKey=zz2_$xcontext.user");
        getUtil().addObject(getTestClassName(), HELLOWIKIWORLD_UIX_PAGE, "XWiki.UIExtensionClass",
            "name", "helloWikiWorld1",
            "extensionPointId", "hello",
            "content", "HelloWikiWorld1",
            "parameters", "HelloWikiWorldKey=zz1_$xcontext.user");
        getUtil().gotoPage(getTestClassName(), HELLOWORLD_UIX_PAGE);
        Assert.assertEquals("HelloWorld\n"
            + "HelloWikiWorld1\n"
            + "HelloWikiWorld2\n"
            + "{HelloWorldKey=HelloWorldValue}\n"
            + "{HelloWikiWorldKey=zz1_XWiki.superadmin}\n"
            + "{HelloWikiWorldKey=zz2_XWiki.superadmin}", testPage.getContent());
    }
}
