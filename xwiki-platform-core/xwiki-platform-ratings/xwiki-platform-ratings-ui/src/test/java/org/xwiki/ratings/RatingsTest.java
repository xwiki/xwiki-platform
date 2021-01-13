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
package org.xwiki.ratings;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Named;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.ratings.script.RatingsScriptService;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.xml.html.filter.HTMLFilter;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of the ratings.
 *
 * @version $Id$
 * @since 13.0RC1
 */
@XWikiSyntax21ComponentList
@XHTML10ComponentList
class RatingsTest extends PageTest
{
    // Needs to be registered for the ratings macro to be loaded successfully.
    @MockComponent
    @Named("template")
    private Macro templateMacro;

    // Needs to be registered for the ratings macro to be loaded successfully.
    @MockComponent
    @Named("controlcharacters")
    private HTMLFilter htmlFilter;

    @Test
    void displayFullRatingEscapesRequestDocRef() throws Exception
    {
        MacroDescriptor templateMacroDescriptor = mock(MacroDescriptor.class);
        when(this.templateMacro.getDescriptor()).thenReturn(templateMacroDescriptor);
        when(templateMacroDescriptor.getParametersBeanClass()).thenReturn((Class) "".getClass());

        registerVelocityTool("escapetool", new EscapeTool());

        // Initialize the ratings script service to allow displayFullRating to display the blocks to be tested.
        RatingsScriptService ratingsScriptService = mock(RatingsScriptService.class);
        when(ratingsScriptService.getAverageRating(any())).thenReturn(Optional.of(mock(AverageRating.class)));
        this.oldcore.getMocker().registerComponent(ScriptService.class, "ratings", ratingsScriptService);

        setOutputSyntax(Syntax.XHTML_1_0);

        // Initialize a document reference to be used as parameter when calling displayFullRating.
        ModelScriptService modelScriptService = mock(ModelScriptService.class);
        when(modelScriptService.createDocumentReference("a", "b", "c\"d"))
            .thenReturn(new DocumentReference("a", "b", "c\"d"));
        this.oldcore.getMocker().registerComponent(ScriptService.class, "model", modelScriptService);

        // Load the RatingsMacros that contains the tested displayFullRating.
        loadPage(new DocumentReference("xwiki", Arrays.asList("XWiki", "Ratings"), "RatingsMacros"));

        // Call the displayFullRating macro with a name with a double quote to test that it is properly escaped.
        String script = "{{include reference=\"XWiki.Ratings.RatingsMacros\"/}}\n"
            + "{{velocity}}#set($tdoc = $services.model.createDocumentReference('a', 'b', 'c\"d'))\n"
            + "#displayFullRating($tdoc){{/velocity}}";

        XWikiDocument document = new XWikiDocument(new DocumentReference("xwiki", "XWiki", "Test"));
        document.setSyntax(Syntax.XWIKI_2_0);
        document.setContent(script);
        this.xwiki.saveDocument(document, "registering document", true, this.context);
        String content = document.getRenderedContent(this.context);

        Document parse = Jsoup.parse(content);
        Elements elementsByClass = parse.getElementsByClass("rating-wrapper");
        assertEquals(1, elementsByClass.size());
        String attr = elementsByClass.first().attr("data-reference");
        // Verify that the displayFullRating parameter is actually escaped when used.
        assertEquals("a:b.c\"d", attr);
    }
}
