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
package org.xwiki.vfs;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.WikiMacroFactoryComponentClass;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xwiki.test.page.WikiMacroSetup.loadWikiMacro;

/**
 * Page test of the {@code Macros.VFSTreeMacro} wiki macro.
 *
 * @version $Id$
 */
@ComponentList({
    TestTreeMacro.class
})
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@XWikiSyntax21ComponentList
@HTML50ComponentList
@WikiMacroFactoryComponentClass
class VFSTreeMacroPageTest extends PageTest
{
    private static final DocumentReference PAGE_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @BeforeEach
    void setUp() throws Exception
    {
        loadWikiMacro(this, this.componentManager, new DocumentReference("xwiki", "Macros", "VFSTreeMacro"));
    }

    @Test
    void rootIsPassedToTheTreeMacro() throws Exception
    {
        // The root contains characters that are special in XWiki 2.1 macro parameters, to check that the root passed
        // to the vfsTree macro reaches the tree macro unchanged.
        XWikiDocument xwikiDocument = this.xwiki.getDocument(PAGE_REFERENCE, this.context);
        xwikiDocument.setSyntax(Syntax.XWIKI_2_1);
        xwikiDocument.setContent("{{vfsTree root=\"attach:Space.Page@my ~\"archive~\"~~1.zip\"/}}");
        this.xwiki.saveDocument(xwikiDocument, this.context);
        // The macro uses $doc to compute the URL of the tree data source.
        this.context.setDoc(xwikiDocument);

        Document document = renderHTMLPage(xwikiDocument);

        Element tree = document.selectFirst(".tree-stub");
        assertNotNull(tree);
        assertEquals("attach:Space.Page@my \"archive\"~1.zip", tree.attr("data-root"));
        assertEquals("path:/xwiki/bin/get/Space/Page?sheet=Macros.VFSTreeJSON&outputSyntax=plain",
            tree.attr("data-reference"));
        assertEquals("true", tree.attr("data-links"));
    }
}
