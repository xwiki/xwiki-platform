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
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Edit and save to check that the wiki syntax is preserved.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@UITest(
    properties = {
        // The Image Wizard needs this to be able to upload images.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
    },
    extraJARs = {
        // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
        "org.xwiki.platform:xwiki-platform-websocket",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",

        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    servletEngineNetworkAliases = AbstractBlockNoteIT.XWIKI_ALIAS
)
class RoundTripIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void textFormat(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            one **two** //three// __four__ --five-- ,,six,, ^^seven^^ ##eight## nine

            __//--**,,##^^all^^##,,**--//__""",
            // The content is modified on save:
            // * the order of the nested styles is normalized because the editor stores text styles in a flat structure
            """
                one **two** //three// __four__ --five-- ,,six,, ^^seven^^ ##eight## nine

                **//__--^^,,##all##,,^^--__//**""");
    }

    @Test
    @Order(2)
    void customParameters(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            (% data-bar="foo" %)
            one (% data-foo="bar" %)two(%%) three""");
    }

    @Test
    @Order(3)
    void headings(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            = Heading 1 with **bold** and //italic// =

            == Heading 2 ==

            (% id="HTest" %)
            === Heading 3 ===

            ==== Heading 4 ====

            ===== Heading 5 =====

            ====== Heading 6 ======""");
    }

    @Test
    @Order(4)
    void emptyLines(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            first


            second""");
    }

    @Test
    @Order(5)
    void verbatim(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            one {{{**two**}}} three (% data-foo="bar" %){{{__four__}}}(%%) five

            {{{
            not **bold**
            }}}

            (% data-color="green" data-xwiki-verbatim-language="json" %)
            {{{
            {"value": "no [[link]]"}
            }}}""",
            // The content is modified on save:
            // * XWiki doesn't render properly an inline verbatim block with parameters (it looks like the XWiki 2.1
            // renderer expects the parameters to be on a separate Format block, otherwise it thinks the inline verbatim
            // block is standalone)
            """
                one {{{**two**}}} three (% data-foo="bar" %)
                {{{__four__}}} five

                {{{
                not **bold**
                }}}

                (% data-color="green" data-xwiki-verbatim-language="json" %)
                {{{
                {"value": "no [[link]]"}
                }}}""");
    }

    @Test
    @Order(6)
    void blockquote(TestUtils setup, TestReference testReference)
    {
        // Minimal test for now because we have the following issues:
        // XWIKI-24011: Fail to edit a quote with multiple child blocks
        // XWIKI-24012: Failed to save nested quote
        roundTrip(setup, testReference, """
            >line1
            >lineOne
            (% data-foo="bar" %)
            >>line2
            >>lineTwo
            >>>line3
            >>>lineThree
            >>line4
            >>lineFour""");
    }

    @Test
    @Order(7)
    void unorderedList(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            * one
            ** two
            *** three
            * done""");
    }

    @Test
    @Order(8)
    void orderedList(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            (% start="5" %)
            1. one
            11. two
            111. three
            1. done""");
    }

    @Test
    @Order(9)
    void definitionList(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            ; name
            : John
            ; age
            : 27""");
    }

    @Test
    @Order(10)
    void table(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            |=|=One|=Two
            |=Three|1.3|2.3
            |=Four|1.4|2.4""");
    }

    @Test
    @Order(11)
    void link(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference,
            """
                begin https://www.xwiki.org [[label>>Some.Page||anchor="Heading" queryString="foo=bar"]] attach:file.pdf [[Other.Page]] end""",
            // The content is modified on save:
            // * anchor and queryString parameters are lost; we can specify then only for external URLs
            // * links with generated labels are lost
            """
                begin  [[label>>Some.Page]]   end""");
    }

    @Test
    @Order(12)
    void image(TestUtils setup, TestReference testReference)
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
    void div(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            (((
            group
            )))""");
    }

    @Test
    @Order(14)
    void horizontalLine(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            one

            ----

            two""");
    }

    @Test
    @Order(15)
    void macro(TestUtils setup, TestReference testReference)
    {
        roundTrip(setup, testReference, """
            before

            one {{html clean="false"}}two{{/html}} three

            empty {{html/}} macro

            {{velocity wiki="true"}}
            > quoted text
            {{/velocity}}

            {{include reference="Missing.Page"/}}

            after""");
    }

    /**
     * Insert the provided content in the provided page (testReference), edit the page with blocknote, save it back, and
     * verify that the content is not altered. If you expect the content to be modified, see
     * {@link #roundTrip(TestUtils, TestReference, String, String)}.
     *
     * @param setup the test setup
     * @param testReference the reference of the page to edit
     * @param content the initial content of the document
     * @see #roundTrip(TestUtils, TestReference, String, String)
     */
    private void roundTrip(TestUtils setup, TestReference testReference, String content)
    {
        roundTrip(setup, testReference, content, content);
    }

    /**
     * Insert the provided content (contentBefore) in the provided page (testReference), edit the page with blocknote, 
     * save it back, and
     * verify that the content is matching expectations (contentAfter). If you expect the content to be unaltered, 
     * see {@link #roundTrip(TestUtils, TestReference, String)}
     *
     * @param setup the test setup
     * @param testReference the reference of the page to edit
     * @param contentBefore the initial content
     * @param contentAfter the expected final content
     * @see #roundTrip(TestUtils, TestReference, String)
     */
    private void roundTrip(TestUtils setup, TestReference testReference, String contentBefore, String contentAfter)
    {
        setup.createPage(testReference, contentBefore, "");
        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();
        textArea.appendParagraph("end").waitUntilTextContains("end");

        page = page.save();
        WikiEditPage wikiEditor = page.editWiki();
        String ending = """


            end""";
        String content = wikiEditor.getContent();
        assertThat("The content inserted through the editor is missing.", content, endsWith(ending));
        assertEquals(contentAfter, Strings.CS.removeEnd(content, ending));
    }
}
