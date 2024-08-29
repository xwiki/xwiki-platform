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
package org.xwiki.image.style;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.VelocityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code Image.Style.Code.Administration}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    ModelScriptService.class,
    TranslationMacro.class
})
class AdministrationPageTest extends PageTest
{
    @Mock
    private CSRFTokenScriptService csrfScriptService;

    private VelocityManager velocityManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        when(this.csrfScriptService.getToken()).thenReturn("csrf_token0", "csrf_token1");
        this.velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
    }

    @Test
    void testForms() throws Exception
    {
        // A current doc is expected to be in the context when rendering the page in the administration.
        this.velocityManager.getVelocityContext()
            .put("currentDoc", this.xwiki.getDocument(new DocumentReference("wiki", "Space", "Page"), this.context));

        Document document =
            renderHTMLPage(new DocumentReference("xwiki", List.of("Image", "Style", "Code"), "Administration"));
        Elements forms = document.getElementsByTag("form");
        // Form 0;
        Element createImageStyleForm = forms.get(0);
        assertEquals("/xwiki/bin/view/Image/Style/Code/ImageStyleClass",
            createImageStyleForm.attr("action"));
        assertEquals("csrf_token0", createImageStyleForm.getElementsByAttributeValue("name", "form_token").val());
        assertEquals("Image.Style.Code.ImageStyleClass",
            createImageStyleForm.getElementsByAttributeValue("name", "parent").val());
        assertEquals("Image.Style.Code.ImageStyleTemplate",
            createImageStyleForm.getElementsByAttributeValue("name", "template").val());
        assertEquals("1", createImageStyleForm.getElementsByAttributeValue("name", "sheet").val());
        assertEquals("Image.Style.Code.ImageStyles",
            createImageStyleForm.getElementsByAttributeValue("name", "spaceName").val());
        // Form 1;
        Element updateConfigurationForm = forms.get(1);
        assertEquals("/xwiki/bin/saveandcontinue/Image/Style/Code/Configuration",
            updateConfigurationForm.attr("action"));
        assertEquals("csrf_token1", updateConfigurationForm.getElementsByAttributeValue("name", "form_token").val());
        assertEquals("/xwiki/bin/admin/Space/Page?editor=globaladmin&section=image.style",
            updateConfigurationForm.getElementsByAttributeValue("name", "xcontinue").val());
        assertEquals("/xwiki/bin/admin/Space/Page?editor=globaladmin&section=image.style",
            updateConfigurationForm.getElementsByAttributeValue("name", "xredirect").val());
        assertEquals("Image.Style.Code.Configuration",
            updateConfigurationForm.getElementsByAttributeValue("name", "classname").val());
    }
}
