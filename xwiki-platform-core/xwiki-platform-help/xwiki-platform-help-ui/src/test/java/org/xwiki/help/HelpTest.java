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
package org.xwiki.help;

import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.block.BlockMatcherConverter;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.plain.Plain10SyntaxProvider;
import org.xwiki.rendering.internal.renderer.plain.PlainTextBlockRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for testing the {@code XWiki.XWikiSyntax} wiki page.
 *
 * @version $Id$
 * @since 8.3M2
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    BlockMatcherConverter.class,
    Plain10SyntaxProvider.class,
    PlainTextBlockRenderer.class,
    PlainTextRenderer.class,
    PlainTextRendererFactory.class
})
class HelpTest extends PageTest
{
    private static final DocumentReference XWIKI_SYNTAX_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiSyntax");

    private static final String SPECIAL_VALUE = "\"/}}(% id=\"special\" %)**special**";

    private static final String SECTION_CONTENT = "= Category =\n\n== Section ==";

    private static final String SPECIAL_SYNTAX = "~{~{/html~}~}~*~*special~*~*";

    private ScriptQuery syntaxSectionsQuery;

    @BeforeEach
    void setUp() throws Exception
    {
        // Register query script service since it's not registered by default, and it's used in XWiki.XWikiSyntax to
        // find all syntax pages.
        QueryManagerScriptService qss = mock(QueryManagerScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "query", qss);
        this.syntaxSectionsQuery = mock(ScriptQuery.class);
        when(qss.xwql(contains("from doc.object(XWiki.XWikiSyntaxClass)"))).thenReturn(this.syntaxSectionsQuery);
        when(this.syntaxSectionsQuery.addFilter(anyString())).thenReturn(this.syntaxSectionsQuery);
    }

    /**
     * The bug we're trying to prevent happening again was that there was that "$subHeading" was rendered when going to
     * the Links section (for example).
     * Note: It was working fine when displaying all sections though.
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-13650">XWIKI-13650</a>
     */
    @Test
    void verifySubHeadingVelocityVariableCorrectlyEvaluatedWhenUsedInSection() throws Exception
    {
        // URL that we're simulating:
        //   http://localhost:8080/xwiki/bin/view/XWiki/XWikiSyntax?syntax=2.1&section=Links&xpage=print
        setOutputSyntax(Syntax.HTML_5_0);
        request.put("section", "Links");
        request.put("xpage", "print");

        // Register the XWikiSyntaxLinks page with an XWikiSyntaxClass xobject in it so that it can be found later on
        // by XWiki.XWikiSyntax
        loadPage(new DocumentReference("xwiki", "XWiki", "XWikiSyntaxLinks"));
        when(this.syntaxSectionsQuery.execute()).thenReturn(Arrays.asList("XWiki.XWikiSyntaxLinks"));

        String result = renderPage(XWIKI_SYNTAX_REFERENCE);
        assertThat("$subHeading should have been evaluated and replaced by '==='", result,
            containsString("<h3 id=\"HXWikiSyntax2.1LinkSpecification\""));
        assertThat("$subHeading should have been evaluated and replaced by '==='", result,
            not(containsString("$subHeading")));
    }

    @Test
    void displaySectionLimitedToOlderSyntax() throws Exception
    {
        setOutputSyntax(Syntax.HTML_5_0);
        this.request.put("syntax", "2.1");
        this.request.put("section", "GeneralRemarks");
        loadPage(new DocumentReference("xwiki", "XWiki", "XWikiSyntaxGeneralRemarks"));
        when(this.syntaxSectionsQuery.execute()).thenReturn(List.of("XWiki.XWikiSyntaxGeneralRemarks"));

        Document document = renderHTMLPage(XWIKI_SYNTAX_REFERENCE);

        assertNotNull(document.getElementById("H2.0GeneralRemarks"));
    }

    @Test
    void displaySectionWithSpecialMaxSyntaxId() throws Exception
    {
        createSyntaxSection(new DocumentReference("xwiki", "Sandbox", "XWikiSyntaxSection"), SPECIAL_VALUE,
            SECTION_CONTENT);

        Document document = renderHTMLPage(XWIKI_SYNTAX_REFERENCE);

        assertNull(document.getElementById("special"));
    }

    @Test
    void displaySectionWithSpecialName() throws Exception
    {
        createSyntaxSection(new DocumentReference("xwiki", "Sandbox", "XWikiSyntaxSection" + SPECIAL_VALUE), "2.1",
            SECTION_CONTENT);

        Document document = renderHTMLPage(XWIKI_SYNTAX_REFERENCE);

        assertNull(document.getElementById("special"));
    }

    @ParameterizedTest
    @CsvSource({
        ",",
        "category, 0",
        "section, Section"
    })
    void displaySectionWithSyntaxInHeadings(String parameterName, String parameterValue) throws Exception
    {
        if (parameterName != null) {
            this.request.put(parameterName, parameterValue);
        }
        // XWiki.XWikiSyntax needs programming rights to access the XDOM of the syntax pages.
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.context)).thenReturn(true);
        // The escaped syntax of the headings is rendered as plain text when computing the category and section names.
        createSyntaxSection(new DocumentReference("xwiki", "XWiki", "XWikiSyntaxSection"), "2.1",
            "= Category " + SPECIAL_SYNTAX + " =\n\n== Section " + SPECIAL_SYNTAX + " ==");

        Document document = renderHTMLPage(XWIKI_SYNTAX_REFERENCE);

        assertTrue(document.select("strong").isEmpty());
        assertThat(document.text(), containsString("Section {{/html}}**special**"));
    }

    private void createSyntaxSection(DocumentReference sectionReference, String maxSyntaxId, String content)
        throws Exception
    {
        loadPage(new DocumentReference("xwiki", "XWiki", "XWikiSyntaxClass"));
        XWikiDocument sectionDocument = this.xwiki.getDocument(sectionReference, this.context);
        sectionDocument.setSyntax(Syntax.XWIKI_2_1);
        sectionDocument.setContent(content);
        BaseObject sectionObject = sectionDocument.newXObject(
            new DocumentReference("xwiki", "XWiki", "XWikiSyntaxClass"), this.context);
        sectionObject.setIntValue("category", 99);
        sectionObject.setIntValue("section", 0);
        sectionObject.setStringValue("minSyntaxId", "1.0");
        sectionObject.setStringValue("maxSyntaxId", maxSyntaxId);
        this.xwiki.saveDocument(sectionDocument, this.context);
        when(this.syntaxSectionsQuery.execute()).thenReturn(List.of(sectionDocument.getPrefixedFullName()));
    }
}
