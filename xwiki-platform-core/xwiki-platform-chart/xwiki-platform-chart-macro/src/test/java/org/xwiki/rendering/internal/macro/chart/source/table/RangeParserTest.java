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

/**
 * Tests the range parser from {@link AbstractTableBlockDataSource}.
 * 
 * @version $Id$
 */
public class RangeParserTest
{
    @Test
    public void getColumnNumberFromIdentifier()
    {
        Assert.assertTrue(0 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("A4"));
        Assert.assertTrue(1 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("B4"));
        Assert.assertTrue(25 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("Z4"));
        Assert.assertTrue(26 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("AA4"));
        Assert.assertTrue(27 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("AB4"));
        Assert.assertTrue(52 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("BA4"));
        Assert.assertTrue(53 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("BB4"));
        Assert.assertTrue(701 == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("ZZ4"));
        Assert.assertTrue(null == AbstractTableBlockDataSource.getColumnNumberFromIdentifier("."));
    }
}
