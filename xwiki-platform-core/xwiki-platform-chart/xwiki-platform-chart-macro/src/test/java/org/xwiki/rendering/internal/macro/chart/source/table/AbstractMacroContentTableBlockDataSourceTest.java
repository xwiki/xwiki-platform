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
package org.xwiki.rendering.internal.macro.chart.source.table;

import org.jmock.Expectations;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.AbstractMockingComponentTestCase;

import java.io.StringReader;
import java.util.Map;
import java.util.HashMap;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.annotation.MockingRequirement;

import org.xwiki.rendering.renderer.BlockRenderer;

/**
 * Helper to write unit tests for Table-based data sources.
 *
 * @version $Id$
 * @since 4.2M1
 */
public abstract class AbstractMacroContentTableBlockDataSourceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions={ComponentManager.class, BlockRenderer.class})
    private MacroContentTableBlockDataSource source;

    protected MacroContentTableBlockDataSource getDataSource()
    {
        return this.source;
    }

    protected Map<String, String> map(String... keyValues)
    {
        Map<String, String> map = new HashMap<String, String>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }

        return map;
    }

    protected void setUpContentExpectation(final String macroContent) throws Exception
    {
        final MacroContentParser parser = getComponentManager().getInstance(MacroContentParser.class);

        // In order to make it easy to write unit tests, we allow tests to pass a string written in XWiki/2.0 synyax
        // which we then parser to generate an XDOM that we use in the expectation.
        final XDOM expectedXDOM = getComponentManager().<Parser>getInstance(Parser.class,
            Syntax.XWIKI_2_0.toIdString()).parse(new StringReader(macroContent));

        getMockery().checking(new Expectations() {{
            // Simulate parsing the macro content that returns a XDOM not containing a table
            oneOf(parser).parse(macroContent, null, true, false);
                will(returnValue(expectedXDOM));
        }});
    }
}
