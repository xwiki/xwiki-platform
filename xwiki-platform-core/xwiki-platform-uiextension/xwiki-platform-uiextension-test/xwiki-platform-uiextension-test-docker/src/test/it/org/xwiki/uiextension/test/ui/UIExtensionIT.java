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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.TestUtils.RestTestUtils;
import org.xwiki.test.ui.po.DocExtraPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;

import com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the UI Extension feature.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@UITest
class UIExtensionIT
{
    private static final String HELLOWORLD_UIX_PAGE = "HelloWorld";

    private static final String HELLOWIKIWORLD_UIX_PAGE = "HelloWikiWorld";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference) throws Exception
    {
        // Login as superadmin to have delete rights.
        setup.loginAsSuperAdmin();

        // Delete pages that we create in the test
        SpaceReference space = testReference.getLastSpaceReference();
        setup.rest().delete(new DocumentReference(HELLOWORLD_UIX_PAGE, space));
        setup.rest().delete(new DocumentReference(HELLOWIKIWORLD_UIX_PAGE, space));
    }

    @Test
    @Order(1)
    void testUIExtension(TestUtils setup, TestReference testReference) throws Exception
    {
        SpaceReference space = testReference.getLastSpaceReference();

        // Test Java UI extensions
        setup.rest().savePage(new DocumentReference(HELLOWORLD_UIX_PAGE, space),
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

        Page extensionsPage = setup.rest().page(new DocumentReference(HELLOWIKIWORLD_UIX_PAGE, space));
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
        setup.rest().save(extensionsPage);

        setup.gotoPage(new DocumentReference(HELLOWORLD_UIX_PAGE, space));
        ViewPage page = new ViewPage();
        assertEquals("HelloWorld\n"
            + "HelloWikiWorld1\n"
            + "HelloWikiWorld2\n"
            + "{HelloWorldKey=HelloWorldValue}\n"
            + "{HelloWorldKey=zz1_XWiki.superadmin}\n"
            + "{HelloWorldKey=zz2_XWiki.superadmin}", page.getContent());
    }

    @Test
    @Order(2)
    void testDocExtraUIExtension(TestUtils setup, TestReference testReference) throws Exception
    {
        SpaceReference space = testReference.getLastSpaceReference();

        Page extensionPage = setup.rest().page(new DocumentReference("DocExtra", space));
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
            ## Load the OnDemandJS extension to verify that the UI extension can load JSX. The OnDemandJS page is a
            ## sibling of the current page so it can be referenced by its name only.
            $xwiki.jsx.use('OnDemandJS')
            {{/velocity}}

            Doc extra content
            """));
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

        setup.rest().save(extensionPage);

        // Create a second page with a pure on-demand JavaScript extension that adds a different content to the page.
        Page onDemandJSPage = setup.rest().page(new DocumentReference("OnDemandJS", space));
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
        setup.rest().save(onDemandJSPage);

        setup.gotoPage(new DocumentReference("DocExtra", space));
        ViewPage page = new ViewPage();
        // Verify that the JavaScript extension is loaded.
        String expectedPageContent = "Parameter value: My Custom Value";
        assertEquals(expectedPageContent, page.getContent().trim());
        assertTrue(page.hasDocExtraPane("DocExtra"));
        DocExtraPane docExtra = page.openDocExtraPane("DocExtra");
        assertEquals("Doc extra content", docExtra.getText());
        // Verify that the page content is not modified by the "on this page" JavaScript extension but is modified by
        // the JavaScript extension loaded by the DocExtra UI extension.
        String expectedContent = expectedPageContent + "OnDemandJS";
        // Wait for the expected content to be added by the JavaScript extension loaded by the DocExtra UI extension.
        page.waitUntilContent(expectedContent);
        assertEquals(expectedContent, page.getContent().trim());
    }
}
