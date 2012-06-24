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
package org.xwiki.chart.internal.source;

import java.io.Reader;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Unit tests for {@link MacroContentTableBlockDataSource}.
 * @version $Id$
 * @since 2.4M2
 */
public class MacroContentTableBlockDataSourceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private MacroContentTableBlockDataSource source;

    @Override
    public void configure() throws Exception
    {
        // Mock components
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        final ComponentManager cm = getComponentManager().getInstance(ComponentManager.class);
        final DocumentReference currentDocumentReference = new DocumentReference("wiki", "space", "page");
        getMockery().checking(new Expectations() {{
            allowing(dab).getCurrentDocumentReference();
                will(returnValue(currentDocumentReference));
            allowing(serializer).serialize(currentDocumentReference);
                will(returnValue("wiki:space.page"));
            allowing(dab).getDocumentSyntaxId("wiki:space.page");
                will(returnValue("xwiki/2.0"));
            allowing(cm).getInstance(Parser.class, "xwiki/2.0");
                will(returnValue(new Parser() {
                    @Override
                    public Syntax getSyntax()
                    {
                        return Syntax.XWIKI_2_0;
                    }

                    @Override
                    public XDOM parse(Reader source) throws ParseException
                    {
                        return new XDOM(Collections.<Block>emptyList());
                    }
                }));
        }});
    }

    @Test
    public void testGetTableBlockWhenNullMacroContent() throws Exception
    {
        try {
            this.source.getTableBlock(null, Collections.<String, String>emptyMap());
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("A Chart Macro using an inline source must have a data table defined in its content.",
                expected.getMessage());
        }
    }

    @Test
    public void testGetTableBlockWhenEmptyMacroContent() throws Exception
    {
        try {
            this.source.getTableBlock("", Collections.<String, String>emptyMap());
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("A Chart Macro using an inline source must have a data table defined in its content.",
                expected.getMessage());
        }
    }

    @Test
    public void testGetTableBlockWhenMacroContentDoesntContainTable() throws Exception
    {
        try {
            this.source.getTableBlock("not a table", Collections.<String, String>emptyMap());
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("Unable to locate a suitable data table.", expected.getMessage());
        }
    }
}
