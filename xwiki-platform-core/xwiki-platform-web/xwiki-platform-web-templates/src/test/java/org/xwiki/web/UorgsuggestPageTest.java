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
package org.xwiki.web;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xwiki.icon.IconManagerScriptServiceComponentList;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.QueryParameter;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.IconSetup;
import org.xwiki.test.page.PageTest;
import org.xwiki.user.UserConfiguration;
import org.xwiki.user.UserProperties;
import org.xwiki.user.script.UserScriptService;
import org.xwiki.wiki.script.WikiManagerScriptService;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.script.WikiUserManagerScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.builder.Input.fromStream;
import static org.xmlunit.builder.Input.fromString;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

/**
 * Test of the {@code uorgsuggest.vm} Velocity template.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@SecurityScriptServiceComponentList
@IconManagerScriptServiceComponentList
@ComponentList({
    ModelScriptService.class,
})
class UorgsuggestPageTest extends PageTest
{
    private static final String UORGSUGGEST = "uorgsuggest.vm";

    private static final String USERS_STATEMENT_PREFIX = "from doc.object(XWiki.XWikiUsers) as user "
        + "where (lower(doc.name) like :input "
        + "or concat(concat(lower(user.first_name), ' '), lower(user.last_name)) like :input) ";

    private static final String USERS_ORDER_BY =
        "order by lower(user.first_name), user.first_name, lower(user.last_name), user.last_name";

    private static final String MAIN_WIKI = "xwiki";

    private static final String SUBWIKI = "subwiki";

    private TemplateManager templateManager;

    @Mock
    private QueryManagerScriptService queryManagerScriptService;

    @Mock
    private ScriptQuery query;

    @Mock
    private UserScriptService userScriptService;

    @Mock
    private UserConfiguration userConfiguration;

    @Mock
    private WikiManagerScriptService wikiManagerScriptService;

    @Mock
    private WikiUserManagerScriptService wikiUserManagerScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        IconSetup.setUp(this, "/icons/default.iconset");

        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.componentManager.registerComponent(ScriptService.class, "query", this.queryManagerScriptService);
        when(this.queryManagerScriptService.xwql(anyString())).thenReturn(this.query);
        when(this.query.setWiki(any())).thenReturn(this.query);
        when(this.query.addFilter(anyString())).thenReturn(this.query);
        QueryParameter qp = mock(QueryParameter.class);
        when(this.query.bindValue(anyString())).thenReturn(qp);
        when(qp.anyChars()).thenReturn(qp);
        when(qp.literal(anyString())).thenReturn(qp);
        when(qp.query()).thenReturn(this.query);

        this.componentManager.registerComponent(ScriptService.class, "user", this.userScriptService);
        when(this.userScriptService.getConfiguration()).thenReturn(this.userConfiguration);
    }

    @Test
    void usersGlobalXML() throws Exception
    {
        when(this.xwiki.getPlainUserName(new DocumentReference("xwiki", "XWiki", "U1"), this.context))
            .thenReturn("User 1");

        // U2 is excluded from the results because the current user does not have read access to this user.
        // U12 is excluded from the results because after U11, the 10 maximum results are already aggregated.
        when(this.query.execute()).thenReturn(
            asList("xwiki:XWiki.U1", "xwiki:XWiki.U2", "xwiki:XWiki.U3", "xwiki:XWiki.U4", "xwiki:XWiki.U5",
                "xwiki:XWiki.U6", "xwiki:XWiki.U7", "xwiki:XWiki.U8", "xwiki:XWiki.U9", "xwiki:XWiki.U10",
                "xwiki:XWiki.U11", "xwiki:XWiki.U12"));
        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "U2")))).thenReturn(false);

        this.request.put("uorg", "user");

        String render = this.templateManager.render(UORGSUGGEST).trim();

        verify(this.queryManagerScriptService).xwql(USERS_STATEMENT_PREFIX + "and user.active = 1 " + USERS_ORDER_BY);
        InputStream inputStream = getClass().getResourceAsStream("/uorgsuggest/users.xml");
        assertThat(fromString(render), isIdenticalTo(fromStream(inputStream)).ignoreWhitespace());
    }

    @Test
    void usersGlobalJson() throws Exception
    {
        when(this.xwiki.getPlainUserName(new DocumentReference("xwiki", "XWiki", "U1"), this.context))
            .thenReturn("User 1");

        when(this.userConfiguration.getUserQualifierProperty()).thenReturn("address");
        UserProperties u3Properties = mock(UserProperties.class, "U3");
        when(u3Properties.getProperty("address")).thenReturn("Paris, France");
        when(this.userScriptService.getProperties("xwiki:XWiki.U3")).thenReturn(u3Properties);

        // U2 is excluded from the results because the current user does not have read access to this user.
        // U12 is excluded from the results because after U11, the 10 maximum results are already aggregated.
        when(this.query.execute())
            .thenReturn(asList("xwiki:XWiki.U1", "xwiki:XWiki.U2", "xwiki:XWiki.U3", "xwiki:XWiki.U4", "xwiki:XWiki.U5",
                "xwiki:XWiki.U6", "xwiki:XWiki.U7", "xwiki:XWiki.U8", "xwiki:XWiki.U9", "xwiki:XWiki.U10",
                "xwiki:XWiki.U11", "xwiki:XWiki.U12"));
        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "U2")))).thenReturn(false);

        this.request.put("uorg", "user");
        this.request.put("media", "json");

        String render = this.templateManager.render(UORGSUGGEST).trim();

        verify(this.queryManagerScriptService).xwql(USERS_STATEMENT_PREFIX + "and user.active = 1 " + USERS_ORDER_BY);

        InputStream inputStream = getClass().getResourceAsStream("/uorgsuggest/users.json");
        JSONAssert.assertEquals(IOUtils.toString(inputStream, StandardCharsets.UTF_8), render, true);
    }

    @Test
    void usersIncludingInactiveUsers() throws Exception
    {
        this.request.put("uorg", "user");
        this.request.put("media", "json");
        this.request.put("includeInactiveUsers", "true");

        String render = this.templateManager.render(UORGSUGGEST).trim();

        // The disabled users are not filtered out.
        verify(this.queryManagerScriptService).xwql(USERS_STATEMENT_PREFIX + USERS_ORDER_BY);
        assertEquals("[]", render);
    }

    @Test
    void groupsGlobalXML() throws Exception
    {
        when(this.query.execute()).thenReturn(asList("xwiki:XWiki.G1", "xwiki:XWiki.G2"));
        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "G2")))).thenReturn(false);

        String render = this.templateManager.render(UORGSUGGEST).trim();

        verify(this.queryManagerScriptService).xwql(
            "from doc.object(XWiki.XWikiGroups) as groups " + "where lower(doc.name) like :input "
                + "and doc.fullName <> 'XWiki.XWikiGroupTemplate' " + "order by lower(doc.name), doc.name");

        InputStream inputStream = getClass().getResourceAsStream("/uorgsuggest/groups.xml");
        assertThat(fromString(render), isIdenticalTo(fromStream(inputStream)).ignoreWhitespace());
    }

    @Test
    void groupsGlobalJson() throws Exception
    {
        when(this.query.execute()).thenReturn(asList("xwiki:XWiki.G1", "xwiki:XWiki.G2"));
        when(this.oldcore.getMockContextualAuthorizationManager()
            .hasAccess(same(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "G2")))).thenReturn(false);
        this.request.put("media", "json");

        String render = this.templateManager.render(UORGSUGGEST).trim();

        verify(this.queryManagerScriptService).xwql(
            "from doc.object(XWiki.XWikiGroups) as groups " + "where lower(doc.name) like :input "
                + "and doc.fullName <> 'XWiki.XWikiGroupTemplate' " + "order by lower(doc.name), doc.name");

        InputStream inputStream = getClass().getResourceAsStream("/uorgsuggest/groups.json");
        JSONAssert.assertEquals(IOUtils.toString(inputStream, StandardCharsets.UTF_8), render, true);
    }

    @Test
    void autoSuggestsTheLocalUsersOfAWikiThatHasItsOwn() throws Exception
    {
        setUserScope(SUBWIKI, UserScope.LOCAL_ONLY);
        when(this.query.execute()).thenReturn(asList("XWiki.Alice"));

        suggestUsersOf(SUBWIKI);

        verify(this.query).setWiki(SUBWIKI);
        verify(this.query, never()).setWiki(MAIN_WIKI);
    }

    @Test
    void autoSuggestsTheGlobalUsersOfAWikiWithoutUsersOfItsOwn() throws Exception
    {
        setUserScope(SUBWIKI, UserScope.GLOBAL_ONLY);
        when(this.query.execute()).thenReturn(asList("XWiki.Alice"));

        suggestUsersOf(SUBWIKI);

        verify(this.query).setWiki(MAIN_WIKI);
        verify(this.query, never()).setWiki(SUBWIKI);
    }

    @Test
    void autoCompletesTheLocalUsersWithTheGlobalOnes() throws Exception
    {
        setUserScope(SUBWIKI, UserScope.LOCAL_AND_GLOBAL);
        userName(SUBWIKI, "alice");
        userName(SUBWIKI, "Carol");
        userName(MAIN_WIKI, "Bob");
        when(this.query.execute()).thenReturn(asList("XWiki.Carol", "XWiki.alice"), asList("XWiki.Bob"));

        // Sorted by name, ignoring its case, so that the two wikis don't look like two separate lists.
        assertEquals(List.of("alice", "Bob", "Carol"), labels(suggestUsersOf(SUBWIKI)));

        verify(this.query).setWiki(SUBWIKI);
        verify(this.query).setWiki(MAIN_WIKI);
    }

    @Test
    void autoQueriesTheMainWikiOnceWhenItsUsersAreLocalAndGlobal() throws Exception
    {
        setUserScope(MAIN_WIKI, UserScope.LOCAL_AND_GLOBAL);
        userName(MAIN_WIKI, "Alice");
        when(this.query.execute()).thenReturn(asList("XWiki.Alice"));

        // The local and the global users are the same on the main wiki, so they are suggested once.
        assertEquals(List.of("Alice"), labels(suggestUsersOf(MAIN_WIKI)));

        verify(this.query).setWiki(MAIN_WIKI);
    }

    @Test
    void autoStopsQueryingWhenTheLocalUsersFillTheLimit() throws Exception
    {
        setUserScope(SUBWIKI, UserScope.LOCAL_AND_GLOBAL);
        when(this.query.execute()).thenReturn(asList("XWiki.Alice", "XWiki.Bob"));
        this.request.put("limit", "2");

        suggestUsersOf(SUBWIKI);

        verify(this.query, never()).setWiki(MAIN_WIKI);
    }

    @Test
    void autoSuggestsTheLocalUsersWithoutTheWikiModule() throws Exception
    {
        // The wiki script service isn't registered, as when the wiki module isn't installed.
        when(this.query.execute()).thenReturn(asList("XWiki.Alice"));

        suggestUsersOf(SUBWIKI);

        verify(this.query).setWiki(SUBWIKI);
        verify(this.query, never()).setWiki(MAIN_WIKI);
    }

    /**
     * Configure the user scope of a wiki, which also registers the script service holding it.
     *
     * @param wikiId the identifier of the wiki
     * @param userScope the user scope of that wiki
     */
    private void setUserScope(String wikiId, UserScope userScope) throws Exception
    {
        this.componentManager.registerComponent(ScriptService.class, "wiki", this.wikiManagerScriptService);
        when(this.wikiManagerScriptService.get("user")).thenReturn(this.wikiUserManagerScriptService);
        when(this.wikiUserManagerScriptService.getUserScope(wikiId)).thenReturn(userScope);
    }

    /**
     * Give a user the name the suggestions display, instead of the name of its page.
     *
     * @param wikiId the identifier of the wiki holding the user
     * @param name the name of the user, also the name of its page
     */
    private void userName(String wikiId, String name)
    {
        when(this.xwiki.getPlainUserName(new DocumentReference(wikiId, "XWiki", name), this.context))
            .thenReturn(name);
    }

    /**
     * Suggest the users of a wiki, letting that wiki apply its own user scope.
     *
     * @param wikiId the identifier of the wiki the request is sent to
     * @return the suggestions, as JSON
     */
    private String suggestUsersOf(String wikiId) throws Exception
    {
        // The suggestions come from the wiki of the page the template renders against.
        this.context.setDoc(new XWikiDocument(new DocumentReference(wikiId, "Main", "WebHome")));
        this.request.put("uorg", "user");
        this.request.put("media", "json");
        this.request.put("wiki", "auto");

        return this.templateManager.render(UORGSUGGEST).trim();
    }

    /**
     * @param suggestions the suggestions, as JSON
     * @return the names the given suggestions display, in the order in which they are suggested
     */
    private List<String> labels(String suggestions)
    {
        JSONArray results = new JSONArray(suggestions);

        return IntStream.range(0, results.length())
            .mapToObj(index -> results.getJSONObject(index).getString("label"))
            .toList();
    }
}
