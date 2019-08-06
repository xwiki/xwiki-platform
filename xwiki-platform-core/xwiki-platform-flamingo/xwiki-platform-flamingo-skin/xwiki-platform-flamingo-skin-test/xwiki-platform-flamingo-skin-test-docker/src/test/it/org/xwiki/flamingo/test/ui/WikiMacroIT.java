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
package org.xwiki.flamingo.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests related to wikimacros.
 * @version $Id$
 * @since 11.7RC1
 * @since 11.6.1
 */
@UITest
public class WikiMacroIT
{
    private final static String WIKI_MACRO_CLASS = "XWiki.WikiMacroClass";

    private final static String WIKI_MACRO_PARAMETER_CLASS = "XWiki.WikiMacroParameterClass";

    /**
     * Ensure that the wikimacro bindings are properly managed, even when using nested macro, and calling them
     * with some context manipulation by using getRenderedContent.
     */
    @Test
    public void bindingsAndNestedMacros(TestUtils setup)
    {
        // We need to be superadmin to have scripts rights
        setup.loginAsSuperAdmin();
        DocumentReference parentReference = new DocumentReference("xwiki", "WikiMacroIT", "WebHome");
        DocumentReference wikimacro1 = new DocumentReference("wikimacro1", parentReference.getLastSpaceReference());
        DocumentReference wikimacro2 = new DocumentReference("wikimacro2", parentReference.getLastSpaceReference());

        setup.deletePage(wikimacro1);
        setup.deletePage(wikimacro2);

        String wikiMacro1MacroContent = "{{velocity}}\n"
            + "parameter wikimacro1: $xcontext.macro.params.param1 $wikimacro.parameters.param1\n"
            + "content wikimacro1: $xcontext.macro.content $wikimacro.content\n"
            + "{{/velocity}}";

        String wikiMacro1Serialized = setup.serializeReference(wikimacro1);
        String wikiMacro2MacroContent = String.format("{{velocity}}\n"
            + "\n"
            + "Wikimacro2 parameter: $xcontext.macro.params.param1 $wikimacro.parameters.param1\n"
            + "Wikimacro2 content: $xcontext.macro.content $wikimacro.content\n"
            + "\n"
            + "#set ($wikimacro1 = $xwiki.getDocument(\"%s\"))\n"
            + "Content from the wikimacro1 page itself: \n"
            + "\n"
            + "$wikimacro1.getRenderedContent()\n"
            + "\n"
            + "Wikimacro2 parameter: $xcontext.macro.params.param1 $wikimacro.parameters.param1\n"
            + "Wikimacro2 content: $xcontext.macro.content $wikimacro.content\n"
            + "{{/velocity}}", wikiMacro1Serialized);

        String wikiMacro1PageContent = "{{wikimacro1 param1=\"wikimacro1_parameter\"}}\n"
            + "Wikimacro 1 content\n"
            + "{{/wikimacro1}}";
        String wikiMacro2PageContent = "{{wikimacro2 param1=\"wikimacro2_parameter\"}}\n"
            + "Contenu de la wikimacro2\n"
            + "{{/wikimacro2}}";

        setup.createPage(wikimacro1, wikiMacro1PageContent, "");
        setup.addObject(wikimacro1, WIKI_MACRO_CLASS,
            "id", "wikimacro1",
            "contentType", "Mandatory",
            "visibility", "Current Wiki",
            "code", wikiMacro1MacroContent);
        setup.addObject(wikimacro1, WIKI_MACRO_PARAMETER_CLASS, "name", "param1");
        //setup.gotoPage(wikimacro1).editObjects().clickSaveAndView();

        setup.createPage(wikimacro2, wikiMacro2PageContent, "");
        setup.addObject(wikimacro2, WIKI_MACRO_CLASS,
            "id", "wikimacro2",
            "contentType", "Mandatory",
            "visibility", "Current Wiki",
            "code", wikiMacro2MacroContent);
        setup.addObject(wikimacro2, WIKI_MACRO_PARAMETER_CLASS, "name", "param1");
        //setup.gotoPage(wikimacro2).editObjects().clickSaveAndView();

        String expectedContent = "Wikimacro2 parameter: wikimacro2_parameter wikimacro2_parameter\n"
            + "Wikimacro2 content: Contenu de la wikimacro2 Contenu de la wikimacro2\n"
            + "Content from the wikimacro1 page itself: \n"
            + "<p>parameter wikimacro1: wikimacro1_parameter wikimacro1_parameter<br/>"
            + "content wikimacro1: Wikimacro 1 content Wikimacro 1 content</p>\n"
            + "Wikimacro2 parameter: wikimacro2_parameter wikimacro2_parameter\n"
            + "Wikimacro2 content: Contenu de la wikimacro2 Contenu de la wikimacro2";
        ViewPage viewPage = setup.gotoPage(wikimacro2);
        assertEquals(expectedContent, viewPage.getContent());
    }
}
