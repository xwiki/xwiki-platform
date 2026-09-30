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
package org.xwiki.sharepage;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the mail templates of the {@code XWiki.SharePage} page.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    ModelScriptService.class
})
class SharePagePageTest extends PageTest
{
    private static final DocumentReference SHARE_PAGE = new DocumentReference("xwiki", "XWiki", "SharePage");

    private static final DocumentReference MAIL_CLASS = new DocumentReference("xwiki", "XWiki", "Mail");

    private static final DocumentReference SHARED_PAGE = new DocumentReference("xwiki", "Space", "Page");

    private static final DocumentReference SENDER = new DocumentReference("xwiki", "XWiki", "Alice");

    private static final List<String> LANGUAGES = List.of("en", "fr", "de");

    private static final List<String> PROPERTIES = List.of("subject", "text", "html");

    private static final Pattern VELOCITY_REFERENCE = Pattern.compile("\\$!?\\{?([A-Za-z_]\\w*(?:\\.\\w+)*)");

    private static final String SERVER_NAME = "www.example.org";

    private static final String RECIPIENT_NAME = "<b>Bob</b>";

    private static final String TITLE = "<em>Title</em>";

    private static final String MESSAGE = "<i>Hi</i>";

    private XWikiDocument sharePage;

    private final TestWatchListScriptService watchListScriptService = new TestWatchListScriptService();

    /**
     * Stands for the WatchList script service, which is only used by the mail templates to know whether the WatchList
     * is enabled.
     */
    public static class TestWatchListScriptService implements ScriptService
    {
        private boolean enabled;

        /**
         * @return {@code true} if the WatchList is enabled
         */
        public boolean isEnabled()
        {
            return this.enabled;
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        this.sharePage = loadPage(SHARE_PAGE);
        this.componentManager.registerComponent(ScriptService.class, "watchlist", this.watchListScriptService);
        // The HTML mails link to the wiki home page, which $xwiki.getDocument only returns when it's viewable.
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("view"), any(), any(), any())).thenReturn(true);

        XWikiDocument sharedPage = this.xwiki.getDocument(SHARED_PAGE, this.context);
        sharedPage.setTitle(TITLE);
        sharedPage.setSyntax(Syntax.XWIKI_2_1);
        sharedPage.setContent("Shared content");
        this.xwiki.saveDocument(sharedPage, this.context);
        this.context.setDoc(sharedPage);
        this.context.setUserReference(SENDER);
        this.stubRequest.setServerName(SERVER_NAME);
        this.stubRequest.setrequestURL(new StringBuffer("http://" + SERVER_NAME + "/xwiki/bin/view/Space/Page"));
        // The URL factory computes the server URL from the request when it's created.
        this.context.setURLFactory(new XWikiServletURLFactory(this.context));
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de" })
    void htmlMail(String language) throws Exception
    {
        String html = evaluate(language, "html");
        assertEvaluated(html);

        Document document = Jsoup.parseBodyFragment(html);
        // Values coming from the page and from the share form are displayed as they are, never interpreted as HTML.
        assertTrue(document.select("b, em, i").isEmpty(), html);
        assertEquals(TITLE, document.selectFirst("h1").text());
        assertTrue(document.selectFirst("p").text().contains(RECIPIENT_NAME), html);
        assertEquals(MESSAGE, document.selectFirst("pre").text());
        assertEquals(TITLE, document.selectFirst("a[href$=/xwiki/bin/view/Space/Page]").text());
        assertTrue(document.text().contains("Shared content"), html);
        String footer = document.select("p").last().text();
        assertTrue(footer.contains(SERVER_NAME) && footer.contains("Alice"), footer);
        assertTrue(document.select("a[href*=xpage=watch]").isEmpty(), html);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de" })
    void textMail(String language) throws Exception
    {
        String text = evaluate(language, "text");
        assertEvaluated(text);

        List<String> lines = text.lines().toList();
        // A plain text mail displays the values as they are, without any HTML escaping.
        assertTrue(lines.get(0).endsWith(' ' + RECIPIENT_NAME + ','), text);
        assertTrue(text.contains('"' + TITLE + '"'), text);
        assertTrue(lines.contains(MESSAGE), text);
        assertTrue(text.contains("http://" + SERVER_NAME + "/xwiki/bin/view/Space/Page"), text);
        assertTrue(text.contains("Shared content"), text);
        String footer = lines.get(lines.size() - 1);
        assertTrue(footer.contains(SERVER_NAME) && footer.contains("Alice"), footer);
        assertFalse(text.contains("xpage=watch"), text);
        assertTrue(text.lines().noneMatch(line -> line.startsWith("\t") || line.startsWith(" ")), text);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de" })
    void subject(String language) throws Exception
    {
        String subject = evaluate(language, "subject");

        assertEvaluated(subject);
        assertTrue(subject.startsWith("Alice "), subject);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de" })
    void watchListLinkWhenEnabled(String language) throws Exception
    {
        this.watchListScriptService.enabled = true;

        assertNotNull(Jsoup.parseBodyFragment(evaluate(language, "html")).selectFirst("a[href*=xpage=watch]"));
        assertTrue(evaluate(language, "text").contains("xpage=watch"));
    }

    /**
     * The mail templates of all languages must share the same Velocity code and differ only by their text, so that a
     * change made to one of them is not forgotten in the others.
     */
    @Test
    void sameVelocityCodeInAllLanguages()
    {
        for (String property : PROPERTIES) {
            String reference = getMailProperty("en", property);
            for (String language : LANGUAGES) {
                String content = getMailProperty(language, property);
                assertEquals(getDirectives(reference), getDirectives(content),
                    String.format("Velocity directives of [%s] in [%s]", property, language));
                assertEquals(getReferences(reference), getReferences(content),
                    String.format("Velocity references of [%s] in [%s]", property, language));
            }
        }
    }

    private String getMailProperty(String language, String property)
    {
        BaseObject mail = this.sharePage.getXObjects(MAIL_CLASS).stream()
            .filter(object -> object != null && language.equals(object.getStringValue("language")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No mail template for language " + language));
        return mail.getStringValue(property);
    }

    private static List<String> getDirectives(String content)
    {
        return content.lines().map(String::trim).filter(line -> line.startsWith("#")).toList();
    }

    private static List<String> getReferences(String content)
    {
        List<String> references = new ArrayList<>();
        Matcher matcher = VELOCITY_REFERENCE.matcher(content);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        references.sort(null);
        return references;
    }

    /**
     * Evaluates a mail template property the way the mail sender does, with the variables set by
     * {@code shareinline.vm}, each property getting its own child Velocity context.
     */
    private String evaluate(String language, String property) throws Exception
    {
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        VelocityContext velocityContext = new VelocityContext(velocityManager.getVelocityContext());
        velocityContext.put("doc", this.context.getDoc().newDocument(this.context));
        velocityContext.put("recipientName", RECIPIENT_NAME);
        velocityContext.put("message", MESSAGE);
        velocityContext.put("display", "inline");

        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityContext, writer, property,
            getMailProperty(language, property));
        return writer.toString();
    }

    private static void assertEvaluated(String output)
    {
        assertFalse(output.contains("$"), output);
        assertFalse(output.contains("#if") || output.contains("#set") || output.contains("#end"), output);
    }
}
