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
package org.xwiki.flamingo;

import java.util.Locale;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code FlamingoThemesCode.WebHomeSheet} page.
 *
 * @version $Id$
 * @since 13.10.10
 * @since 14.4.6
 * @since 14.9RC1
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
class WebHomeSheetPageTest extends PageTest
{
    @Test
    void createAction() throws Exception
    {
        this.request.put("newThemeName", "some content\"/}}{{noscript/}}");
        this.request.put("form_token", "1");
        this.request.put("action", "create");

        TranslationBundleContext translationBundleContext = this.componentManager
            .getInstance(TranslationBundleContext.class);
        TranslationBundle translationBundle = mock(TranslationBundle.class);
        Translation translation = mock(Translation.class);
        when(translation.getLocale()).thenReturn(Locale.ENGLISH);
        when(translation.render(any(), any())).thenAnswer(invocationOnMock -> new WordBlock(
            "platform.flamingo.themes.home.create.csrf " + invocationOnMock.getArgument(1)));
        when(translationBundle.getTranslation(eq("platform.flamingo.themes.home.create.csrf"), any()))
            .thenReturn(translation);
        translationBundleContext.addBundle(translationBundle);

        Document document = this.renderHTMLPage(new DocumentReference("xwiki", "FlamingoThemesCode", "WebHomeSheet"));

        assertEquals("platform.flamingo.themes.home.create.csrf some content\"/}}{{noscript/}}",
            document.select(".box.errormessage").text());
    }
}
