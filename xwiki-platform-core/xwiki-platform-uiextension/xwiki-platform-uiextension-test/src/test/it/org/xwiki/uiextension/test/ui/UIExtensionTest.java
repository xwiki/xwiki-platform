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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.TestUtils.RestTestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;

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

    // Login as superadmin to have delete rights.
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Before
    public void setUp() throws Exception
    {
        // Delete pages that we create in the test
        getUtil().rest().delete(new LocalDocumentReference(getTestClassName(), HELLOWORLD_UIX_PAGE));
        getUtil().rest().delete(new LocalDocumentReference(getTestClassName(), HELLOWIKIWORLD_UIX_PAGE));
    }

    @Test
    public void testUIExtension() throws Exception
    {
        // Test Java UI extensions
        getUtil().rest().savePage(new LocalDocumentReference(getTestClassName(), HELLOWORLD_UIX_PAGE),
            "{{velocity}}\n"
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
            + "{{/velocity}}\n", "");

        // Test Wiki UI extension

        Page extensionsPage =
            getUtil().rest().page(new LocalDocumentReference(getTestClassName(), HELLOWIKIWORLD_UIX_PAGE));
        Objects objects = new Objects();
        extensionsPage.setObjects(objects);
        Object object = RestTestUtils.object(WikiUIExtensionConstants.CLASS_REFERENCE_STRING);
        object.setNumber(0);
        object.withProperties(RestTestUtils.property("name", "helloWikiWorld2"));
        object.withProperties(RestTestUtils.property("extensionPointId", "hello"));
        object.withProperties(RestTestUtils.property("content", "HelloWikiWorld2"));
        object.withProperties(RestTestUtils.property("parameters", "HelloWorldKey=zz2_$xcontext.user"));
        objects.withObjectSummaries(object);
        object = RestTestUtils.object(WikiUIExtensionConstants.CLASS_REFERENCE_STRING);
        object.setNumber(1);
        object.withProperties(RestTestUtils.property("name", "helloWikiWorld1"));
        object.withProperties(RestTestUtils.property("extensionPointId", "hello"));
        object.withProperties(RestTestUtils.property("content", "HelloWikiWorld1"));
        object.withProperties(RestTestUtils.property("parameters", "HelloWorldKey=zz1_$xcontext.user"));
        objects.withObjectSummaries(object);
        getUtil().rest().save(extensionsPage);

        ViewPage page = getUtil().gotoPage(getTestClassName(), HELLOWORLD_UIX_PAGE);
        Assert.assertEquals("HelloWorld\n"
            + "HelloWikiWorld1\n"
            + "HelloWikiWorld2\n"
            + "{HelloWorldKey=HelloWorldValue}\n"
            + "{HelloWorldKey=zz1_XWiki.superadmin}\n"
            + "{HelloWorldKey=zz2_XWiki.superadmin}", page.getContent());
    }
}
