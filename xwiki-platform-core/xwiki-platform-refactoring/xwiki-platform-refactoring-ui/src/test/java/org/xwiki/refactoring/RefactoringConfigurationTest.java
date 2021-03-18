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

import javax.inject.Named;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.macro.html.HTMLMacroXHTMLRenderer;
import org.xwiki.rendering.internal.macro.html.HTMLMacroXHTMLRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.xml.html.filter.HTMLFilter;

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
@XHTML10ComponentList
@ComponentList({ HTMLMacroXHTMLRenderer.class, HTMLMacroXHTMLRendererFactory.class })
class RefactoringConfigurationTest extends PageTest
{
    public static final DocumentReference REFACTORING_CONFIGURATION =
        new DocumentReference("xwiki", asList("Refactoring", "Code"), "RefactoringConfiguration");

    @Mock
    private StoreConfiguration storeConfiguration;

    // Needs to be registered for the form to be displayed.
    @MockComponent
    @Named("controlcharacters")
    private HTMLFilter htmlFilter;

    @Test
    void verifyFormXRedirectField() throws Exception
    {
        setOutputSyntax(Syntax.XHTML_1_0);

        this.componentManager.registerComponent(StoreConfiguration.class, this.storeConfiguration);
        registerVelocityTool("escapetool", new EscapeTool());

        // Activates the recyclebin feature.
        when(this.storeConfiguration.isRecycleBinEnabled()).thenReturn(true);

        String content = renderPage(REFACTORING_CONFIGURATION);
        String value = Jsoup.parse(content).getElementsByAttributeValue("name", "xredirect").first().attr("value");
        // Checks that the xredirect URL is relative.
        assertEquals("/xwiki/bin/Main/WebHome", value);
    }
}
