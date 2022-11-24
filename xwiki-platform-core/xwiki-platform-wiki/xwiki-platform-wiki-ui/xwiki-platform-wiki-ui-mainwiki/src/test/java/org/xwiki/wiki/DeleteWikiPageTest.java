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
package org.xwiki.wiki;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of the {@code WikiManager.DeleteWiki} page.
 *
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.7
 * @since 14.10
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@ComponentList({
    ErrorMessageMacro.class,
    TranslationMacro.class,
    TestNoScriptMacro.class,
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class
})
class DeleteWikiPageTest extends PageTest
{
    @Test
    void unknownWikiId() throws Exception
    {
        this.request.put("wikiId", "\" /}}{{noscript/}}");
        Document document = renderHTMLPage(new DocumentReference("xwiki", "WikiManager", "DeleteWiki"));
        assertEquals("platform.wiki.error.wikidoesnotexist [\" /}}{{noscript/}}]",
            document.select(".box.errormessage").text());
    }
}
