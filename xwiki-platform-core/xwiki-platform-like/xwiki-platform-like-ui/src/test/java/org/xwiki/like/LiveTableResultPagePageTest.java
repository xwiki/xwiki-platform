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
package org.xwiki.like;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.like.script.LikeScriptServiceComponentList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.internal.DefaultRating;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@code XWiki.Like.Code.LiveTableResultPage}.
 *
 * @version $Id$
 * @since 15.1
 * @since 14.10.6
 */
@XWikiSyntax21ComponentList
@LikeScriptServiceComponentList
@UserReferenceComponentList
@ComponentList({
    TemplateScriptService.class,
    ModelScriptService.class
})
class LiveTableResultPagePageTest extends PageTest
{
    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", List.of("XWiki", "Like", "Code"), "LiveTableResultPage");

    @MockComponent
    @Named("solr")
    private RatingsManager ratingsManager;

    @Test
    void likedPageWithADot() throws Exception
    {
        DocumentReference likedPageReference = new DocumentReference("xwiki", "Space", "With.ADot");

        // Create an empty page to be returned as a liked page.
        XWikiDocument likedDocument = this.xwiki.getDocument(likedPageReference, this.context);
        this.xwiki.saveDocument(likedDocument, this.context);

        // The rating manager is mocked for now as the focus of the tests is currently not to test the likes indexing.
        when(this.ratingsManager.getRatings(anyMap(), anyInt(), anyInt(), any(RatingsManager.RatingQueryField.class),
            anyBoolean())).thenReturn(List.of(new DefaultRating("id1").setReference(likedPageReference)));

        setOutputSyntax(Syntax.PLAIN_1_0);
        this.context.setAction("get");

        this.request.put("offset", "1");
        this.request.put("reqNo", "1");
        this.request.put("limit", "10");

        JSONObject object = renderJSON(DOCUMENT_REFERENCE);

        assertEquals("xwiki:Space.With\\.ADot", object.getJSONArray("rows").getJSONObject(0).getString("doc_fullName"));
    }

    @SuppressWarnings("unchecked")
    private <T extends JSON> T renderJSON(DocumentReference documentReference) throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);

        renderPage(documentReference);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        return (T) JSONSerializer.toJSON(argument.getValue());
    }
}
