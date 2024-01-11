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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.like.internal.LikeRatingsConfiguration;
import org.xwiki.like.script.LikeScriptServiceComponentList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.ratings.internal.DefaultRating;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.script.UserScriptService;
import org.xwiki.velocity.tools.JSONTool;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code likers.vm} Velocity template.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@ComponentList({
    UserScriptService.class
})
@DefaultUserComponentList
@LikeScriptServiceComponentList
@SecurityScriptServiceComponentList
class LikersPageTest extends PageTest
{
    private static final String LIKERS = "likers.vm";

    private TemplateManager templateManager;

    private Map<String, Object> results;

    private RatingsManager ratingsManager;

    private AuthorizationManager authorizationManager;

    private ContextualAuthorizationManager contextualAuthorizationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.componentManager.registerMockComponent(RatingsManager.class, "solr");

        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);

        this.ratingsManager = this.componentManager.<RatingsManagerFactory>getInstance(RatingsManagerFactory.class)
            .getRatingsManager(LikeRatingsConfiguration.RATING_MANAGER_HINT);

        this.authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);
        this.contextualAuthorizationManager = this.componentManager.getInstance(ContextualAuthorizationManager.class);

        // Make sure LikeScriptService loads properly.
        this.componentManager.getInstance(ScriptService.class, "like");

        // Make sure UserScriptService loads properly.
        this.componentManager.getInstance(ScriptService.class, "user");
    }

    @Test
    void livetableResponseObfuscatedFiltered() throws Exception
    {
        DocumentReference likedDocumentReference = new DocumentReference("xwiki", "XWiki", "LikedDoc");
        DocumentReference user1DocumentReference = new DocumentReference("xwiki", "XWiki", "user1");
        DocumentReference user2DocumentReference = new DocumentReference("xwiki", "XWiki", "user2");

        this.request.put("livetable", "true");
        this.request.put("limit", "5");
        this.request.put("offset", "1");

        XWikiDocument likedDoc = this.xwiki.getDocument(likedDocumentReference, this.context);
        this.context.setDoc(likedDoc);

        Map<String, Object> user1Properties = new HashMap<>();
        user1Properties.put("first_name", "User1");
        user1Properties.put("last_name", "User1LN");
        this.xwiki.createUser("user1", user1Properties, this.context);

        Map<String, Object> user2Properties = new HashMap<>();
        user2Properties.put("first_name", "User2");
        user2Properties.put("last_name", "User2LN");
        this.xwiki.createUser("user2", user2Properties, this.context);

        when(this.authorizationManager.hasAccess(Right.VIEW, user1DocumentReference, user1DocumentReference))
            .thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, user2DocumentReference)).thenReturn(false);

        when(this.ratingsManager.getRatings(anyMap(), eq(0), eq(5), eq(RatingsManager.RatingQueryField.UPDATED_DATE),
            eq(false)))
            .thenReturn(asList(
                createRating(user1DocumentReference, likedDocumentReference),
                createRating(user2DocumentReference, likedDocumentReference)
            ));
        when(this.ratingsManager.countRatings(any())).thenReturn(2L);

        render();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) this.results.get("rows");
        assertEquals(1, rows.size());
        assertEquals("XWiki.user1", rows.get(0).get("doc_fullName"));
    }

    @Test
    void livetableResponseObfuscatedReturned() throws Exception
    {
        DocumentReference likedDocumentReference = new DocumentReference("xwiki", "XWiki", "LikedDoc");
        DocumentReference user1DocumentReference = new DocumentReference("xwiki", "XWiki", "user1");
        DocumentReference user2DocumentReference = new DocumentReference("xwiki", "XWiki", "user2");

        this.request.put("livetable", "true");
        this.request.put("limit", "1");
        this.request.put("offset", "1");

        XWikiDocument likedDoc = this.xwiki.getDocument(likedDocumentReference, this.context);
        this.context.setDoc(likedDoc);

        when(this.authorizationManager.hasAccess(Right.VIEW, user1DocumentReference, user1DocumentReference))
            .thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, user2DocumentReference)).thenReturn(false);

        when(this.ratingsManager.getRatings(anyMap(), eq(0), eq(1), eq(RatingsManager.RatingQueryField.UPDATED_DATE),
            eq(false)))
            .thenReturn(singletonList(createRating(user2DocumentReference, likedDocumentReference)));
        when(this.ratingsManager.countRatings(any())).thenReturn(2L);

        render();

        List<Map<String, Object>> rows = (List<Map<String, Object>>) this.results.get("rows");
        assertEquals(1, rows.size());
        assertEquals("obfuscated", rows.get(0).get("doc_fullName"));
    }

    private DefaultRating createRating(DocumentReference author, DocumentReference reference)
    {
        return new DefaultRating("r1")
            .setReference(reference)
            .setAuthor(new DocumentUserReference(author, false))
            .setVote(1)
            .setScaleUpperBound(1)
            .setManagerId("ratings");
    }

    private void render() throws Exception
    {
        JSONTool jsonTool = mock(JSONTool.class);
        registerVelocityTool("jsontool", jsonTool);
        this.templateManager.render(LIKERS);

        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(jsonTool).serialize(argument.capture());

        this.results = (Map<String, Object>) argument.getValue();
    }
}
