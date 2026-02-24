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
package org.xwiki.blocknote.test.ui;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edit and save to check that the wiki syntax is preserved.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@UITest
class RoundTripIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void textFormat(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            one **two** //three// __four__ --five-- ,,six,, ^^seven^^ ##eight## nine

            __//--**,,##^^all^^##,,**--//__""",
            // The content is modified on save:
            // * subscript and superscript styles are lost because they are not supported by the editor
            // * the order of the nested styles is normalized because the editor stores text styles in a flat structure
            // * the editor outputs default paragraph styles
            """
                (% style="color:default;background-color:default;text-align:left" %)
                one **two** //three// __four__ --five-- six seven ##eight## nine

                (% style="color:default;background-color:default;text-align:left" %)
                **//__--##all##--__//**""");
    }

    @Test
    @Order(2)
    void customParameters(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            (% data-bar="foo" %)
            one (% data-foo="bar" %)two(%%) three
            """,
            // The content is modified on save:
            // * custom parameters are lost because they are not supported by the editor
            // * the editor outputs default paragraph styles
            """
                (% style="color:default;background-color:default;text-align:left" %)
                one two three""");
    }

    @Test
    @Order(3)
    void headings(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            = Heading 1 with **bold** and //italic// =

            == Heading 2 ==

            === Heading 3 ===

            ==== Heading 4 ====

            ===== Heading 5 =====

            ====== Heading 6 ======""",
            // The content is modified on save:
            // * the editor outputs default heading styles
            """
                (% style="color:default;background-color:default;text-align:left" %)
                = Heading 1 with **bold** and //italic// =

                (% style="color:default;background-color:default;text-align:left" %)
                == Heading 2 ==

                (% style="color:default;background-color:default;text-align:left" %)
                === Heading 3 ===

                (% style="color:default;background-color:default;text-align:left" %)
                ==== Heading 4 ====

                (% style="color:default;background-color:default;text-align:left" %)
                ===== Heading 5 =====

                (% style="color:default;background-color:default;text-align:left" %)
                ====== Heading 6 ======""");
    }

    @Test
    @Order(4)
    void emptyLines(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            first


            second""",
            // The content is modified on save:
            // * the empty line is converted to a paragraph on load but it isn't converted back to an empty line on save
            // because the editor outputs default paragraph styles (so it's not an empty line anymore)
            """
                (% style="color:default;background-color:default;text-align:left" %)
                first

                (% style="color:default;background-color:default;text-align:left" %)


                (% style="color:default;background-color:default;text-align:left" %)
                second""");
    }

    @Test
    @Order(5)
    void verbatim(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            one {{{**two**}}} three

            {{{
            not **bold**
            }}}""",
            // The content is modified on save:
            // * the editor doesn't support inline verbatim so it outputs an escape sequence
            // * the editor outputs default verbatim parameter values
            // * the editor outputs default paragraph styles
            """
                (% style="color:default;background-color:default;text-align:left" %)
                one ~*~*two~*~* three

                (% data-xwiki-verbatim-language="" %)
                {{{
                not **bold**
                }}}""");
    }

    @Test
    @Order(6)
    void blockquote(TestUtils setup, TestReference testReference) throws Exception
    {
        // Minimal test for now because we have the following issues:
        // XWIKI-24011: Fail to edit a quote with multiple child blocks
        // XWIKI-24012: Failed to save nested quote
        roundTrip(setup, testReference, """
            > one""",
            // The content is modified on save:
            // * the editor outputs default blockquote styles
            """
                (% style="color:default;background-color:default" %)
                > one""");
    }

    @Test
    @Order(7)
    void unorderedList(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            * one
            ** two
            *** three
            * done""");
    }

    @Test
    @Order(8)
    void orderedList(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            (% start="5" %)
            1. one
            11. two
            111. three
            1. done""",
            // The content is modified on save:
            // * the editor doesn't support setting the start value of ordered lists
            """
                1. one
                11. two
                111. three
                1. done""");
    }

    @Test
    @Order(9)
    void definitionList(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            ; name
            : John
            ; age
            : 27""",
            // The content is modified on save:
            // * the editor doesn't support definition lists os we convert them to unordered lists
            """
                * name
                * John
                * age
                * 27""");
    }

    @Test
    @Order(10)
    void table(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            |=|=One|=Two
            |=Three|1.3|2.3
            |=Four|1.4|2.4""",
            // The content is modified on save:
            // * header column is not supported
            // * the editor outputs default table and cell styles
            """
                (% style="color:default" %)
                |=(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)|=(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)One|=(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)Two
                |(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)Three|(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)1.3|(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)2.3
                |(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)Four|(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)1.4|(% style="color:default;background-color:default;text-align:left" colspan="1" rowspan="1" %)2.4""");
    }

    @Test
    @Order(11)
    void link(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference,
            """
                begin https://www.xwiki.org [[label>>Some.Page||anchor="Heading" queryString="foo=bar"]] attach:file.pdf [[Other.Page]] end""",
            // The content is modified on save:
            // * anchor and queryString parameters are lost; we can specify then only for external URLs
            // * links with generated labels are lost
            """
                (% style="color:default;background-color:default;text-align:left" %)
                begin  [[label>>Some.Page]]   end""");
    }

    @Test
    @Order(12)
    void image(TestUtils setup, TestReference testReference) throws Exception
    {
        // Note that inline images are currently not supported by BlockNote.
        roundTrip(setup, testReference,
            """
                [[Carol>>image:Path.To.Carol@avatar.png||alt="Carol's avatar" data-xwiki-image-style-alignment="center" width="150" height="100"]]

                image:icon:accept""",
            // The content is modified on save:
            // * the alt and height parameters are lost for some reason
            // * parameter order is changed
            // * information about freestanding images is lost
            // * the editor outputs default image styles
            """
                [[Carol>>image:Path.To.Carol@avatar.png||width="150" data-xwiki-image-style-alignment="center"]]

                [[image:icon:accept||data-xwiki-image-style-alignment="start"]]""");
    }

    @Test
    @Order(13)
    void div(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            (((
            group
            )))""",
            // The content is modified on save:
            // * the editor doesn't support DIV blocks so only their content is preserved
            """
                (% style="color:default;background-color:default;text-align:left" %)
                group""");
    }

    @Test
    @Order(14)
    void horizontalLine(TestUtils setup, TestReference testReference) throws Exception
    {
        // TODO: See XWIKI-24001: Dividers (horizontal rule) are not saved and content after them is lost
    }

    @Test
    @Order(15)
    void macro(TestUtils setup, TestReference testReference) throws Exception
    {
        roundTrip(setup, testReference, """
            before

            one {{html clean="false"}}two{{/html}} three

            empty {{html/}} macro

            {{velocity wiki="true"}}
            > quoted text
            {{/velocity}}

            {{include reference="Missing.Page"/}}

            after""",
            // The content is modified on save:
            // * the editor outputs the default paragraph styles
            """
                (% style="color:default;background-color:default;text-align:left" %)
                before

                (% style="color:default;background-color:default;text-align:left" %)
                one {{html clean="false"}}two{{/html}} three

                (% style="color:default;background-color:default;text-align:left" %)
                empty {{html/}} macro

                {{velocity wiki="true"}}
                > quoted text
                {{/velocity}}

                {{include reference="Missing.Page"/}}

                (% style="color:default;background-color:default;text-align:left" %)
                after""");
    }

    void roundTrip(TestUtils setup, TestReference testReference, String content) throws Exception
    {
        roundTrip(setup, testReference, content, content);
    }

    void roundTrip(TestUtils setup, TestReference testReference, String contentBefore, String contentAfter)
        throws Exception
    {
        setup.createPage(testReference, contentBefore, "");
        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();
        textArea.sendKeys(Keys.PAGE_DOWN, "end");

        // FIXME: XWIKI-23717: BlockNote's editing area fails accessibility tests
        page = disableWCAG(setup, page::save);
        WikiEditPage wikiEditor = page.editWiki();
        String ending = """


            (% style="color:default;background-color:default;text-align:left" %)
            end

            (% style="color:default;background-color:default;text-align:left" %)""";
        String content = wikiEditor.getContent();
        assertTrue(content.endsWith(ending), "The content inserted through the editor is missing.");
        assertEquals(contentAfter, Strings.CS.removeEnd(content, ending));
    }
}
