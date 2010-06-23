package org.xwiki.chart.internal.source;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.chart.internal.source.AbstractTableBlockDataSource;

/**
 * Tests the range parser from the {@link AbstractTableBlockDataSource}.
 * 
 * @version $Id$
 */
public class RangeParserTests extends TestCase
{
    @Test
    public void testRangeParser()
    {
        Assert.assertEquals(0, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("A4"));
        Assert.assertEquals(1, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("B4"));
        Assert.assertEquals(25, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("Z4"));
        Assert.assertEquals(26, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("AA4"));
        Assert.assertEquals(27, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("AB4"));
        Assert.assertEquals(52, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("BA4"));
        Assert.assertEquals(53, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("BB4"));
        Assert.assertEquals(701, AbstractTableBlockDataSource.getColumnNumberFromIdentifier("ZZ4"));
    }
}
