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

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Page Test of the {@code XWiki.XWikiSyntaxMacrosList} document.
 *
 * @version $Id$
 * @since 16.4RC1
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@ComponentList({
    // Start - Required in addition of RenderingScriptServiceComponentList
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // End - Required in additional of RenderingScriptServiceComponentList
    ControlCharactersFilter.class
})
class XWikiSyntaxMacrosListPageTest extends PageTest
{
    public static final DocumentReference
        DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiSyntaxMacrosList");

    @Test
    void renderTable() throws Exception
    {
        // TODO: load at least one Wiki Macro and factorize duplicated code with AttachmentSelectorPageTest.
        Document document = renderHTMLPage(DOCUMENT_REFERENCE);
        System.out.println(document);
        assertNotNull(document);
    }
}
