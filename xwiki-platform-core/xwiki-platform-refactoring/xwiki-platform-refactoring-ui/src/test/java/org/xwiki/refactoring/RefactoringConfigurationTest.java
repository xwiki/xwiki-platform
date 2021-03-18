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
package org.xwiki.refactoring;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.macro.html.renderers.html5.HTMLMacroHTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5BlockRenderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5Renderer;
import org.xwiki.rendering.internal.renderer.html5.HTML5RendererFactory;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.image.DefaultXHTMLImageTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.DefaultXHTMLLinkTypeRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import com.xpn.xwiki.internal.store.StoreConfiguration;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for testing the {@code Refactoring.Code.RefactoringConfiguration} wiki page.
 *
 * @version $Id$
 * @since 13.2RC1
 * @since 12.10.6
 */
@XWikiSyntax21ComponentList
@ComponentList({
    HTMLMacroHTML5Renderer.class,
    HTML5BlockRenderer.class,
    HTML5Renderer.class,
    HTML5RendererFactory.class,
    DefaultXHTMLLinkRenderer.class,
    DefaultXHTMLLinkTypeRenderer.class,
    DefaultXHTMLImageRenderer.class,
    DefaultXHTMLImageTypeRenderer.class,
    ControlCharactersFilter.class
})
class RefactoringConfigurationTest extends PageTest
{
    public static final DocumentReference REFACTORING_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", asList("Refactoring", "Code"), "RefactoringConfiguration");

    public static final String SKIN_PROPERTIES_PATH = "/skins/flamingo/skin.properties";

    @MockComponent
    private StoreConfiguration storeConfiguration;

    @Test
    void verifyFormXRedirectField() throws Exception
    {
        setOutputSyntax(Syntax.HTML_5_0);

        registerVelocityTool("escapetool", new EscapeTool());

        // Load the environment from the test resources. This is needed to access the 
        Environment environment = this.componentManager.getInstance(Environment.class);
        when(environment.getResource(SKIN_PROPERTIES_PATH)).thenReturn(getClass().getResource(SKIN_PROPERTIES_PATH));

        // Activates the recyclebin feature.
        when(this.storeConfiguration.isRecycleBinEnabled()).thenReturn(true);

        String content = renderPage(REFACTORING_CONFIGURATION_REFERENCE);
        String value = Jsoup.parse(content).getElementsByAttributeValue("name", "xredirect").first().attr("value");
        // Checks that the xredirect URL is relative.
        assertEquals("/xwiki/bin/Main/WebHome", value);
    }
}
