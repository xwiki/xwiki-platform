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
package org.xwiki.flamingo;

import java.util.List;

import javax.script.ScriptContext;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptServiceComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.wiki.script.WikiManagerScriptServiceComponentList;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code FlamingoThemesCode.WebHomeSheet} page.
 *
 * @version $Id$
 * @since 13.10.10
 * @since 14.4.6
 * @since 14.9RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@SecurityScriptServiceComponentList
@WikiManagerScriptServiceComponentList
@ComponentList({
    ErrorMessageMacro.class,
    TranslationMacro.class,
    TestNoScriptMacro.class,
    ModelScriptService.class
})
class WebHomeSheetPageTest extends PageTest
{
    private static final DocumentReference WEBHOME_SHEET =
        new DocumentReference("xwiki", "FlamingoThemesCode", "WebHomeSheet");

    private static final DocumentReference NEW_THEME_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "Space", "NewTheme");

    private QueryManagerScriptService queryManagerScriptService;

    private AuthorizationManager authorizationManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.queryManagerScriptService =
            this.componentManager.registerMockComponent(ScriptService.class, "query", QueryManagerScriptService.class,
                false);
        this.authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
    }

    @Test
    void createAction() throws Exception
    {
        this.request.put("newThemeName", "some content\"/}}{{noscript/}}");
        this.request.put("form_token", "1");
        this.request.put("action", "create");

        Document document = renderHTMLPage(WEBHOME_SHEET);

        assertEquals("platform.flamingo.themes.home.create.csrf [some content\"/}}{{noscript/}}]",
            document.select(".box.errormessage").text());
    }

    @Test
    void listAvailableThemes() throws Exception
    {
        loadPage(new DocumentReference("xwiki", "FlamingoThemes", "Charcoal"));
        initNewTheme();
        
        // Mock the database.
        Query query = mock(Query.class);
        when(this.queryManagerScriptService.xwql("from doc.object(FlamingoThemesCode.ThemeClass) obj WHERE doc"
            + ".fullName <> 'FlamingoThemesCode.ThemeTemplate' ORDER BY doc.name")).thenReturn(query);
        when(query.setWiki(anyString())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("Space.NewTheme"));
        
        // Allow the current user to have access to the resources.
        when(this.authorizationManager.hasAccess(eq(Right.VIEW), any(), eq(NEW_THEME_DOCUMENT_REFERENCE)))
            .thenReturn(true);
        this.scriptContext.setAttribute("hasAdmin", true, GLOBAL_SCOPE);

        Document document = renderHTMLPage(WEBHOME_SHEET);
        
        // Validate the links and styles.
        Element newThemeHeader = document.select("h3").get(1);
        String newThemeHeaderText = newThemeHeader.text();
        String newThemeHeaderLinkHref = newThemeHeader.selectFirst("a").attr("href");
        assertEquals("]] &#123;&#123;noscript}}println(\"Hello from title!\")&#123;&#123;/noscript}}",
            newThemeHeaderText);
        assertEquals("Space.NewTheme", newThemeHeaderLinkHref);

        String newThemeMockupPageStyle = document.select(".mockup-page").get(1).attr("style");
        assertEquals("background-color: {{/html}} {{noscript}}println(\"Hello from body-bg!\"){{/noscript}} \"/>"
                + "<script>...</script/>",
            newThemeMockupPageStyle);
    }

    /**
     * Creates a new page describing a theme.
     *
     * @throws XWikiException in case of error
     */
    private void initNewTheme() throws XWikiException
    {
        XWikiDocument newTheme = this.xwiki.getDocument(NEW_THEME_DOCUMENT_REFERENCE, this.context);
        newTheme.setTitle("]] {{noscript}}println(\"Hello from title!\"){{/noscript}}");
        BaseObject baseObject =
            newTheme.newXObject(new DocumentReference("xwiki", "FlamingoThemesCode", "ThemeClass"), this.context);
        baseObject.setStringValue("body-bg",
            "{{/html}} {{noscript}}println(\"Hello from body-bg!\"){{/noscript}} \"/><script>...</script/>");
        this.xwiki.saveDocument(newTheme, this.context);
    }
}
