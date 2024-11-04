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
package org.xwiki.help.test.ui.docker;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.test.po.PanelViewPage;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.WikiMacroClassDocumentInitializer;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.uiextension.internal.UIExtensionClassDocumentInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.object;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Validate the Tips panel.
 *
 * @version $Id$
 */
@UITest(properties = {
    // "$xcontext.context" (used in the test) requires PR
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:.*PanelIT"
        + ".verifyTipsContentIsExecutedWithTheRightAuthor.WebHome"
})
class TipsPanelIT
{
    private final static String TIPS_UIXP = "org.xwiki.platform.help.tipsPanel";

    private final static String TIPS_UIXP_DISABLED = TIPS_UIXP + ".disabled";

    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void verifyTipsParameterIsRestricted(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // Unregister all tips
        switchUIXs(TIPS_UIXP, TIPS_UIXP_DISABLED, testUtils);

        // Register a macro to check if the context is restricted
        registerIsrestrictedMacro(testUtils);

        // Create a parameter based tip
        Page tipPage = testUtils.rest().page(testReference);
        tipPage.setObjects(new Objects());
        org.xwiki.rest.model.jaxb.Object tipObject = object(UIExtensionClassDocumentInitializer.CLASS_REFERENCE_STRING);
        tipObject.getProperties().add(property("name", testUtils.serializeReference(testReference)));
        tipObject.getProperties().add(property("extensionPointId", "org.xwiki.platform.help.tipsPanel"));
        tipObject.getProperties().add(property("parameters", "tip=execution is restricted: {{isrestricted/}}"));
        tipPage.getObjects().getObjectSummaries().add(tipObject);
        testUtils.rest().save(tipPage);

        // Execute the tip panel and verify the result is restricted (the velocity macro is forbidden)
        testUtils.gotoPage(new DocumentReference("xwiki", List.of("Help", "TipsPanel"), "WebHome"));
        PanelViewPage panelPage = new PanelViewPage();
        assertEquals("execution is restricted: true", panelPage.getPanelContent().getText());

        // Put back all tips
        switchUIXs(TIPS_UIXP_DISABLED, TIPS_UIXP, testUtils);
    }

    @Test
    @Order(2)
    void verifyTipsContentIsExecutedWithTheRightAuthor(TestUtils testUtils, TestReference testReference)
        throws Exception
    {
        // Unregister all tips
        switchUIXs(TIPS_UIXP, TIPS_UIXP_DISABLED, testUtils);

        // Register a macro to check if the context is restricted
        registerIsrestrictedMacro(testUtils);

        // Switch to a different user than tips panel author
        testUtils.createAdminUser(true);

        // Create a content based tip
        Page tipPage = testUtils.rest().page(testReference);
        tipPage.setObjects(new Objects());
        org.xwiki.rest.model.jaxb.Object tipObject = object(UIExtensionClassDocumentInitializer.CLASS_REFERENCE_STRING);
        tipObject.getProperties().add(property("name", testUtils.serializeReference(testReference)));
        tipObject.getProperties().add(property("extensionPointId", "org.xwiki.platform.help.tipsPanel"));
        tipObject.getProperties().add(property("content",
            "execution is restricted: {{isrestricted/}}, "
                + "executed by {{velocity}}$xcontext.context.authorReference{{/velocity}}"));
        tipPage.getObjects().getObjectSummaries().add(tipObject);
        testUtils.rest().save(tipPage);

        // Execute the tip panel and verify the result is the expected one
        testUtils.gotoPage(new DocumentReference("xwiki", List.of("Help", "TipsPanel"), "WebHome"));
        PanelViewPage panelPage = new PanelViewPage();
        assertEquals(
            "execution is restricted: false, executed by xwiki:XWiki." + TestUtils.ADMIN_CREDENTIALS.getUserName(),
            panelPage.getPanelContent().getText());

        // Put back all tips
        switchUIXs(TIPS_UIXP_DISABLED, TIPS_UIXP, testUtils);
    }

    private void switchUIXs(String oldid, String newid, TestUtils testUtils) throws Exception
    {
        testUtils.executeWiki(String.format(
        // @formatter:off
            "{{velocity}}\n"
            + "#foreach($uix in $services.uix.getExtensions('%s'))\n"
            + "  #set ($uixDocument = $xwiki.getDocument($uix.documentReference))\n"
            + "  $uixDocument.set('extensionPointId', '%s')\n"
            + "  $uixDocument.save()\n"
            + "#end\n"
            + "{{/velocity}}", oldid, newid)
            // @formatter:on
            , Syntax.XWIKI_2_1);
    }

    private void registerIsrestrictedMacro(TestUtils testUtils) throws Exception
    {
        LocalDocumentReference macroReference = new LocalDocumentReference("Test", "IsrestrictedMacro");
        Page macroPage = testUtils.rest().page(macroReference);
        macroPage.setObjects(new Objects());
        org.xwiki.rest.model.jaxb.Object tipObject = object(WikiMacroClassDocumentInitializer.WIKI_MACRO_CLASS);
        tipObject.getProperties().add(property("id", "isrestricted"));
        tipObject.getProperties().add(property("supportsInlineMode", 1));
        tipObject.getProperties()
            .add(property("code", "{{velocity}}$wikimacro.context.transformationContext.isRestricted(){{/velocity}}"));
        macroPage.getObjects().getObjectSummaries().add(tipObject);
        testUtils.rest().save(macroPage);
    }
}
