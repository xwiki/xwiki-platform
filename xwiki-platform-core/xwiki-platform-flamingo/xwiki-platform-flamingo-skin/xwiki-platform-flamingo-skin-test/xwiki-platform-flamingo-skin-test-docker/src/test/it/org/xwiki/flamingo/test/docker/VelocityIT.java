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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests related to the usage of velocity macros.
 *
 * @version $Id$
 * @since 11.5RC1
 */
@UITest
public class VelocityIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
    }

    @Order(1)
    @Test
    public void verifyMacros(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.setWikiPreference("iconTheme",  "IconThemes.FontAwesome");
        setup.deletePage(testReference);
        String macroContent = "{{velocity}}"
            + "{{html}}"
            + "#mimetypeimg('image/jpeg' 'photo.jpeg')"
            + "{{/html}}"
            + "{{/velocity}}";

        setup.createPage(testReference, macroContent);
        ViewPage viewPage = setup.gotoPage(testReference);
        assertTrue(viewPage.contentContainsElement(By.xpath("//p/span[@title = 'Image']/span[@class = 'fa fa-image']")));
    }

    /**
     * Verify that we can create macros in a document and including them into another document.
     */
    @Order(2)
    @Test
    public void includeMacrosInPage(TestUtils setup, TestReference testReference)
    {
        DocumentReference macoPageReference = new DocumentReference("Macro", testReference.getLastSpaceReference());
        DocumentReference testedPageReference = new DocumentReference("IncludeMacroTest",
            testReference.getLastSpaceReference());
        setup.deletePage(macoPageReference);
        setup.deletePage(testedPageReference);

        String macroContent = "{{velocity}}"
            + "#macro(testIncludeMacrosInPage)"
            + "hellomacro"
            + "#end"
            + "{{/velocity}}";

        String includeMacroContent = "{{velocity}}"
            + "#includeMacros(\"%s\")\n"
            + "#testIncludeMacrosInPage()"
            + "{{/velocity}}";
        setup.createPage(macoPageReference, macroContent);
        setup.createPage(testedPageReference, String.format(includeMacroContent,
            setup.serializeReference(macoPageReference)));
        ViewPage viewPage = setup.gotoPage(testedPageReference);
        assertEquals("hellomacro", viewPage.getContent());
    }

    /**
     * Verify that a Macro defined in a document is not visible from another document (using XWiki Syntax 1.0).
     * Note that for XWiki Syntax 2.0 this is verified in a unit test in the Velocity Macro module.
     */
    @Order(3)
    @Test
    public void testMacrosAreLocal(TestUtils setup, TestReference testReference)
    {
        DocumentReference localMacro1 = new DocumentReference("localMacro1", testReference.getLastSpaceReference());
        DocumentReference localMacro2 = new DocumentReference("localMacro2", testReference.getLastSpaceReference());
        setup.deletePage(localMacro1);
        setup.deletePage(localMacro2);
        String localMacroContent1 = "{{velocity}}"
            + "#macro(testMacrosAreLocal)"
            + "mymacro"
            + "#end\n"
            + "#testMacrosAreLocal()"
            + "{{/velocity}}";

        String localMacroContent2 = "{{velocity}}"
            + "#testMacrosAreLocal()"
            + "{{/velocity}}";

        setup.createPage(localMacro1, localMacroContent1);
        setup.createPage(localMacro2, localMacroContent2);
        ViewPage viewPage = setup.gotoPage(localMacro2);
        assertEquals("#testMacrosAreLocal()", viewPage.getContent());
        viewPage = setup.gotoPage(localMacro1);
        assertEquals("mymacro", viewPage.getContent());
    }

    /**
     * Tests that the document dates are always of the type java.util.Date, as hibernate returns
     * java.sql.Timestamp, which is not entirely compatible with java.util.Date. When the cache
     * storage is enabled, this problem isn't detected until the document is removed from the cache.
     */
    @Order(4)
    @Test
    public void dateClass(TestUtils testUtils, TestReference testReference)
    {
        ViewPage viewPage = testUtils.createPage(testReference,
            "{{velocity}}$xwiki.flushCache()\n$xwiki.getDocument('Main.WebHome').date.class{{/velocity}}",
            "TestDateClass");
        assertEquals("class java.util.Date", viewPage.getContent());
    }

    /**
     * Tests the security measures for calling {@code #ŧemplate('XXX')} in velocity code.
     */
    @Order(5)
    @Test
    public void velocityTemplate(TestUtils testUtils, TestReference testReference)
    {
        String templateCode = "#template('%s')\n";
        ViewPage viewPage = testUtils.createPage(testReference,
            "{{velocity}}{{html}}"
            + String.format(templateCode, "code.vm")
            + "{{/html}}{{/velocity}}",
            "TestTemplate");

        // we cannot check the actual content here, so we're just checking the presence of the CSS class related to
        // code.vm in the content view.
        WebElement content = testUtils.getDriver().findElementWithoutWaiting(By.id("xwikicontent"));
        assertNotNull(testUtils.getDriver().findElementWithoutWaiting(content, By.className("wiki-code")));

        viewPage = testUtils.createPage(testReference, "{{velocity}}"
            + String.format(templateCode, "../../")
            + "{{/velocity}}",
            "TestTemplate");
        assertTrue(viewPage.getContent().isEmpty(), "root directory template call should not display anything.");

        viewPage = testUtils.createPage(testReference, "{{velocity}}"
                + String.format(templateCode, "asdfasdf")
                + "{{/velocity}}",
            "TestTemplate");
        assertTrue(viewPage.getContent().isEmpty(), "Not existing template call should not display anything.");

        viewPage = testUtils.createPage(testReference, "{{velocity}}"
                + String.format(templateCode, "../redirect")
                + "{{/velocity}}",
            "TestTemplate");
        assertTrue(viewPage.getContent().isEmpty(), "File in parent directory call should not display anything.");

        viewPage = testUtils.createPage(testReference, "{{velocity}}"
                + String.format(templateCode, "../WEB-INF/version.properties")
                + "{{/velocity}}",
            "TestTemplate");
        assertTrue(viewPage.getContent().isEmpty(), "File in the wrong directory call should not display anything.");

        viewPage = testUtils.createPage(testReference, "{{velocity}}"
                + String.format(templateCode, "/chw/../../WEB-INF/../WEB-INF/lib/../version.properties")
                + "{{/velocity}}",
            "TestTemplate");
        assertTrue(viewPage.getContent().isEmpty(), "File in the wrong directory, with not normalized path call should "
            + "not display anything.");
    }
}
