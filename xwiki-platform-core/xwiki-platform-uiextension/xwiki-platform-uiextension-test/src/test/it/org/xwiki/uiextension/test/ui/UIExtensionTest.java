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
import org.xwiki.test.ui.po.DocExtraPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;

import com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin;

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

    @Test
    public void testDocExtraUIExtension() throws Exception
    {
        Page extensionPage = getUtil().rest().page(new LocalDocumentReference(getTestClassName(), "DocExtra"));
        extensionPage.setContent(
            "{{velocity}}$xwiki.jsx.use($doc.getDocumentReference(), {'parameter': 'My Custom Value'}){{/velocity}}");

        Objects objects = new Objects();
        extensionPage.setObjects(objects);

        // Create a JavaScript extension that adds content to the page depending on the parameter. This allows seeing
        // if the JavaScript extension is loaded again by the DocExtra UI extension.
        Object jsxObject = RestTestUtils.object(JsSkinExtensionPlugin.JSX_CLASS_NAME);
        jsxObject.setNumber(0);
        jsxObject.withProperties(RestTestUtils.property("name", "DocContent"));
        jsxObject.withProperties(RestTestUtils.property("code", """
            function doSomething() {
              document.getElementById('xwikicontent')?.append(
                $jsontool.serialize("Parameter value: $!request.parameter")
              );
            }
            if (document.readyState === 'loading') {
              // Loading hasn't finished yet
              document.addEventListener('DOMContentLoaded', doSomething);
            } else {
              // `DOMContentLoaded` has already fired
              doSomething();
            }
            """));
        jsxObject.withProperties(RestTestUtils.property("cache", "forbid"));
        jsxObject.withProperties(RestTestUtils.property("parse", "1"));
        jsxObject.withProperties(RestTestUtils.property("use", "currentPage"));
        objects.withObjectSummaries(jsxObject);

        // Create a Wiki UI extension that adds a DocExtra pane.
        Object extensionsObject = RestTestUtils.object(WikiUIExtensionConstants.CLASS_REFERENCE_STRING);
        extensionsObject.setNumber(0);
        extensionsObject.withProperties(RestTestUtils.property("name", "DocExtra"));
        extensionsObject.withProperties(
            RestTestUtils.property("extensionPointId", "org.xwiki.plaftorm.template.docextra"));
        extensionsObject.withProperties(RestTestUtils.property("content", """
            {{velocity output="false"}}
            ## Load the OnDemandJS extension to verify that the UI extension can load JSX.
            $xwiki.jsx.use('%s.OnDemandJS')
            {{/velocity}}
            
            Doc extra content
            """.formatted(getTestClassName())));
        // Construct parameters
        String parameters = """
            show=true
            title=HelloDocExtra
            itemnumber=-1
            name=DocExtra
            shortcut=alt+k
            order=1
            """;
        extensionsObject.withProperties(RestTestUtils.property("parameters", parameters));
        objects.withObjectSummaries(extensionsObject);

        getUtil().rest().save(extensionPage);

        // Create a second page with a pure on-demand JavaScript extension that adds a different content to the page.
        Page onDemandJSPage =
            getUtil().rest().page(new LocalDocumentReference(getTestClassName(), "OnDemandJS"));
        Objects onDemandJSObjects = new Objects();
        onDemandJSPage.setObjects(onDemandJSObjects);
        Object onDemandJSObject = RestTestUtils.object(JsSkinExtensionPlugin.JSX_CLASS_NAME);
        onDemandJSObject.setNumber(0);
        onDemandJSObject.withProperties(RestTestUtils.property("name", "OnDemandJS"));
        onDemandJSObject.withProperties(RestTestUtils.property("code",
            "document.getElementById('xwikicontent')?.append('OnDemandJS');"));
        onDemandJSObject.withProperties(RestTestUtils.property("cache", "long"));
        onDemandJSObject.withProperties(RestTestUtils.property("parse", "0"));
        onDemandJSObject.withProperties(RestTestUtils.property("use", "onDemand"));
        onDemandJSObjects.withObjectSummaries(onDemandJSObject);
        getUtil().rest().save(onDemandJSPage);

        ViewPage page = getUtil().gotoPage(getTestClassName(), "DocExtra");
        // Verify that the JavaScript extension is loaded.
        String expectedPageContent = "Parameter value: My Custom Value";
        Assert.assertEquals(expectedPageContent, page.getContent().trim());
        Assert.assertTrue(page.hasDocExtraPane("DocExtra"));
        DocExtraPane docExtra = page.openDocExtraPane("DocExtra");
        Assert.assertEquals("Doc extra content", docExtra.getText());
        // Verify that the page content is not modified by the "on this page" JavaScript extension but is modified by
        // the JavaScript extension loaded by the DocExtra UI extension.
        String expectedContent = expectedPageContent + "OnDemandJS";
        // Wait for the expected content to be added by the JavaScript extension loaded by the DocExtra UI extension.
        page.waitUntilContent(expectedContent);
        Assert.assertEquals(expectedContent, page.getContent().trim());
    }
}
