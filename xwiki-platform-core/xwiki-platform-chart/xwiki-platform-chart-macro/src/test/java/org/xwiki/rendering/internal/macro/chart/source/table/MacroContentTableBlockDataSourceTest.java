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

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Unit tests for {@link MacroContentTableBlockDataSource}.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class MacroContentTableBlockDataSourceTest extends AbstractMacroContentTableBlockDataSourceTest
{
    @Test
    public void testGetTableBlockWhenNullMacroContent() throws Exception
    {
        try {
            getDataSource().getTableBlock(null, null);
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
            getDataSource().getTableBlock("", null);
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("A Chart Macro using an inline source must have a data table defined in its content.",
                expected.getMessage());
        }
    }

    @Test
    public void testGetTableBlockWhenMacroContentDoesntContainTable() throws Exception
    {
        // Simulate a macro content of "not a table", i.e. not containing a table.
        setUpContentExpectation("not a table");

        try {
            getDataSource().getTableBlock("not a table", null);
            Assert.fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("Unable to locate a suitable data table.", expected.getMessage());
        }
    }
}
