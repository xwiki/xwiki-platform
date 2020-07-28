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
package org.xwiki.flamingo.test.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the section editing feature.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@UITest
public class SectionEditIT
{
    private static final String MAIN_PAGENAME = "SectionEditing";
    private static final String INCLUDED_PAGENAME = "SectionEditingIncluded";

    private Set<DocumentReference> createdPages;

    @BeforeEach
    public void setup(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        this.createdPages = new HashSet<>();
    }

    @AfterEach
    public void after(TestUtils testUtils) throws Exception
    {
        for (DocumentReference createdPage : this.createdPages) {
            testUtils.rest().delete(createdPage);
        }
        testUtils.setWikiPreference("multilingual", "false");
        testUtils.setWikiPreference("default_language", "en");
        testUtils.setWikiPreference("languages", "en");
    }

    private ViewPage createTestPages(TestUtils setup, SpaceReference testReference, String syntaxId)
    {
        ViewPage result;
        DocumentReference mainPageReference = new DocumentReference(MAIN_PAGENAME, testReference);
        if (syntaxId.equalsIgnoreCase("xwiki/1.0")) {
            result = setup.createPage(mainPageReference, "1 Section1\nContent1\n\n"
                + "1 Section2\nContent2\n\n1.1 Section3\nContent3\n\n"
                + "1 Section4\nContent4", "section test in " + syntaxId, syntaxId);
            this.createdPages.add(mainPageReference);

        } else if (syntaxId.startsWith("xwiki/2.")) {
            DocumentReference includedReference = new DocumentReference(INCLUDED_PAGENAME, testReference);
            setup.createPage(includedReference, "== Section4 ==\n" +
                "Content4\n" +
                "\n" +
                "{{velocity wiki=true}}\n" +
                "#foreach($h in ['5', '6'])\n" +
                "== Section$h ==\n" +
                "Content$h\n" +
                "#end\n" +
                "{{velocity}}", "section test included in " + syntaxId, syntaxId);
            this.createdPages.add(includedReference);

            result = setup.createPage(mainPageReference,
                String.format("= Section1 =\nContent1\n\n"
                    + "= Section2 =\nContent2\n\n== Section3 ==\nContent3\n\n"
                    + "{{include document='%s'/}}\n\n"
                    + "= Section7 =\nContent7", includedReference.toString()),
                "section test in " + syntaxId, syntaxId);
            this.createdPages.add(mainPageReference);
        } else {
            throw new RuntimeException("Unhandled syntax [" + syntaxId + "]");
        }

        return result;
    }

    private void sectionEditInWikiEditorWhenSyntax2x(TestUtils setup, SpaceReference testReference, String syntaxId)
    {
        ViewPage vp = createTestPages(setup, testReference, syntaxId);

        // Edit the second section in the wiki editor
        WikiEditPage wikiEditPage = vp.editSection(2).editWiki();
        DocumentReference includedReference = new DocumentReference(INCLUDED_PAGENAME, testReference);

        assertEquals(String.format("= Section2 =\n\n"
            + "Content2\n\n"
            + "== Section3 ==\n\n"
            + "Content3\n\n"
            + "{{include document=\"%s\"/}}", includedReference.toString()), wikiEditPage.getContent());
        vp = wikiEditPage.clickCancel();

        // Edit the third section in the wiki editor
        wikiEditPage = vp.editSection(3).editWiki();
        assertEquals(String.format("== Section3 ==\n\n"
                + "Content3\n\n"
                + "{{include document=\"%s\"/}}", includedReference.toString()),
            wikiEditPage.getContent());
        vp = wikiEditPage.clickCancel();

        // Edit the fourth section in the wiki editor
        // Note: we prove that included documents don't generate editable sections by checking that the fourth section
        // is "Section7".
        wikiEditPage = vp.editSection(4).editWiki();
        assertEquals("= Section7 =\n\n"
            + "Content7", wikiEditPage.getContent());
    }

    private void setLanguageSettings(TestUtils setup, boolean isMultiLingual, String defaultLanguage,
        String supportedLanguages) throws Exception
    {
        setup.setWikiPreference("multilingual", Boolean.toString(isMultiLingual));
        setup.setWikiPreference("default_language", defaultLanguage);
        setup.setWikiPreference("languages", supportedLanguages);
    }

    /**
     * Verify edit section is working in both wiki and wysiwyg editors (xwiki/2.0 and xwiki/2.1).
     *
     * Note that we currently don't support section editing for included content (it would mean navigating to the
     * included page since it would change that page's content and not the currently page's content).
     *
     * See XWIKI-2881: Implement Section editing.
     */
    @Test
    @Order(1)
    public void sectionEditInWikiEditorWhenSyntax2x(TestUtils setup, TestReference testReference)
    {
        sectionEditInWikiEditorWhenSyntax2x(setup, testReference.getLastSpaceReference(), "xwiki/2.0");
        sectionEditInWikiEditorWhenSyntax2x(setup, testReference.getLastSpaceReference(), "xwiki/2.1");
    }

    /**
     * Verify section save does not override the whole document content (xwiki/2.0).
     * See XWIKI-4033: When saving after section edit entire page is overwritten.
     */
    @Test
    @Order(2)
    public void sectionSaveDoesNotOverwriteTheWholeContentWhenSyntax20(TestUtils setup, TestReference testReference)
    {
        ViewPage vp = createTestPages(setup, testReference.getLastSpaceReference(), "xwiki/2.0");
        vp.editSection(4).editWiki().clickSaveAndView();
        WikiEditPage wep = vp.editWiki();
        DocumentReference includedReference =
            new DocumentReference(INCLUDED_PAGENAME, testReference.getLastSpaceReference());
        assertEquals(String.format("= Section1 =\n\n"
            + "Content1\n\n"
            + "= Section2 =\n\n"
            + "Content2\n\n"
            + "== Section3 ==\n\n"
            + "Content3\n\n"
            + "{{include document=\"%s\"/}}\n\n"
            + "= Section7 =\n\n"
            + "Content7", includedReference.toString()), wep.getContent());
    }

    /**
     * Verify that the document title is not overwritten when saving a section.
     *
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-8938">XWIKI-8938: Translated title is overwritten by the default
     *      translation title when editing a document section</a>
     */
    @Test
    @Order(3)
    public void testSectionSaveDoesNotOverwriteTheTitle(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create the English version.
        setLanguageSettings(setup, false, "en", "en");
        setup.rest().savePage(testReference, "Original content", "Original title");
        this.createdPages.add(testReference);

        try {
            // Create the French version.
            setLanguageSettings(setup, true, "en", "en,fr");
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("language", "fr");
            parameters.put("title", "Translated title");
            parameters.put("content", "= Chapter 1 =\n\n Once upon a time ...");
            setup.gotoPage(testReference, "save", parameters);

            // Switch back to monolingual with French as default language.
            setLanguageSettings(setup, false, "fr", "fr");

            // Edit and save a document section and check if the document title was overwritten.
            setup.gotoPage(testReference, "edit", "editor=wiki&section=1");
            assertEquals("Translated title", new WikiEditPage().clickSaveAndView().getDocumentTitle());
        } finally {
            // Restore language settings.
            setLanguageSettings(setup, false, "en", "en");
        }
    }
}
