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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code FlamingoThemesCode.ThemeSheet} page.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    ModelScriptService.class
})
class ThemeSheetPageTest extends PageTest
{
    private static final DocumentReference THEME_SHEET =
        new DocumentReference("xwiki", "FlamingoThemesCode", "ThemeSheet");

    private static final DocumentReference THEME_CLASS =
        new DocumentReference("xwiki", "FlamingoThemesCode", "ThemeClass");

    @Mock
    private Macro<Object> attachmentSelectorMacro;

    private final List<String> attachmentSelectorFilters = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception
    {
        // Register a fake attachment selector macro that collects the file extensions it is asked to accept.
        when(this.attachmentSelectorMacro.supportsInlineMode()).thenReturn(true);
        when(this.attachmentSelectorMacro.getDescriptor()).thenReturn(
            new DefaultMacroDescriptor(new MacroId("attachmentSelector"), "Attachment Selector Macro"));
        when(this.attachmentSelectorMacro.execute(any(), any(), any())).then(invocation -> {
            this.attachmentSelectorFilters.add(invocation.getArgument(2, MacroTransformationContext.class)
                .getCurrentMacroBlock().getParameter("filter"));
            return Collections.emptyList();
        });
        this.componentManager.registerComponent(Macro.class, "attachmentSelector", this.attachmentSelectorMacro);
    }

    @Test
    void logoAttachmentSelectorFilter() throws Exception
    {
        loadPage(THEME_CLASS);
        XWikiDocument theme = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Theme"), this.context);
        theme.newXObject(THEME_CLASS, this.context);
        this.xwiki.saveDocument(theme, this.context);

        XWikiDocument sheet = loadPage(THEME_SHEET);
        // Display the theme in edit mode, where the logo can be chosen.
        this.context.setDoc(theme);
        this.context.setAction("edit");
        Document document = renderHTMLPage(sheet);

        assertEquals(0, document.getElementsByClass("xwikirenderingerror").size());
        assertEquals(List.of("png,jpg,jpeg,gif,svg,webp,avif,jxl"), this.attachmentSelectorFilters);
    }
}
