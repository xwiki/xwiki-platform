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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DatabaseProduct}.
 *
 * @version $Id$
 */
class DatabaseProductTest
{
    @Test
    void toProductEquality()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("Oracle");
        assertEquals(DatabaseProduct.ORACLE, product);
        assertSame(DatabaseProduct.ORACLE, product);
        product = DatabaseProduct.toProduct("oracle");
        assertEquals(DatabaseProduct.ORACLE, product);
        assertSame(DatabaseProduct.ORACLE, product);

        product = DatabaseProduct.toProduct("MySQL");
        assertEquals(DatabaseProduct.MYSQL, product);
        assertSame(DatabaseProduct.MYSQL, product);
        product = DatabaseProduct.toProduct("mysql");
        assertEquals(DatabaseProduct.MYSQL, product);
        assertSame(DatabaseProduct.MYSQL, product);
        product = DatabaseProduct.toProduct("MariaDB");
        assertEquals(DatabaseProduct.MARIADB, product);
        assertSame(DatabaseProduct.MARIADB, product);
        product = DatabaseProduct.toProduct("mariadb");
        assertEquals(DatabaseProduct.MARIADB, product);
        assertSame(DatabaseProduct.MARIADB, product);

        product = DatabaseProduct.toProduct("Apache Derby");
        assertEquals(DatabaseProduct.DERBY, product);
        assertSame(DatabaseProduct.DERBY, product);
        product = DatabaseProduct.toProduct("derby");
        assertEquals(DatabaseProduct.DERBY, product);
        assertSame(DatabaseProduct.DERBY, product);

        product = DatabaseProduct.toProduct("HSQL Database Engine");
        assertEquals(DatabaseProduct.HSQLDB, product);
        assertSame(DatabaseProduct.HSQLDB, product);
        product = DatabaseProduct.toProduct("hsqldb");
        assertEquals(DatabaseProduct.HSQLDB, product);
        assertSame(DatabaseProduct.HSQLDB, product);

        product = DatabaseProduct.toProduct("DB2/LINUXX8664");
        assertEquals(DatabaseProduct.DB2, product);
        assertSame(DatabaseProduct.DB2, product);
        product = DatabaseProduct.toProduct("db2");
        assertEquals(DatabaseProduct.DB2, product);
        assertSame(DatabaseProduct.DB2, product);

        product = DatabaseProduct.toProduct("H2");
        assertEquals(DatabaseProduct.H2, product);
        assertSame(DatabaseProduct.H2, product);
        product = DatabaseProduct.toProduct("h2");
        assertEquals(DatabaseProduct.H2, product);
        assertSame(DatabaseProduct.H2, product);

        product = DatabaseProduct.toProduct("Unknown");
        assertEquals(DatabaseProduct.UNKNOWN, product);
        assertSame(DatabaseProduct.UNKNOWN, product);
        product = DatabaseProduct.toProduct("unknown");
        assertEquals(DatabaseProduct.UNKNOWN, product);
        assertSame(DatabaseProduct.UNKNOWN, product);
    }

    @Test
    void toProductDifference()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("Oracle");
        assertTrue(product != DatabaseProduct.DERBY);
        assertNotSame(DatabaseProduct.DERBY, product);
    }

    @Test
    void toProductUnknown()
    {
        DatabaseProduct product = DatabaseProduct.toProduct("whatever");
        assertSame(DatabaseProduct.UNKNOWN, product);
    }
}