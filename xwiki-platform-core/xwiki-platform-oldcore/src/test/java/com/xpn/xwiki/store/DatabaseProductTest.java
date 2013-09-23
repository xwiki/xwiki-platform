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