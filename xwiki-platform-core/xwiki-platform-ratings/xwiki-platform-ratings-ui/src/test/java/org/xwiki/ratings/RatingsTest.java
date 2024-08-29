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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.ratings.script.RatingsScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of the ratings.
 *
 * @version $Id$
 * @since 13.1RC1
 * @since 12.10.4
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
class RatingsTest extends PageTest
{
    @Test
    void displayFullRatingEscapesRequestDocRef() throws Exception
    {
        // Initialize the ratings script service to allow displayFullRating to display the blocks to be tested.
        RatingsScriptService ratingsScriptService = mock(RatingsScriptService.class);
        when(ratingsScriptService.getAverageRating(any())).thenReturn(Optional.of(mock(AverageRating.class)));
        this.oldcore.getMocker().registerComponent(ScriptService.class, "ratings", ratingsScriptService);

        setOutputSyntax(Syntax.HTML_5_0);

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

        // Verify that the displayFullRating parameter is actually escaped when used.
        assertThat(renderHTMLPage(document).getElementsByClass("rating-wrapper").eachAttr("data-reference"),
            contains("a:b.c\"d"));
    }
}
