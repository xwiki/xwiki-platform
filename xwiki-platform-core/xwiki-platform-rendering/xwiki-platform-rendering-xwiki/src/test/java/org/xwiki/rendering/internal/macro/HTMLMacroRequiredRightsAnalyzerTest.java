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
package org.xwiki.rendering.internal.macro;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.xwiki.platform.security.requiredrights.MacroRequiredRight;
import org.xwiki.platform.security.requiredrights.MacroRequiredRightReporter;
import org.xwiki.properties.ConverterManager;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.xml.html.DefaultHTMLCleanerComponentList;
import org.xwiki.xml.internal.html.filter.SanitizerDetectorFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link HTMLMacroRequiredRightsAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
@DefaultHTMLCleanerComponentList
@ComponentList(SanitizerDetectorFilter.class)
class HTMLMacroRequiredRightsAnalyzerTest
{
    private static final String HTML_MACRO_ID = "html";

    @MockComponent
    private ConverterManager converterManager;

    @InjectMockComponents
    private HTMLMacroRequiredRightsAnalyzer htmlMacroRequiredRightsAnalyzer;

    @BeforeEach
    void setUp()
    {
        when(this.converterManager.convert(eq(Boolean.class), any())).thenAnswer(invocation -> {
            String parameter = invocation.getArgument(1);
            return Boolean.valueOf(parameter);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "<a href='javascript:alert()'>test, true, false, true",
        "<a href='https://xwiki.org'>XWiki, true, false, false",
        "<div>, false, false, true",
        "<div>**wiki**, false, true, true",
        "<div>**wiki**, true, true, true",
    })
    void analyze(String content, boolean clean, boolean wiki, boolean needsScript)
    {
        MacroBlock macroBlock = new MacroBlock(HTML_MACRO_ID,
            Map.of("wiki", String.valueOf(wiki), "clean", String.valueOf(clean)),
            content, false);

        MacroRequiredRightReporter reporter = mock();
        this.htmlMacroRequiredRightsAnalyzer.analyze(macroBlock, reporter);

        if (wiki) {
            verify(reporter).analyzeContent(macroBlock, content);
        }

        if (needsScript) {
            // Capture the required rights to assert different values depending on the parameters.
            ArgumentCaptor<List<MacroRequiredRight>> argumentCaptor = ArgumentCaptor.captor();
            verify(reporter).report(eq(macroBlock), argumentCaptor.capture(), anyString());
            List<MacroRequiredRight> requiredRights = argumentCaptor.getValue();
            // For wiki syntax with cleaning enabled, we cannot analyze the content in advance, so we cannot be sure.
            // For other cases, the result is clear, either cleaning is explicitly disabled or dangerous content has
            // been detected.
            if (wiki && clean) {
                assertEquals(List.of(MacroRequiredRight.MAYBE_SCRIPT), requiredRights);
            } else {
                assertEquals(List.of(MacroRequiredRight.SCRIPT), requiredRights);
            }
        } else {
            verifyNoMoreInteractions(reporter);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "Comment: <!-- comment -->, true",
        "<a href='https://xwiki.org'>XWiki, false",
    })
    void analyzeWithoutParameters(String content, boolean scriptRequired)
    {
        MacroBlock macroBlock = new MacroBlock(HTML_MACRO_ID, Listener.EMPTY_PARAMETERS, content, false);

        MacroRequiredRightReporter reporter = mock();
        this.htmlMacroRequiredRightsAnalyzer.analyze(macroBlock, reporter);

        if (scriptRequired) {
            verify(reporter).report(eq(macroBlock), eq(List.of(MacroRequiredRight.SCRIPT)), anyString());
        }
        verifyNoMoreInteractions(reporter);
    }
}
