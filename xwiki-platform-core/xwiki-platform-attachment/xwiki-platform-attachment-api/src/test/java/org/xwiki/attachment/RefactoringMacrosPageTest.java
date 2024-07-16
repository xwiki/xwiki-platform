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
package org.xwiki.attachment;

import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.attachment.script.AttachmentScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of the macros provided by {@code refactoring_macros.vm}.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateScriptService.class
})
class RefactoringMacrosPageTest extends PageTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @ParameterizedTest
    @CsvSource({
        "false,false,true",
        "false,true,false",
        "true,false,false",
        "true,true, false"
    })
    void displayAttachmentLinksCheckboxIsHidden(boolean isAdvancedUser, boolean isSuperAdmin, boolean expectHidden)
        throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context);
        document.setContent(String.format("{{velocity}}\n"
                + "#template('attachment/refactoring_macros.vm')\n"
                + "#set($isAdvancedUser = %s)\n"
                + "#set($isSuperAdmin = %s)\n"
                + "{{html}}#displayAttachmentLinksCheckbox(){{/html}}\n"
                + "{{/velocity}}",
            isAdvancedUser, isSuperAdmin));
        document.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(document, this.context);
        Document content = Jsoup.parse(document.getRenderedContent(this.context));
        assertEquals(expectHidden, content.select("dt").hasClass("hidden"));
        assertEquals(expectHidden, content.select("dd").hasClass("hidden"));
    }

    @ParameterizedTest
    @MethodSource("displayAttachmentLinksCheckboxSource")
    void displayAttachmentLinksCheckbox(String updateReferences, String expectedChecked)
        throws Exception
    {
        AttachmentScriptService attachmentScriptService = mock(AttachmentScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "attachment", attachmentScriptService);
        when(attachmentScriptService.backlinksCount(any())).thenReturn(10L);

        XWikiDocument document = this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context);
        document.setContent("{{velocity}}\n"
            + "#template('attachment/refactoring_macros.vm')\n"
            + "{{html}}#displayAttachmentLinksCheckbox(){{/html}}\n"
            + "{{/velocity}}");
        document.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(document, this.context);

        this.request.put("updateReferences", updateReferences);

        Document parse = Jsoup.parse(document.getRenderedContent(this.context));
        // The checkbox is checked by default, unless updateReferences is defined to value different than 'true'.
        assertEquals(expectedChecked, parse.select("[name='updateReferences']").attr("checked"));
        assertEquals("attachment.move.links.hint [10]", parse.select(".xHint").text());
    }

    /**
     * @return the arguments for {@link #displayAttachmentLinksCheckbox(String, String)}
     */
    private static Stream<Arguments> displayAttachmentLinksCheckboxSource()
    {
        return Stream.of(
            Arguments.of(null, "checked"),
            Arguments.of("", ""),
            Arguments.of("false", ""),
            Arguments.of("true", "checked")
        );
    }
}
