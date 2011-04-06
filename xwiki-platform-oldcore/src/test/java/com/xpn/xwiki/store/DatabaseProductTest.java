package com.xpn.xwiki.store;

import junit.framework.TestCase;

/**
 * Unit tests for {@link DatabaseProduct}.
 *
 * @version $Id$
 */
public class DatabaseProductTest extends TestCase
{
    public void testEquality()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("Oracle");
        assertEquals(DatabaseProduct.ORACLE, product);
        assertSame(DatabaseProduct.ORACLE, product);

        product = DatabaseProduct.toProduct("Apache Derby");
        assertEquals(DatabaseProduct.DERBY, product);
        assertSame(DatabaseProduct.DERBY, product);

        product = DatabaseProduct.toProduct("HSQL Database Engine");
        assertEquals(DatabaseProduct.HSQLDB, product);
        assertSame(DatabaseProduct.HSQLDB, product);

        product = DatabaseProduct.toProduct("DB2/LINUXX8664");
        assertEquals(DatabaseProduct.DB2, product);
        assertSame(DatabaseProduct.DB2, product);

        product = DatabaseProduct.toProduct("Unknown");
        assertEquals(DatabaseProduct.UNKNOWN, product);
        assertSame(DatabaseProduct.UNKNOWN, product);
    }

    public void testDifference()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("Oracle");
        assertTrue(product != DatabaseProduct.DERBY);
        assertNotSame(DatabaseProduct.DERBY, product);
    }

    public void testUnknown()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("whatever");
        assertSame(DatabaseProduct.UNKNOWN, product);
    }
}