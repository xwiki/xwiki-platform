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
package org.xwiki.test.webstandards;

import org.jsoup.nodes.Element;
import org.xwiki.test.webstandards.framework.DocumentReferenceTarget;
import org.xwiki.test.webstandards.framework.Target;
import org.xwiki.validator.HTML5DutchWebGuidelinesValidator;
import org.xwiki.validator.ValidationError.Type;

public class CustomDutchWebGuidelinesValidator extends HTML5DutchWebGuidelinesValidator
{
    private static final String SPACE_META = "space";

    private Target target;

    /**
     * Set the target being analyzed.
     * 
     * @param target the target
     */
    public void setTarget(Target target)
    {
        this.target = target;
    }

    /**
     * @param metaName name of the meta to get
     * @return the value for the given meta
     */
    private String getMeta(String metaName)
    {
        for(Element meta : this.html5Document.getElementsByTag(ELEM_META)) {
            if (metaName.equals(meta.attr("name"))) {
                return meta.attr(ATTR_CONTENT);
            }
        }
        return null;
    }

    /**
     * @return true if the current page is the give page, false otherwise.
     */
    private boolean isPage(String space, String page)
    {
        boolean isPage;

        if (target instanceof DocumentReferenceTarget) {
            DocumentReferenceTarget documentReferenceTarget = (DocumentReferenceTarget) target;

            isPage =
                documentReferenceTarget.getDocumentReference().getName().equals(page)
                    && documentReferenceTarget.getDocumentReference().getLastSpaceReference().getName().equals(space);
        } else {
            isPage = false;
        }

        return isPage;
    }

    /**
     * @return true if it's a translation document.
     */
    private boolean isTranslationDocument()
    {
        if (target instanceof DocumentReferenceTarget) {
            DocumentReferenceTarget documentReferenceTarget = (DocumentReferenceTarget) target;

            return documentReferenceTarget.getDocumentReference().getName().contains("Translations");
        } else {
            return false;
        }
    }

    /**
     * Use the p (paragraph) element to indicate paragraphs. Do not use the br (linebreak) element to separate
     * paragraphs.
     */
    @Override
    public void validateRpd3s4()
    {
        // Exclude translation documents as they are using plain syntax and line breaking are then translated with
        // potentially multiple br tags. Now those translations pages are not supposed to be watched by the users,
        // so we can ignore them.
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("XWiki", "XWikiSyntaxParagraphs")
            && !isPage("XWiki", "XWikiSyntaxGeneralRemarks") && !isTranslationDocument()) {
            super.validateRpd3s4();
        }
    }

    /**
     * Avoid using the sup (superscript) and sub (subscript) element if possible. XWiki exception: wiki syntax allows
     * using sub and sup tags, this usage is demonstrated in the Sandbox space and the XWiki.XWikiSyntax page.
     */
    @Override
    public void validateRpd3s9()
    {
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("XWiki", "XWikiSyntaxTextFormatting")
            && !isPage("XWiki", "XWikiSyntaxTextFormatting") && !isPage("Sandbox", "WebHome")) {
            super.validateRpd3s9();
        }
    }

    /**
     * Use ol (ordered list) and ul (unordered list) elements to indicate lists. XWiki exception: XWiki.XWikiSyntax
     * shows the wiki syntax to use to create lists, this syntax is precisely what's forbidden to use in the generated
     * html.
     */
    @Override
    public void validateRpd3s13()
    {
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("XWiki", "XWikiSyntaxHeadings")) {
            super.validateRpd3s13();
        }
    }

    /**
     * CSS should be placed in linked files and not mixed with the HTML source code. XWiki exceptions: in the ColorTheme
     * application we have to allow the use of inline styles, this is the only way to offer a preview of the themes
     * color. In XWiki.XWikiSyntax usage of style custom parameter is demonstrated. In Panels.PanelWizard and
     * XWiki.Treeview the use of JS libraries make the use of inline styles mandatory.
     */
    @Override
    public void validateRpd9s1()
    {
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("XWiki", "XWikiSyntaxHorizontalLine")
            && !isPage("XWiki", "XWikiSyntaxTables") && !isPage("XWiki", "XWikiSyntaxLinks")
            && !isPage("XWiki", "XWikiSyntaxParagraphs") && !isPage("XWiki", "XWikiSyntaxMacros")
            && !isPage("XWiki", "XWikiSyntaxDefinitionLists") && !isPage("XWiki", "XWikiSyntaxHeadings")
            && !isPage("XWiki", "XWikiSyntaxLists") && !isPage("XWiki", "XWikiSyntaxParameters")
            && !isPage("XWiki", "XWikiSyntaxGroups") && !isPage("XWiki", "Treeview")
            && !isPage("Panels", "PanelWizard") && !isPage("Invitation", "WebHome")) {
            // Usage of the style attribute is strictly forbidden in the other spaces.

            assertTrue(Type.ERROR, "rpd9s1.attr", getElement(ELEM_BODY).getElementsByAttribute(STYLE).isEmpty());
        }

        // <style> tags are forbidden everywhere.
        assertTrue(Type.ERROR, "rpd9s1.tag", getElement(ELEM_BODY).getElementsByTag(STYLE).isEmpty());
    }

    /**
     * Use the scope attribute to associate table labels (th cells) with columns or rows. XWiki exception: wiki syntax
     * allows using table headers without scope, this usage is demonstrated in the Sandbox space and the
     * XWiki.XWikiSyntax page.
     */
    @Override
    public void validateRpd11s4()
    {
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("Sandbox", "WebHome")) {
            super.validateRpd11s4();
        }
    }

    /**
     * Use the headers and id attributes to associate table labels (th cells) with individual cells in complex tables.
     * XWiki exception: wiki syntax allows using tables without headers, this usage is demonstrated in the Sandbox space
     * and the XWiki.XWikiSyntax page.
     */
    @Override
    public void validateRpd11s5()
    {
        if (!isPage("XWiki", "XWikiSyntax") && !isPage("Sandbox", "WebHome")) {
            super.validateRpd11s5();
        }
    }
}
